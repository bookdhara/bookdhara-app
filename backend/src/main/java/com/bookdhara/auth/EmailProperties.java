package com.bookdhara.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Brevo settings. Leave the API key empty to run in test mode (code shown in the app). */
@ConfigurationProperties(prefix = "bookdhara.email")
public record EmailProperties(String brevoApiKey, String senderEmail, String senderName) {

    public boolean configured() {
        return brevoApiKey != null && !brevoApiKey.isBlank() && senderEmail != null && !senderEmail.isBlank();
    }
}
