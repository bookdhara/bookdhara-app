package com.bookdhara.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.userId = :userId AND w.bookId = :bookId")
    int remove(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.userId = :userId")
    int removeAllFor(@Param("userId") Long userId);
}
