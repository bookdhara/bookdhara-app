package com.bookdhara.auth;

import com.bookdhara.TestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses its own small limits, so it runs in a separate Spring context from the other tests. */
@SpringBootTest(properties = {"bookdhara.rate-limit.code-requests=4", "bookdhara.rate-limit.login-attempts=12"})
@AutoConfigureMockMvc
class PasswordAndRateLimitTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.bookdhara.security.AuthRateLimitFilter rateLimit;

    @BeforeEach
    void clean() throws Exception {
        jdbc.update("DELETE FROM credit_entries");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM book_photos");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM otp_codes");
        jdbc.update("DELETE FROM users");
        resetLimits();
    }

    @Test
    void setPasswordThenLogInWithItAndLockAfterWrongTries() throws Exception {
        String token = TestLogin.login(mvc, json, "riya@example.com", "Riya");

        setPassword(token, null, "short").andExpect(status().isBadRequest());
        setPassword(token, null, "aaaaaaaaaa").andExpect(status().isBadRequest());
        setPassword(token, null, "books-are-great1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPassword").value(true));

        // Changing it needs the current password.
        setPassword(token, null, "another-pass-22").andExpect(status().isBadRequest());
        setPassword(token, "wrong-one-123", "another-pass-22").andExpect(status().isBadRequest());

        login("Riya@Example.com", "books-are-great1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Riya"));

        // Unknown email and wrong password look the same.
        login("nobody@example.com", "books-are-great1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_CREDENTIALS"));
        for (int i = 0; i < 5; i++) {
            login("riya@example.com", "nope-nope-" + i).andExpect(jsonPath("$.code").value("WRONG_CREDENTIALS"));
        }
        // Locked now, even with the right password.
        login("riya@example.com", "books-are-great1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
    }

    @Test
    void forgotPasswordResetsWithAnEmailedCode() throws Exception {
        TestLogin.login(mvc, json, "aman@example.com", "Aman");
        String body = mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aman@example.com\"}"))
                .andReturn().getResponse().getContentAsString();
        String code = json.readTree(body).get("devCode").asText();

        mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aman@example.com\",\"otp\":\"" + code + "\",\"newPassword\":\"fresh-start-99\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(true));
        login("aman@example.com", "fresh-start-99").andExpect(status().isOk());

        // The same code can't be used twice.
        mvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aman@example.com\",\"otp\":\"" + code + "\",\"newPassword\":\"hacker-pass-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneNetworkCantFloodCodeRequests() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"user" + i + "@example.com\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user9@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // A different visitor (as Cloudflare reports it) is not blocked.
        mvc.perform(post("/api/auth/otp/request").header("CF-Connecting-IP", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"other@example.com\"}"))
                .andExpect(status().isOk());

        // Other APIs are not limited.
        mvc.perform(get("/api/status")).andExpect(status().isOk());
    }

    private ResultActions setPassword(String token, String current, String next) throws Exception {
        String body = current == null
                ? "{\"newPassword\":\"" + next + "\"}"
                : "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
        return mvc.perform(post("/api/me/password").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private void resetLimits() throws Exception {
        var method = rateLimit.getClass().getDeclaredMethod("reset");
        method.setAccessible(true);
        method.invoke(rateLimit);
    }
}
