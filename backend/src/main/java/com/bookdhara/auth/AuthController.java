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

    public record OtpRequest(@NotBlank String phone) {
    }

    public record OtpVerifyRequest(@NotBlank String phone,
                                   @NotBlank @Pattern(regexp = "\\d{4,8}", message = "must be digits") String otp) {
    }

    public record LoginResponse(String accessToken, long expiresInSeconds, boolean needsProfile, UserResponse user) {
    }

    private final OtpService otpService;
    private final JwtService jwtService;

    public AuthController(OtpService otpService, JwtService jwtService) {
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @PostMapping("/otp/request")
    public OtpService.OtpRequestResult requestOtp(@Valid @RequestBody OtpRequest request) {
        return otpService.requestOtp(request.phone());
    }

    @PostMapping("/otp/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        User user = otpService.verifyOtp(request.phone(), request.otp());
        boolean needsProfile = user.getName() == null;
        return new LoginResponse(jwtService.createToken(user.getId()), jwtService.ttlSeconds(),
                needsProfile, UserResponse.from(user));
    }
}
