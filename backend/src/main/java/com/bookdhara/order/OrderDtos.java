package com.bookdhara.order;

import com.bookdhara.book.BookEnums.Payout;
import com.bookdhara.order.Order.DeliveryMethod;
import com.bookdhara.order.Order.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record Address(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 60) String city,
            @NotNull @Pattern(regexp = "\\d{6}", message = "must be a 6-digit pincode") String pincode) {
    }

    public record PlaceOrderRequest(
            @NotNull Long bookId,
            @NotNull DeliveryMethod method,
            @Min(0) int creditsToUse,
            @Valid Address address) {
    }

    public record Quote(int bookPrice, int creditsUsed, int shippingFee, int platformFee, int cashToPay,
                        long creditBalance, int maxCredits) {
    }

    public record ShipRequest(@Size(max = 200) String trackingNote) {
    }

    public record HandoverRequest(@NotBlank @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String code) {
    }

    public record CancelRequest(@Size(max = 200) String reason) {
    }

    public record BookInfo(Long id, String title, String author, String coverPhotoUrl) {
    }

    /** What each side may do next. The app shows only these buttons. */
    public record Actions(boolean canShip, boolean canConfirmReceived, boolean canEnterHandoverCode, boolean canCancel) {
    }

    public record OrderView(
            Long id, Status status, DeliveryMethod method, String myRole, BookInfo book,
            String buyerName, String sellerName, String sellerCity,
            int bookPrice, int creditsUsed, int shippingFee, int platformFee, int cashPaid, String paymentMode,
            /* Buyer only, pickup, while open: share it when you have the book. */
            String handoverCode,
            /* Seller only: what they receive. */
            Payout sellerPayout, Integer sellerPayoutAmount,
            Address shippingAddress, String trackingNote, String cancelReason,
            Actions actions, Instant createdAt, Instant completedAt,
            /* Chat messages from the other person I haven't read yet. */
            long unreadMessages,
            /* The other person's id, for "Report" and "Block". */
            Long otherUserId) {
    }
}
