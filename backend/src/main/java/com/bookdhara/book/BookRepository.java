package com.bookdhara.book;

import com.bookdhara.book.BookEnums.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    /** Loads a book and locks its row, so two buyers can't order the same copy at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<Book> findBySellerIdAndStatusNotOrderByCreatedAtDesc(Long sellerId, Status status);

    @Query("SELECT DISTINCT b.city FROM Book b WHERE b.status = com.bookdhara.book.BookEnums.Status.AVAILABLE ORDER BY b.city")
    List<String> findAvailableCities();
}
