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
        jdbc.update("DELETE FROM otp_codes");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void newUserCanLogInWithOtpAndSetName() throws Exception {
        String otp = requestOtp("98765 43210");

        String body = mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsProfile").value(true))
                .andExpect(jsonPath("$.user.phone").value("+919876543210"))
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
        login("9876543210");
        String otp = requestOtp("9876543210");

        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.phone").value("+919876543210"));

        Integer userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        org.assertj.core.api.Assertions.assertThat(userCount).isEqualTo(1);
    }

    @Test
    void wrongOtpIsRejected() throws Exception {
        requestOtp("9876543210");

        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usedOtpCannotBeReused() throws Exception {
        String otp = requestOtp("9876543210");
        String verify = "{\"phone\":\"9876543210\",\"otp\":\"" + otp + "\"}";

        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON).content(verify))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
    }

    @Test
    void tooManyOtpRequestsAreBlocked() throws Exception {
        requestOtp("9876543210");
        requestOtp("9876543210");
        requestOtp("9876543210");

        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9876543210\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_RATE_LIMITED"));
    }

    @Test
    void invalidPhoneIsRejected() throws Exception {
        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE"));
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

    private String requestOtp(String phone) throws Exception {
        String body = mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("devCode").asText();
    }

    private void login(String phone) throws Exception {
        String otp = requestOtp(phone);
        mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
    }
}
