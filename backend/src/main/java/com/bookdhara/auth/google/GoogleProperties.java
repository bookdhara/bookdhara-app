package com.bookdhara.auth.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The public OAuth client id of the BookDhara web app. It is not a secret. */
@ConfigurationProperties(prefix = "bookdhara.google")
public record GoogleProperties(String clientId) {

    public boolean configured() {
        return clientId != null && !clientId.isBlank();
    }
}
