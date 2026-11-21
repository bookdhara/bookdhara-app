package com.bookdhara.chat;

import com.bookdhara.chat.ChatDtos.ChatView;
import com.bookdhara.chat.ChatDtos.MessageView;
import com.bookdhara.common.ApiException;
import com.bookdhara.notify.OrderEvent;
import com.bookdhara.order.Order;
import com.bookdhara.order.OrderRepository;
import com.bookdhara.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Chat between the buyer and seller of one order, so they can agree where and when to meet without
 * sharing phone numbers. Only the two of them can read it. Sending stops once the order is closed.
 */
@Service
public class ChatService {

    static final int MAX_PER_MINUTE = 20;
    /** At most one "new message" email per person per order in this window. */
    static final Duration EMAIL_GAP = Duration.ofMinutes(30);

    private final OrderRepository orders;
    private final OrderMessageRepository messages;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final com.bookdhara.safety.BlockService blocks;

    public ChatService(OrderRepository orders, OrderMessageRepository messages,
                       ApplicationEventPublisher events, Clock clock, com.bookdhara.safety.BlockService blocks) {
        this.blocks = blocks;
        this.orders = orders;
        this.messages = messages;
        this.events = events;
        this.clock = clock;
    }

    /** Messages after [afterId] (0 = all). Reading marks them as seen. */
    @Transactional
    public ChatView read(Long orderId, Long userId, long afterId) {
        Order o = loadFor(orderId, userId);
        List<OrderMessage> list = messages.findTop200ByOrderIdAndIdGreaterThanOrderByIdAsc(orderId, Math.max(afterId, 0));
        if (!list.isEmpty()) {
            o.markReadUpto(userId, list.get(list.size() - 1).getId());
        }
        return new ChatView(o.getId(), other(o, userId).getName(), o.getBook().getTitle(), o.isOpen(),
                list.stream().map(m -> view(m, userId)).toList());
    }

    @Transactional
    public MessageView send(Long orderId, Long userId, String rawBody) {
        Order o = loadFor(orderId, userId);
        if (!o.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "CHAT_CLOSED", "This order is closed, so the chat is read-only.");
        }
        blocks.requireNotBlocked(userId, other(o, userId).getId(), "You can't message this person.");
        String body = clean(rawBody);
        if (body.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_MESSAGE", "Type a message first.");
        }
        Instant now = clock.instant();
        if (messages.countBySenderIdAndCreatedAtAfter(userId, now.minusSeconds(60)) >= MAX_PER_MINUTE) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_MESSAGES", "Slow down a little and try again.");
        }

        User me = o.isBuyer(userId) ? o.getBuyer() : o.getSeller();
        User them = other(o, userId);
        boolean theyWereCaughtUp = messages.countUnread(orderId, them.getId(), o.readUptoFor(them.getId())) == 0;

        OrderMessage saved = messages.save(new OrderMessage(o, me, body, now));
        o.markReadUpto(userId, saved.getId());

        Instant lastEmail = o.chatEmailedAtFor(them.getId());
        if (theyWereCaughtUp && (lastEmail == null || lastEmail.isBefore(now.minus(EMAIL_GAP)))) {
            o.markChatEmailed(them.getId(), now);
            events.publishEvent(new OrderEvent(OrderEvent.Type.MESSAGE, o.getId(), them.getEmail(), them.getName(),
                    me.getName(), o.getBook().getTitle(), o.getDeliveryMethod() == Order.DeliveryMethod.PICKUP, body));
        }
        return view(saved, userId);
    }

    private Order loadFor(Long orderId, Long userId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (!o.getBuyer().getId().equals(userId) && !o.getSeller().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        return o;
    }

    private static User other(Order o, Long userId) {
        return o.isBuyer(userId) ? o.getSeller() : o.getBuyer();
    }

    private static MessageView view(OrderMessage m, Long userId) {
        return new MessageView(m.getId(), m.getSender().getId().equals(userId), m.getBody(), m.getCreatedAt());
    }

    /** Plain text only: no control characters, at most two blank lines in a row. */
    static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r\n", "\n").replaceAll("[\\p{Cntrl}&&[^\n\t]]", "").strip();
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
