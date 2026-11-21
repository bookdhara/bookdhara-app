package com.bookdhara.chat;

import com.bookdhara.order.Order;
import com.bookdhara.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "order_messages")
public class OrderMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderMessage() {
        // for JPA
    }

    public OrderMessage(Order order, User sender, String body, Instant createdAt) {
        this.order = order;
        this.sender = sender;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public User getSender() { return sender; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
