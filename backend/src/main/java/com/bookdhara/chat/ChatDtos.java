package com.bookdhara.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record SendRequest(@NotBlank @Size(max = 1000) String body) {
    }

    public record MessageView(Long id, boolean mine, String body, Instant createdAt) {
    }

    /** canSend is false once the order is completed or cancelled (the history stays readable). */
    public record ChatView(Long orderId, String otherName, String bookTitle, boolean canSend, List<MessageView> messages) {
    }
}
