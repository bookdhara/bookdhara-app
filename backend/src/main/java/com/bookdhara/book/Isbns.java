package com.bookdhara.book;

import com.bookdhara.common.ApiException;
import org.springframework.http.HttpStatus;

/** ISBN helpers: strips spaces and dashes, accepts 10- or 13-character ISBNs. */
public final class Isbns {

    private Isbns() {
    }

    public static String normalize(String raw) {
        String isbn = raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase();
        if (!isbn.matches("\\d{13}|\\d{9}[\\dX]")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ISBN",
                    "An ISBN has 10 or 13 digits. You'll find it on the back cover, above the barcode.");
        }
        return isbn;
    }

    public static String normalizeOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : normalize(raw);
    }
}
