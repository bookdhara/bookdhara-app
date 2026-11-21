package com.bookdhara.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, Order.Status status);

    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, Order.Status status);

    /** Credits a seller will get once their open orders complete. */
    @Query("""
            SELECT COALESCE(SUM(o.sellerPayoutAmount), 0) FROM Order o
            WHERE o.seller.id = :sellerId AND o.status IN :statuses
              AND o.sellerPayout = com.bookdhara.book.BookEnums.Payout.CREDITS
            """)
    long pendingCreditsFor(@Param("sellerId") Long sellerId, @Param("statuses") Collection<Order.Status> statuses);

    @Query("""
            SELECT COALESCE(SUM(o.sellerPayoutAmount), 0) FROM Order o
            WHERE o.seller.id = :sellerId AND o.status = com.bookdhara.order.Order.Status.COMPLETED
              AND o.sellerPayout = com.bookdhara.book.BookEnums.Payout.CASH
            """)
    long cashEarnedBy(@Param("sellerId") Long sellerId);
}
