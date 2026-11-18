package com.bookdhara.auth;

import com.bookdhara.common.ApiException;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

@Service
public class OtpService {

    /** What requestOtp hands back. devCode is filled only in test mode (no email service connected). */
    public record OtpRequestResult(String email, long expiresInSeconds, String devCode) {
    }

    private final OtpCodeRepository otpCodes;
    private final UserRepository users;
    private final OtpSender sender;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository otpCodes, UserRepository users, OtpSender sender,
                      PasswordEncoder passwordEncoder, OtpProperties properties, Clock clock) {
        this.otpCodes = otpCodes;
        this.users = users;
        this.sender = sender;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public OtpRequestResult requestOtp(String rawEmail) {
        String email = EmailAddresses.normalize(rawEmail);
        Instant now = clock.instant();

        long recent = otpCodes.countByEmailAndCreatedAtAfter(email, now.minus(properties.requestWindow()));
        if (recent >= properties.maxRequestsPerWindow()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
                    "Too many code requests. Please try again in a few minutes.");
        }

        String code = generateCode();
        otpCodes.save(new OtpCode(email, passwordEncoder.encode(code), now, now.plus(properties.ttl())));
        sender.sendOtp(email, code);

        // Never show the code once real emails are going out, whatever the setting says.
        boolean showCode = properties.exposeInResponse() && !sender.deliversForReal();
        return new OtpRequestResult(email, properties.ttl().toSeconds(), showCode ? code : null);
    }

    /** Checks the code and returns the user, creating the account on first login. */
    @Transactional(noRollbackFor = ApiException.class)
    public User verifyOtp(String rawEmail, String code) {
        String email = EmailAddresses.normalize(rawEmail);
        Instant now = clock.instant();

        OtpCode otp = otpCodes.findFirstByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(OtpService::invalidCode);

        if (otp.isUsed() || otp.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED",
                    "This code has expired. Please request a new one.");
        }
        if (otp.getAttempts() >= properties.maxAttempts()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_TOO_MANY_ATTEMPTS",
                    "Too many wrong tries. Please request a new code.");
        }

        otp.recordAttempt();
        if (code == null || !passwordEncoder.matches(code, otp.getCodeHash())) {
            otpCodes.save(otp);
            throw invalidCode();
        }

        otp.markUsed();
        otpCodes.save(otp);
        return users.findByEmail(email).orElseGet(() -> users.save(User.withEmail(email, now)));
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, properties.length());
        return String.format("%0" + properties.length() + "d", random.nextInt(bound));
    }

    private static ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "Wrong code. Please check and try again.");
    }
}
