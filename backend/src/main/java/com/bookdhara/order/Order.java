package com.bookdhara.order;

import com.bookdhara.book.Book;
import com.bookdhara.book.BookEnums.Payout;
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
@Table(name = "orders")
public class Order {

    public enum DeliveryMethod { PICKUP, SHIPPING }

    /** PLACED -> (SHIPPED, delivery only) -> COMPLETED, or CANCELLED before completion. */
    public enum Status { PLACED, SHIPPED, COMPLETED, CANCELLED }

    /** TEST until a real payment gateway is connected. */
    public enum PaymentMode { TEST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 10)
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "book_price", nullable = false)
    private int bookPrice;

    @Column(name = "credits_used", nullable = false)
    private int creditsUsed;

    @Column(name = "shipping_fee", nullable = false)
    private int shippingFee;

    @Column(name = "platform_fee", nullable = false)
    private int platformFee;

    @Column(name = "cash_paid", nullable = false)
    private int cashPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, length = 10)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_payout", nullable = false, length = 10)
    private Payout sellerPayout;

    @Column(name = "seller_payout_amount", nullable = false)
    private int sellerPayoutAmount;

    @Column(name = "handover_code", length = 4)
    private String handoverCode;

    @Column(name = "ship_name", length = 80)
    private String shipName;

    @Column(name = "ship_phone", length = 13)
    private String shipPhone;

    @Column(name = "ship_address", length = 300)
    private String shipAddress;

    @Column(name = "ship_city", length = 60)
    private String shipCity;

    @Column(name = "ship_pincode", length = 6)
    private String shipPincode;

    @Column(name = "tracking_note", length = 200)
    private String trackingNote;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Order() {
        // for JPA
    }

    Order(Book book, User buyer, DeliveryMethod method, int creditsUsed, int shippingFee, int platformFee,
          Payout sellerPayout, int sellerPayoutAmount, String handoverCode, Instant now) {
        this.book = book;
        this.buyer = buyer;
        this.seller = book.getSeller();
        this.deliveryMethod = method;
        this.status = Status.PLACED;
        this.bookPrice = book.getPrice();
        this.creditsUsed = creditsUsed;
        this.shippingFee = shippingFee;
        this.platformFee = platformFee;
        this.cashPaid = bookPrice - creditsUsed + shippingFee + platformFee;
        this.paymentMode = PaymentMode.TEST;
        this.sellerPayout = sellerPayout;
        this.sellerPayoutAmount = sellerPayoutAmount;
        this.handoverCode = handoverCode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void shipTo(String name, String phone, String address, String city, String pincode) {
        this.shipName = name;
        this.shipPhone = phone;
        this.shipAddress = address;
        this.shipCity = city;
        this.shipPincode = pincode;
    }

    void markShipped(String note, Instant now) {
        this.status = Status.SHIPPED;
        this.trackingNote = note;
        this.updatedAt = now;
    }

    void complete(Instant now) {
        this.status = Status.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    void cancel(String reason, Instant now) {
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
        this.updatedAt = now;
    }

    public boolean isOpen() {
        return status == Status.PLACED || status == Status.SHIPPED;
    }

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public User getBuyer() { return buyer; }
    public User getSeller() { return seller; }
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public Status getStatus() { return status; }
    public int getBookPrice() { return bookPrice; }
    public int getCreditsUsed() { return creditsUsed; }
    public int getShippingFee() { return shippingFee; }
    public int getPlatformFee() { return platformFee; }
    public int getCashPaid() { return cashPaid; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public Payout getSellerPayout() { return sellerPayout; }
    public int getSellerPayoutAmount() { return sellerPayoutAmount; }
    public String getHandoverCode() { return handoverCode; }
    public String getShipName() { return shipName; }
    public String getShipPhone() { return shipPhone; }
    public String getShipAddress() { return shipAddress; }
    public String getShipCity() { return shipCity; }
    public String getShipPincode() { return shipPincode; }
    public String getTrackingNote() { return trackingNote; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
