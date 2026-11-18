package com.bookdhara.book;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Uses the free Google Books API (no key needed at our volume). */
@Component
public class GoogleBooksIsbnLookup implements IsbnLookup {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksIsbnLookup.class);

    private final RestClient client;

    public GoogleBooksIsbnLookup(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        this.client = builder.baseUrl("https://www.googleapis.com/books/v1").requestFactory(factory).build();
    }

    @Override
    public Optional<BookInfo> find(String isbn) {
        try {
            JsonNode body = client.get()
                    .uri("/volumes?q=isbn:{isbn}&maxResults=1", isbn)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.path("items").isArray() || body.path("items").isEmpty()) {
                return Optional.empty();
            }
            JsonNode info = body.path("items").get(0).path("volumeInfo");
            String title = info.path("title").asText(null);
            if (title == null) {
                return Optional.empty();
            }
            String subtitle = info.path("subtitle").asText(null);
            if (subtitle != null && title.length() + subtitle.length() < 150) {
                title = title + ": " + subtitle;
            }
            List<String> authors = new ArrayList<>();
            info.path("authors").forEach(a -> authors.add(a.asText()));
            String cover = info.path("imageLinks").path("thumbnail").asText(null);
            if (cover != null) {
                cover = cover.replace("http://", "https://");
            }
            return Optional.of(new BookInfo(isbn, title, authors.isEmpty() ? null : String.join(", ", authors), cover));
        } catch (RestClientException e) {
            log.warn("ISBN lookup failed for {}: {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }
}
