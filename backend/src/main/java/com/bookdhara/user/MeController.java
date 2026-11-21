package com.bookdhara.user;

import com.bookdhara.auth.PasswordService;
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

    public record SetPasswordRequest(String currentPassword, @NotBlank String newPassword) {
    }

    private final UserRepository users;
    private final PasswordService passwords;
    private final com.bookdhara.safety.AccountDeletionService deletion;

    public MeController(UserRepository users, PasswordService passwords,
                        com.bookdhara.safety.AccountDeletionService deletion) {
        this.users = users;
        this.passwords = passwords;
        this.deletion = deletion;
    }

    /** Permanently deletes the account. The app asks the person to confirm first. */
    @org.springframework.web.bind.annotation.DeleteMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal Long userId) {
        deletion.delete(userId);
    }

    /** Set a password (or change it: then the current one is required). */
    @org.springframework.web.bind.annotation.PostMapping("/password")
    public UserResponse setPassword(@AuthenticationPrincipal Long userId, @Valid @RequestBody SetPasswordRequest request) {
        return UserResponse.from(passwords.setPassword(userId, request.currentPassword(), request.newPassword()));
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
