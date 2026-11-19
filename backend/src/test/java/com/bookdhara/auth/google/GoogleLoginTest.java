package com.bookdhara.auth.google;

import com.bookdhara.TestLogin;
import com.bookdhara.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "bookdhara.google.client-id=test-client.apps.googleusercontent.com")
@AutoConfigureMockMvc
class GoogleLoginTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private GoogleTokenVerifier verifier;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM credit_entries");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM book_photos");
        jdbc.update("DELETE FROM books");
        jdbc.update("DELETE FROM otp_codes");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void appLearnsTheClientId() throws Exception {
        mvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleClientId").value("test-client.apps.googleusercontent.com"));
    }

    @Test
    void newPersonGetsAnAccountWithTheirGoogleName() throws Exception {
        googleSays("sub-1", "Asha@Gmail.com", true, "  Asha   Rao ");
        google()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.needsProfile").value(false))
                .andExpect(jsonPath("$.user.email").value("asha@gmail.com"))
                .andExpect(jsonPath("$.user.name").value("Asha Rao"))
                .andExpect(jsonPath("$.user.hasPassword").value(false));

        // Signing in again opens the same account.
        long first = userCount();
        google().andExpect(status().isOk());
        assertThat(userCount()).isEqualTo(first).isEqualTo(1);
    }

    @Test
    void googleOpensTheSameAccountMadeWithEmail() throws Exception {
        String token = TestLogin.login(mvc, json, "riya@example.com", "Riya");
        long id = me(token).get("id").asLong();

        googleSays("sub-riya", "riya@example.com", true, "Riya Google Name");
        String body = google()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.user.name").value("Riya"))
                .andReturn().getResponse().getContentAsString();
        assertThat(userCount()).isEqualTo(1);

        // The Google token works for normal API calls.
        String googleToken = json.readTree(body).get("accessToken").asText();
        assertThat(me(googleToken).get("email").asText()).isEqualTo("riya@example.com");
    }

    @Test
    void linkedAccountIsFoundEvenIfTheGmailAddressChanges() throws Exception {
        googleSays("sub-2", "old@gmail.com", true, "Dev");
        google().andExpect(status().isOk());

        googleSays("sub-2", "new@gmail.com", true, "Dev");
        google()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("old@gmail.com"));
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void unverifiedGoogleEmailIsRefused() throws Exception {
        TestLogin.login(mvc, json, "victim@example.com", "Victim");
        googleSays("attacker", "victim@example.com", false, "Attacker");
        google()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GOOGLE_EMAIL_UNVERIFIED"));
    }

    @Test
    void emailLinkedToAnotherGoogleAccountIsRefused() throws Exception {
        googleSays("sub-a", "same@example.com", true, "A");
        google().andExpect(status().isOk());

        googleSays("sub-b", "same@example.com", true, "B");
        google()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GOOGLE_ACCOUNT_MISMATCH"));
    }

    @Test
    void badTokenIsRefused() throws Exception {
        when(verifier.verify(anyString())).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "nope"));
        google()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE_TOKEN_INVALID"));

        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(userCount()).isZero();
    }

    private void googleSays(String sub, String email, boolean verified, String name) {
        when(verifier.verify(anyString())).thenReturn(new GoogleIdentity(sub, email, verified, name));
    }

    private ResultActions google() throws Exception {
        return mvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("credential", "fake.id.token"))));
    }

    private JsonNode me(String token) throws Exception {
        String header = token.startsWith("Bearer ") ? token : "Bearer " + token;
        String body = mvc.perform(get("/api/me").header("Authorization", header))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private long userCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }
}
