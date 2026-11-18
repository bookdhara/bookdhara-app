package com.bookdhara.book;

public final class BookEnums {

    private BookEnums() {
    }

    public enum Condition { LIKE_NEW, GOOD, READABLE }

    /** What the seller wants when the book sells. Credits come with a 25% bonus. */
    public enum Payout { CASH, CREDITS }

    public enum Status { AVAILABLE, RESERVED, SOLD, REMOVED }
}
