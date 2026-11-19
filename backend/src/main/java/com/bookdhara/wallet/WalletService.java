package com.bookdhara.wallet;

import com.bookdhara.order.Order;
import com.bookdhara.order.OrderRepository;
import com.bookdhara.wallet.CreditEntry.Kind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class WalletService {

    public record Entry(Long id, int amount, Kind kind, Long orderId, String note, Instant createdAt) {
    }

    public record Wallet(long credits, long creditsOnHold, long cashEarned, List<Entry> history) {
    }

    private final CreditEntryRepository entries;
    private final OrderRepository orders;
    private final Clock clock;

    public WalletService(CreditEntryRepository entries, OrderRepository orders, Clock clock) {
        this.entries = entries;
        this.orders = orders;
        this.clock = clock;
    }

    public long balance(Long userId) {
        return entries.balanceOf(userId);
    }

    /** Adds (positive) or spends (negative) credits. Callers check the balance first. */
    public void record(Long userId, int amount, Kind kind, Long orderId, String note) {
        if (amount != 0) {
            entries.save(new CreditEntry(userId, amount, kind, orderId, note, clock.instant()));
        }
    }

    @Transactional(readOnly = true)
    public Wallet walletOf(Long userId) {
        List<Entry> history = entries.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(e -> new Entry(e.getId(), e.getAmount(), e.getKind(), e.getOrderId(), e.getNote(), e.getCreatedAt()))
                .toList();
        long onHold = orders.pendingCreditsFor(userId, EnumSet.of(Order.Status.PLACED, Order.Status.SHIPPED));
        return new Wallet(entries.balanceOf(userId), onHold, orders.cashEarnedBy(userId), history);
    }
}
