package com.bookdhara.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Placeholder until a real SMS provider is connected: writes only that an OTP was sent. */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendOtp(String phone, String code) {
        String masked = phone.substring(0, phone.length() - 4).replaceAll("\\d", "*")
                + phone.substring(phone.length() - 4);
        log.info("OTP generated for {} (no SMS provider connected yet)", masked);
    }
}
