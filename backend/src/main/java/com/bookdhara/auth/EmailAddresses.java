package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.regex.Pattern;

/** Cleans up an email address (trims, lower-cases) and rejects obvious typos. */
public final class EmailAddresses {

    private static final Pattern SIMPLE_EMAIL =
            Pattern.compile("^[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}$");

    private EmailAddresses() {
    }

    public static String normalize(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !SIMPLE_EMAIL.matcher(email).matches() || email.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "Enter a valid email address");
        }
        return email;
    }
}
