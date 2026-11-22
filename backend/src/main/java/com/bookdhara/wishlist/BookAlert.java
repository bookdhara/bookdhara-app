package com.bookdhara.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "book_alerts")
public class BookAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String query;

    @Column(name = "query_lower", nullable = false, length = 120)
    private String queryLower;

    @Column(length = 60)
    private String city;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    protected BookAlert() {
        // for JPA
    }

    public BookAlert(Long userId, String query, String city, Instant createdAt) {
        this.userId = userId;
        this.query = query;
        this.queryLower = query.toLowerCase();
        this.city = city;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getQuery() { return query; }
    public String getCity() { return city; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastNotifiedAt() { return lastNotifiedAt; }

    public void markNotified(Instant at) {
        this.lastNotifiedAt = at;
    }
}
