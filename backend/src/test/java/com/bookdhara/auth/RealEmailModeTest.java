package com.bookdhara.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Once a real email service is connected, the code must go by email and never appear in the response. */
@SpringBootTest
@AutoConfigureMockMvc
class RealEmailModeTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OtpSender sender;

    @Test
    void codeIsEmailedAndNotReturned() throws Exception {
        when(sender.deliversForReal()).thenReturn(true);

        mvc.perform(post("/api/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"real.mode@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("real.mode@example.com"))
                .andExpect(jsonPath("$.devCode").doesNotExist());

        verify(sender).sendOtp(eq("real.mode@example.com"), matches("\\d{6}"));
    }
}
