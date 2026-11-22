package com.bookdhara.book;

import com.bookdhara.book.BookEnums.Condition;
import com.bookdhara.book.BookEnums.Payout;
import com.bookdhara.book.BookEnums.Status;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class BookDtos {

    private BookDtos() {
    }

    public record CreateBookRequest(
            @Size(max = 20) String isbn,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 200) String author,
            @Min(1) @Max(100_000) Integer mrp,
            @NotNull @Min(1) @Max(100_000) Integer price,
            @NotNull Condition condition,
            @Size(max = 500) String note,
            @NotBlank @Size(max = 60) String city,
            @NotNull @Pattern(regexp = "\\d{6}", message = "must be a 6-digit pincode") String pincode,
            boolean allowsPickup,
            boolean allowsShipping,
            @NotNull Payout payout
    ) {
        @AssertTrue(message = "choose local pickup, delivery, or both")
        public boolean isDeliveryChosen() {
            return allowsPickup || allowsShipping;
        }
    }

    public record SellerInfo(Long id, String name) {
    }

    public record BookSummary(
            Long id, String title, String author, int price, Integer mrp, Condition condition,
            String city, String coverPhotoUrl, Instant createdAt, Status status) {
    }

    public record BookDetail(
            Long id, String isbn, String title, String author, int price, Integer mrp, Condition condition,
            String note, String city, boolean allowsPickup, boolean allowsShipping, Status status,
            List<String> photoUrls, SellerInfo seller, boolean mine,
            /* Only for the seller: what they receive when it sells. */
            Payout payout, Integer payoutAmount, Instant createdAt,
            /* Whether the viewer saved this book. */
            boolean saved) {
    }

    public record IsbnInfo(String isbn, String title, String author, String coverUrl) {
    }

    public record PhotoUploaded(Long id, String url) {
    }
}
