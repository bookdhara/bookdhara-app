package com.bookdhara.auth;

/** Sends the OTP text. Swap the implementation when we pick an SMS provider (e.g. MSG91). */
public interface SmsSender {

    void sendOtp(String phone, String code);
}
