package com.bookdhara.book;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "book_photos")
public class BookPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private int position;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BookPhoto() {
        // for JPA
    }

    public BookPhoto(Long bookId, int position, String contentType, byte[] data, Instant createdAt) {
        this.bookId = bookId;
        this.position = position;
        this.contentType = contentType;
        this.data = data;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getBookId() { return bookId; }
    public int getPosition() { return position; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}
