package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Sends login codes through Brevo's transactional email API (HTTPS, so it works on Render's free plan). */
public class BrevoOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoOtpSender.class);

    private final RestClient client;
    private final EmailProperties properties;

    public BrevoOtpSender(EmailProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        this.client = builder.baseUrl("https://api.brevo.com/v3")
                .requestFactory(factory)
                .defaultHeader("api-key", properties.brevoApiKey())
                .build();
        this.properties = properties;
    }

    @Override
    public void sendOtp(String email, String code) {
        String senderName = properties.senderName() == null ? "BookDhara" : properties.senderName();
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", properties.senderEmail()),
                "to", List.of(Map.of("email", email)),
                "subject", code + " is your BookDhara login code",
                "textContent", "Your BookDhara login code is " + code + ".\n\n"
                        + "It expires in 5 minutes. If you didn't ask for this, you can ignore this email.\n\n"
                        + "Kitaabein behti rahein.",
                "htmlContent", """
                        <div style="font-family:Arial,sans-serif;max-width:420px;margin:auto;color:#15201E">
                          <h2 style="color:#0B6B5D;margin:0 0 16px">BookDhara</h2>
                          <p>Your login code is</p>
                          <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:8px 0 16px">%s</p>
                          <p style="color:#5B6763">It expires in 5 minutes. If you didn't ask for this, you can ignore this email.</p>
                          <p style="color:#5B6763">Kitaabein behti rahein.</p>
                        </div>
                        """.formatted(code));
        unblockIfUnsubscribed(email);
        try {
            client.post().uri("/smtp/email").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Brevo could not send the login code: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_FAILED",
                    "We couldn't send the email right now. Please try again in a minute.");
        }
    }

    /**
     * Gmail shows an "Unsubscribe" button on every Brevo email. If someone pressed it, Brevo would stop
     * delivering their login codes too. They are asking for a code right now, so lift that block first.
     * Best effort: a 404 (not blocked) or any error is ignored.
     */
    private void unblockIfUnsubscribed(String email) {
        try {
            client.delete().uri("/smtp/blockedContacts/{email}", email).retrieve().toBodilessEntity();
            log.info("Removed a transactional email block before sending a login code");
        } catch (RestClientException ignored) {
            // Usually 404: the address wasn't blocked.
        }
    }

    @Override
    public boolean deliversForReal() {
        return true;
    }
}
