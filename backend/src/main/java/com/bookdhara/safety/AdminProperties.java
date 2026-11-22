package com.bookdhara.safety;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/** Emails of the BookDhara team: they get report alerts (and later, admin screens). */
@ConfigurationProperties(prefix = "bookdhara.admin")
public record AdminProperties(String emails) {

    public List<String> emailList() {
        if (emails == null || emails.isBlank()) {
            return List.of();
        }
        return Arrays.stream(emails.split(",")).map(String::trim).map(String::toLowerCase).filter(e -> !e.isEmpty()).toList();
    }

    public boolean isAdmin(String email) {
        return email != null && emailList().contains(email.toLowerCase());
    }
}
