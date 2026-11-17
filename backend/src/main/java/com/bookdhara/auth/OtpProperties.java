package com.bookdhara.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bookdhara.otp")
public record OtpProperties(
        int length,
        Duration ttl,
        int maxAttempts,
        int maxRequestsPerWindow,
        Duration requestWindow,
        boolean exposeInResponse
) {
}
