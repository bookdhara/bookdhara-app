package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.regex.Pattern;

/** Turns the many ways people type an Indian mobile number into one format: +91XXXXXXXXXX. */
public final class PhoneNumbers {

    private static final Pattern INDIAN_MOBILE = Pattern.compile("[6-9]\\d{9}");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw invalid();
        }
        String digits = raw.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+91")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        if (!INDIAN_MOBILE.matcher(digits).matches()) {
            throw invalid();
        }
        return "+91" + digits;
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE",
                "Enter a valid 10-digit Indian mobile number");
    }
}
