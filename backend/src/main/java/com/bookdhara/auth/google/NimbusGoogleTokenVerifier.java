package com.bookdhara.auth.google;

import com.bookdhara.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Verifies Google ID tokens ourselves, as Google documents: the signature must match one of Google's
 * published keys, the issuer must be Google, the audience must be our client id, and it must not be
 * expired. Google's keys are downloaded once and cached.
 */
@Component
public class NimbusGoogleTokenVerifier implements GoogleTokenVerifier {

    static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final GoogleProperties properties;
    private volatile JwtDecoder decoder;

    public NimbusGoogleTokenVerifier(GoogleProperties properties) {
        this.properties = properties;
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID",
                    "Google sign-in failed. Please try again.");
        }
        Object verified = jwt.getClaims().get("email_verified");
        return new GoogleIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                verified != null && "true".equalsIgnoreCase(verified.toString()),
                jwt.getClaimAsString("name"));
    }

    private JwtDecoder decoder() {
        JwtDecoder d = decoder;
        if (d == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL)
                            .jwsAlgorithm(SignatureAlgorithm.RS256)
                            .build();
                    nimbus.setJwtValidator(validator(properties.clientId()));
                    decoder = nimbus;
                }
                d = decoder;
            }
        }
        return d;
    }

    /** Package-private so the rules can be tested without real Google tokens. */
    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> aud = jwt.getAudience();
            return aud != null && aud.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));
        };
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "No subject", null));
        // Requires exp, and checks exp/nbf with a small allowance for clock drift.
        OAuth2TokenValidator<Jwt> expiry = jwt -> jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "No expiry", null));
        return new DelegatingOAuth2TokenValidator<>(List.of(
                expiry, new JwtTimestampValidator(), issuer, audience, subject));
    }
}
