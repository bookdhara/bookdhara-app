package com.bookdhara.user;

import com.bookdhara.auth.PhoneNumbers;
import com.bookdhara.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The logged-in user's own profile. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Name is required. Phone is optional: leave it out or send "" to remove it. */
    public record UpdateProfileRequest(@NotBlank @Size(max = 80) String name, @Size(max = 20) String phone) {
    }

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Long userId) {
        return UserResponse.from(load(userId));
    }

    @PatchMapping
    @Transactional
    public UserResponse update(@AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateProfileRequest request) {
        User user = load(userId);
        user.setName(request.name().trim());
        if (request.phone() != null) {
            if (request.phone().isBlank()) {
                user.setPhone(null);
            } else {
                String phone = PhoneNumbers.normalize(request.phone());
                if (users.existsByPhoneAndIdNot(phone, userId)) {
                    throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN",
                            "This mobile number is already used by another account.");
                }
                user.setPhone(phone);
            }
        }
        return UserResponse.from(users.save(user));
    }

    private User load(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Please log in again"));
    }
}
