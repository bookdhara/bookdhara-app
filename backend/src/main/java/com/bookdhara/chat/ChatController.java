package com.bookdhara.chat;

import com.bookdhara.chat.ChatDtos.ChatView;
import com.bookdhara.chat.ChatDtos.MessageView;
import com.bookdhara.chat.ChatDtos.SendRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/messages")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @GetMapping
    public ChatView read(@AuthenticationPrincipal Long userId, @PathVariable Long orderId,
                         @RequestParam(defaultValue = "0") long after) {
        return service.read(orderId, userId, after);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView send(@AuthenticationPrincipal Long userId, @PathVariable Long orderId,
                            @Valid @RequestBody SendRequest request) {
        return service.send(orderId, userId, request.body());
    }
}
