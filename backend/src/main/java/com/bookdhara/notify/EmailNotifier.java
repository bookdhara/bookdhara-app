package com.bookdhara.notify;

/** Sends a plain notification email (not login codes). Must never throw. */
public interface EmailNotifier {

    void send(String toEmail, String subject, String text, String html);
}
