package com.bookdhara.auth;

/** Delivers a login code to the user. */
public interface OtpSender {

    void sendOtp(String email, String code);

    /**
     * True when codes really reach the user's inbox. While false (no email service connected yet),
     * the code is shown in the app instead so we can test.
     */
    boolean deliversForReal();
}
