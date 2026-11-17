package com.bookdhara.user;

import java.time.Instant;

public record UserResponse(Long id, String phone, String name, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPhone(), user.getName(), user.getCreatedAt());
    }
}
