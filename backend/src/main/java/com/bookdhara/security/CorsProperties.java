package com.bookdhara.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bookdhara.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
