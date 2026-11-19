package com.bookdhara.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM book_photos");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM otp_codes");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void newUserCanLogInWithOtpAndSetName() throws Exception {
        String otp = requestOtp("  Riya.S@Example.com ");

        String body = mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riya.s@example.com\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsProfile").value(true))
                .andExpect(jsonPath("$.user.email").value("riya.s@example.com"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();

        mvc.perform(patch("/api/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Akanksha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Akanksha"));

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Akanksha"));
    }

    @Test
    void returningUserGetsSameAccount() throws Exception {
        login("riya.s@example.com");
        String otp = requestOtp("riya.s@example.com");

        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riya.s@example.com\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("riya.s@example.com"));

        Integer userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        org.assertj.core.api.Assertions.assertThat(userCount).isEqualTo(1);
    }

    @Test
    void phoneCanBeAddedChangedAndRemovedInProfile() throws Exception {
        String token = com.bookdhara.TestLogin.login(mvc, json, "riya.s@example.com", "Riya");
        String other = com.bookdhara.TestLogin.login(mvc, json, "aman@example.com", "Aman");

        mvc.perform(patch("/api/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riya\",\"phone\":\"98765 43210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+919876543210"));
        mvc.perform(patch("/api/me").header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Aman\",\"phone\":\"9876543210\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_TAKEN"));
        mvc.perform(patch("/api/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riya\",\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riya\"}"))
                .andExpect(jsonPath("$.phone").value("+919876543210"));
        mvc.perform(patch("/api/me").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riya\",\"phone\":\"\"}"))
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void wrongOtpIsRejected() throws Exception {
        requestOtp("riya.s@example.com");

        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riya.s@example.com\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usedOtpCannotBeReused() throws Exception {
        String otp = requestOtp("riya.s@example.com");
        String verify = "{\"email\":\"riya.s@example.com\",\"otp\":\"" + otp + "\"}";

        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
    }

    @Test
    void tooManyOtpRequestsAreBlocked() throws Exception {
        requestOtp("riya.s@example.com");
        requestOtp("riya.s@example.com");
        requestOtp("riya.s@example.com");

        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"riya.s@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_RATE_LIMITED"));
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
    }

    @Test
    void profileNeedsLogin() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void statusPageIsPlainJsonForBrowsers() throws Exception {
        for (String path : new String[]{"/", "/api/status"}) {
            mvc.perform(get(path).accept(MediaType.TEXT_HTML, MediaType.ALL))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    private String requestOtp(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("devCode").asText();
    }

    private void login(String email) throws Exception {
        String otp = requestOtp(email);
        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
    }
}
