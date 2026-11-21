package com.bookdhara.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WishlistItem() {
        // for JPA
    }

    public WishlistItem(Long userId, Long bookId, Instant createdAt) {
        this.userId = userId;
        this.bookId = bookId;
        this.createdAt = createdAt;
    }

    public Long getBookId() { return bookId; }
}
