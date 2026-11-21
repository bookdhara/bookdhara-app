package com.bookdhara.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {

    /** Messages after a given id, oldest first (the app asks only for new ones while the chat is open). */
    List<OrderMessage> findTop200ByOrderIdAndIdGreaterThanOrderByIdAsc(Long orderId, Long afterId);

    /** Messages from the other person that this reader hasn't seen. */
    @Query("""
            SELECT COUNT(m) FROM OrderMessage m
            WHERE m.order.id = :orderId AND m.sender.id <> :readerId AND m.id > :readUpto
            """)
    long countUnread(@Param("orderId") Long orderId, @Param("readerId") Long readerId, @Param("readUpto") long readUpto);

    long countBySenderIdAndCreatedAtAfter(Long senderId, Instant after);
}
