package com.bookdhara.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OtpSenderConfig {

    private static final Logger log = LoggerFactory.getLogger(OtpSenderConfig.class);

    @Bean
    public OtpSender otpSender(EmailProperties properties, RestClient.Builder restClient) {
        if (properties.configured()) {
            log.info("Login codes are sent by email through Brevo");
            return new BrevoOtpSender(properties, restClient);
        }
        log.warn("No email service configured: login codes are shown in the app (test mode)");
        return new TestModeOtpSender();
    }

    /** Test mode: nothing is sent; the code is returned to the app instead. */
    static class TestModeOtpSender implements OtpSender {

        @Override
        public void sendOtp(String email, String code) {
            log.info("Test mode: login code created for {}", maskEmail(email));
        }

        @Override
        public boolean deliversForReal() {
            return false;
        }
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }
}
