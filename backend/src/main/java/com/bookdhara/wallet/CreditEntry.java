package com.bookdhara.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One change to a user's credits (+ earned, - spent). The balance is the sum of all entries. */
@Entity
@Table(name = "credit_entries")
public class CreditEntry {

    public enum Kind { SALE, PURCHASE, REFUND, BONUS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false, length = 120)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CreditEntry() {
        // for JPA
    }

    public CreditEntry(Long userId, int amount, Kind kind, Long orderId, String note, Instant createdAt) {
        this.userId = userId;
        this.amount = amount;
        this.kind = kind;
        this.orderId = orderId;
        this.note = note.length() > 120 ? note.substring(0, 117) + "..." : note;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public int getAmount() { return amount; }
    public Kind getKind() { return kind; }
    public Long getOrderId() { return orderId; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
