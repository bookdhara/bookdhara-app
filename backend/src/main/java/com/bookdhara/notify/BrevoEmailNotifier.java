package com.bookdhara.notify;

import com.bookdhara.auth.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Order updates through Brevo. Unlike login codes, these respect an "Unsubscribe" click: if Brevo
 * has blocked the address, the email is simply not delivered and the in-app alert still shows.
 */
public class BrevoEmailNotifier implements EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailNotifier.class);

    private final RestClient client;
    private final EmailProperties properties;

    public BrevoEmailNotifier(EmailProperties properties, RestClient.Builder builder) {
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
    public void send(String toEmail, String subject, String text, String html) {
        String senderName = properties.senderName() == null ? "BookDhara" : properties.senderName();
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", senderName, "email", properties.senderEmail()),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", text,
                "htmlContent", html);
        try {
            client.post().uri("/smtp/email").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            log.warn("Brevo could not send an order email: {}", e.getMessage());
        }
    }
}
