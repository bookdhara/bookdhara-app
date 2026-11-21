package com.bookdhara.notify;

/**
 * Something happened to an order that the other person should hear about. Carries everything the
 * email needs, so sending it later doesn't touch the database.
 */
public record OrderEvent(Type type, long orderId, String toEmail, String toName, String otherName,
                         String bookTitle, boolean pickup, String note) {

    public enum Type {
        /** To the seller: someone bought their book. */
        NEW_ORDER,
        /** To the buyer: the seller shipped it. */
        SHIPPED,
        /** To the seller: the sale is complete and they are paid. */
        COMPLETED,
        /** To whoever didn't cancel. */
        CANCELLED
    }
}
