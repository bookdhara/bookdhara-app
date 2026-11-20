package com.bookdhara.auth.google;

/** What we trust from a checked Google sign-in token. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
}
