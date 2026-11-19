package com.bookdhara.user;

import java.time.Instant;

public record UserResponse(Long id, String email, String phone, String name, boolean hasPassword, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getPhone(), user.getName(), user.hasPassword(),
                user.getCreatedAt());
    }
}
