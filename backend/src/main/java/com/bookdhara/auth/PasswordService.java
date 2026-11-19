package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Optional email + password login. Sign-up and "forgot password" still go through an emailed code. */
@Service
public class PasswordService {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 72; // BCrypt only uses the first 72 bytes
    static final int MAX_FAILURES = 5;
    static final Duration LOCK_FOR = Duration.ofMinutes(15);


    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final Clock clock;

    /**
     * A real hash of a random value, checked when the email is unknown, so "no such account"
     * takes as long as "wrong password" and the two can't be told apart.
     */
    private final String dummyHash;

    public PasswordService(UserRepository users, PasswordEncoder encoder, OtpService otpService, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.otpService = otpService;
        this.clock = clock;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public User login(String rawEmail, String password) {
        String email = EmailAddresses.normalize(rawEmail);
        Instant now = clock.instant();
        User user = users.findByEmail(email).orElse(null);

        if (user == null || !user.hasPassword()) {
            encoder.matches(password == null ? "" : password, dummyHash);
            throw wrongCredentials();
        }
        if (user.isLocked(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED",
                    "Too many wrong passwords. Try again in 15 minutes, or log in with an email code.");
        }
        if (password == null || !encoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin(now, MAX_FAILURES, LOCK_FOR);
            users.save(user);
            throw wrongCredentials();
        }
        user.recordSuccessfulLogin();
        return users.save(user);
    }

    /** Set or change the password of a logged-in user. Changing needs the current one. */
    @Transactional
    public User setPassword(Long userId, String currentPassword, String newPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Please log in again"));
        if (user.hasPassword() && (currentPassword == null || !encoder.matches(currentPassword, user.getPasswordHash()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CURRENT_PASSWORD", "Your current password is wrong.");
        }
        user.setPasswordHash(encoder.encode(validNew(newPassword)));
        return users.save(user);
    }

    /** "Forgot password": the emailed code proves it's their email, then the new password is saved. */
    @Transactional(noRollbackFor = ApiException.class)
    public User resetWithCode(String email, String code, String newPassword) {
        String checked = validNew(newPassword);
        User user = otpService.verifyOtp(email, code);
        user.setPasswordHash(encoder.encode(checked));
        return users.save(user);
    }

    static String validNew(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Use at least 8 characters.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Use at most 72 characters.");
        }
        if (password.isBlank() || password.chars().distinct().count() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD",
                    "That password is too easy to guess. Mix a few different letters, numbers or symbols.");
        }
        return password;
    }

    private static ApiException wrongCredentials() {
        return new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CREDENTIALS", "Wrong email or password.");
    }
}
