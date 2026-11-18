package com.bookdhara;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logs a test user in through the real OTP endpoints and returns a bearer token. */
public final class TestLogin {

    private TestLogin() {
    }

    public static String login(MockMvc mvc, ObjectMapper json, String email, String name) throws Exception {
        String otpBody = mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String otp = json.readTree(otpBody).get("devCode").asText();

        String verifyBody = mvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(verifyBody);
        String token = "Bearer " + node.get("accessToken").asText();

        if (name != null) {
            mvc.perform(patch("/api/me").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isOk());
        }
        return token;
    }
}
