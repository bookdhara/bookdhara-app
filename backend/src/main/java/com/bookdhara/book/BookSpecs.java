package com.bookdhara.book;

import com.bookdhara.book.BookEnums.Status;
import org.springframework.data.jpa.domain.Specification;

/** Building blocks for the browse/search query. Only the filters actually given are added. */
final class BookSpecs {

    private BookSpecs() {
    }

    static Specification<Book> available() {
        return (root, query, cb) -> cb.equal(root.get("status"), Status.AVAILABLE);
    }

    static Specification<Book> notSoldBy(Long userId) {
        return (root, query, cb) -> cb.notEqual(root.get("seller").get("id"), userId);
    }

    static Specification<Book> matches(String text) {
        String like = "%" + text.toLowerCase() + "%";
        String digits = text.replaceAll("[^0-9Xx]", "");
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("author")), like),
                cb.equal(root.get("isbn"), digits));
    }

    static Specification<Book> withCondition(com.bookdhara.book.BookEnums.Condition condition) {
        return (root, query, cb) -> cb.equal(root.get("condition"), condition);
    }

    static Specification<Book> priceAtMost(int maxPrice) {
        return (root, query, cb) -> cb.le(root.get("price"), maxPrice);
    }

    static Specification<Book> inCity(String city) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase());
    }
}
