package com.bookdhara.book;

import com.bookdhara.book.BookEnums.Condition;
import com.bookdhara.book.BookEnums.Payout;
import com.bookdhara.book.BookEnums.Status;
import com.bookdhara.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(length = 13)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String author;

    private Integer mrp;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition", nullable = false, length = 20)
    private Condition condition;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(name = "allows_pickup", nullable = false)
    private boolean allowsPickup;

    @Column(name = "allows_shipping", nullable = false)
    private boolean allowsShipping;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Payout payout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Book() {
        // for JPA
    }

    public Book(User seller, String isbn, String title, String author, Integer mrp, int price,
                Condition condition, String note, String city, String pincode,
                boolean allowsPickup, boolean allowsShipping, Payout payout, Instant now) {
        this.seller = seller;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.mrp = mrp;
        this.price = price;
        this.condition = condition;
        this.note = note;
        this.city = city;
        this.pincode = pincode;
        this.allowsPickup = allowsPickup;
        this.allowsShipping = allowsShipping;
        this.payout = payout;
        this.status = Status.AVAILABLE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeStatus(Status status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public User getSeller() { return seller; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public Integer getMrp() { return mrp; }
    public int getPrice() { return price; }
    public Condition getCondition() { return condition; }
    public String getNote() { return note; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public boolean isAllowsPickup() { return allowsPickup; }
    public boolean isAllowsShipping() { return allowsShipping; }
    public Payout getPayout() { return payout; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
