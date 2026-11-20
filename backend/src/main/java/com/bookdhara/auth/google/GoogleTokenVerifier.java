package com.bookdhara.auth.google;

/** Checks a Google sign-in token (an ID token) and returns who it belongs to. */
public interface GoogleTokenVerifier {

    /** @throws com.bookdhara.common.ApiException if the token is fake, expired or meant for another app */
    GoogleIdentity verify(String idToken);
}
