package com.bookdhara.book;

import java.util.Optional;

/** Finds a book's title and author from its ISBN. */
public interface IsbnLookup {

    record BookInfo(String isbn, String title, String author, String coverUrl) {
    }

    Optional<BookInfo> find(String isbn);
}
