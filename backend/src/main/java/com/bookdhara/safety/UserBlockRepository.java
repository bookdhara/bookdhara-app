package com.bookdhara.safety;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM UserBlock b
            WHERE (b.blockerId = :a AND b.blockedId = :b) OR (b.blockerId = :b AND b.blockedId = :a)
            """)
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    @Modifying
    @Query("DELETE FROM UserBlock b WHERE b.blockerId = :blocker AND b.blockedId = :blocked")
    int unblock(@Param("blocker") Long blocker, @Param("blocked") Long blocked);

    @Modifying
    @Query("DELETE FROM UserBlock b WHERE b.blockerId = :user OR b.blockedId = :user")
    int deleteAllFor(@Param("user") Long user);
}
