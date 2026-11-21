package com.bookdhara.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookPhotoRepository extends JpaRepository<BookPhoto, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM BookPhoto p WHERE p.bookId IN :bookIds")
    int deleteByBookIds(@org.springframework.data.repository.query.Param("bookIds") java.util.Collection<Long> bookIds);

    /** Photo ids only (without the image bytes), in display order. */
    interface PhotoRef {
        Long getId();
        Long getBookId();
    }

    @Query("SELECT p.id AS id, p.bookId AS bookId FROM BookPhoto p WHERE p.bookId IN :bookIds ORDER BY p.position")
    List<PhotoRef> findRefsByBookIds(@Param("bookIds") Collection<Long> bookIds);

    long countByBookId(Long bookId);
}
