package com.bookdhara.auth.google;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The checks applied to every Google token after its signature is verified. */
class GoogleTokenRulesTest {

    private static final String CLIENT = "ours.apps.googleusercontent.com";
    private final OAuth2TokenValidator<Jwt> rules = NimbusGoogleTokenVerifier.validator(CLIENT);

    @Test
    void acceptsAGoodToken() {
        assertThat(ok(token("https://accounts.google.com", CLIENT, Instant.now().plusSeconds(600), "123"))).isTrue();
        assertThat(ok(token("accounts.google.com", CLIENT, Instant.now().plusSeconds(600), "123"))).isTrue();
    }

    @Test
    void rejectsTokensForAnotherAppOrFromAnotherIssuer() {
        assertThat(ok(token("https://accounts.google.com", "other.apps.googleusercontent.com",
                Instant.now().plusSeconds(600), "123"))).isFalse();
        assertThat(ok(token("https://evil.example.com", CLIENT, Instant.now().plusSeconds(600), "123"))).isFalse();
    }

    @Test
    void rejectsExpiredOrSubjectlessTokens() {
        assertThat(ok(token("https://accounts.google.com", CLIENT, Instant.now().minusSeconds(600), "123"))).isFalse();
        assertThat(ok(token("https://accounts.google.com", CLIENT, Instant.now().plusSeconds(600), " "))).isFalse();
    }

    private boolean ok(Jwt jwt) {
        return !rules.validate(jwt).hasErrors();
    }

    private static Jwt token(String iss, String aud, Instant exp, String sub) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .claim("iss", iss)
                .audience(List.of(aud))
                .subject(sub)
                .issuedAt(exp.minusSeconds(3600))
                .expiresAt(exp)
                .build();
    }
}
