package com.bookdhara.notify;

import com.bookdhara.auth.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

/** Order emails are sent in the background, so a slow email service never slows down an order. */
@Configuration
@EnableAsync
public class NotifierConfig {

    private static final Logger log = LoggerFactory.getLogger(NotifierConfig.class);

    @Bean
    public EmailNotifier emailNotifier(EmailProperties properties, RestClient.Builder restClient) {
        if (properties.configured()) {
            return new BrevoEmailNotifier(properties, restClient);
        }
        return (to, subject, text, html) -> log.info("Test mode: order email not sent ({})", subject);
    }
}
