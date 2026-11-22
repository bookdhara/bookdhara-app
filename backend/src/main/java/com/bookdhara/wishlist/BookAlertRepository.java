package com.bookdhara.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookAlertRepository extends JpaRepository<BookAlert, Long> {

    List<BookAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndQueryLower(Long userId, String queryLower);

    /** Alerts whose words appear in the new book's title or author, or that name its ISBN. */
    @Query("""
            SELECT a FROM BookAlert a
            WHERE a.userId <> :sellerId
              AND (LOCATE(a.queryLower, :title) > 0 OR LOCATE(a.queryLower, :author) > 0 OR a.queryLower = :isbn)
              AND (a.city IS NULL OR LOWER(a.city) = :city)
            """)
    List<BookAlert> matching(@Param("sellerId") Long sellerId, @Param("title") String titleLower,
                             @Param("author") String authorLower, @Param("isbn") String isbn, @Param("city") String cityLower);

    @Modifying
    @Query("DELETE FROM BookAlert a WHERE a.id = :id AND a.userId = :userId")
    int remove(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM BookAlert a WHERE a.userId = :userId")
    int removeAllFor(@Param("userId") Long userId);
}
