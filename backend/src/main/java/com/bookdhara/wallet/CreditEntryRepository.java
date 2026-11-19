package com.bookdhara.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditEntryRepository extends JpaRepository<CreditEntry, Long> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CreditEntry e WHERE e.userId = :userId")
    long balanceOf(@Param("userId") Long userId);

    List<CreditEntry> findTop50ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
