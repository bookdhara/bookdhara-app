package com.bookdhara.auth.google;

import com.bookdhara.auth.EmailAddresses;
import com.bookdhara.common.ApiException;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * "Continue with Google". One account per email: signing in with Google opens the same account the
 * person made with email code or password, because Google has already verified they own that email.
 */
@Service
public class GoogleAuthService {

    private static final int MAX_NAME = 80;

    private final GoogleProperties properties;
    private final GoogleTokenVerifier verifier;
    private final UserRepository users;
    private final Clock clock;

    public GoogleAuthService(GoogleProperties properties, GoogleTokenVerifier verifier,
                             UserRepository users, Clock clock) {
        this.properties = properties;
        this.verifier = verifier;
        this.users = users;
        this.clock = clock;
    }

    public boolean enabled() {
        return properties.configured();
    }

    @Transactional
    public User login(String idToken) {
        if (!enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_DISABLED",
                    "Google sign-in is not set up yet. Please use email.");
        }
        GoogleIdentity google = verifier.verify(idToken);
        if (!google.emailVerified() || google.email() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GOOGLE_EMAIL_UNVERIFIED",
                    "Your Google account's email isn't verified. Please use email login.");
        }

        // Already linked: Google's id is stable even if the person changes their Gmail address.
        var linked = users.findByGoogleSub(google.subject());
        if (linked.isPresent()) {
            linked.get().recordSuccessfulLogin();
            return linked.get();
        }

        String email = EmailAddresses.normalize(google.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.withEmail(email, clock.instant());
            user.setName(cleanName(google.name()));
        } else if (user.getGoogleSub() != null) {
            // This email belongs to an account linked to a different Google account.
            throw new ApiException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_MISMATCH",
                    "This email is linked to another Google account. Please log in with email.");
        } else if (user.getName() == null) {
            user.setName(cleanName(google.name()));
        }
        user.setGoogleSub(google.subject());
        user.recordSuccessfulLogin();
        return users.save(user);
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.strip().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME).strip() : trimmed;
    }
}
