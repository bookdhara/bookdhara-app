package com.bookdhara.auth;

import com.bookdhara.security.JwtService;
import com.bookdhara.user.User;
import com.bookdhara.user.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record OtpRequest(@NotBlank String email) {
    }

    public record OtpVerifyRequest(@NotBlank String email,
                                   @NotBlank @Pattern(regexp = "\\d{4,8}", message = "must be digits") String otp) {
    }

    public record LoginResponse(String accessToken, long expiresInSeconds, boolean needsProfile, UserResponse user) {
    }

    public record PasswordLoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record ResetPasswordRequest(@NotBlank String email,
                                       @NotBlank @Pattern(regexp = "\\d{4,8}", message = "must be digits") String otp,
                                       @NotBlank String newPassword) {
    }

    private final OtpService otpService;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthController(OtpService otpService, PasswordService passwordService, JwtService jwtService) {
        this.otpService = otpService;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody PasswordLoginRequest request) {
        return loggedIn(passwordService.login(request.email(), request.password()));
    }

    /** Forgot password: request a code with /otp/request, then send it here with the new password. */
    @PostMapping("/password/reset")
    public LoginResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return loggedIn(passwordService.resetWithCode(request.email(), request.otp(), request.newPassword()));
    }

    @PostMapping("/otp/request")
    public OtpService.OtpRequestResult requestOtp(@Valid @RequestBody OtpRequest request) {
        return otpService.requestOtp(request.email());
    }

    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return loggedIn(otpService.verifyOtp(request.email(), request.otp()));
    }

    private LoginResponse loggedIn(User user) {
        return new LoginResponse(jwtService.createToken(user.getId()), jwtService.ttlSeconds(),
                user.getName() == null, UserResponse.from(user));
    }
}
