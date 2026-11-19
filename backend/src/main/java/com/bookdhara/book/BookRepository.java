package com.bookdhara.book;

import com.bookdhara.book.BookEnums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findBySellerIdAndStatusNotOrderByCreatedAtDesc(Long sellerId, Status status);

    @Query("SELECT DISTINCT b.city FROM Book b WHERE b.status = com.bookdhara.book.BookEnums.Status.AVAILABLE ORDER BY b.city")
    List<String> findAvailableCities();
}
