package com.bookdhara.order;

import com.bookdhara.chat.OrderMessageRepository;
import com.bookdhara.notify.OrderEvent;
import org.springframework.context.ApplicationEventPublisher;

import com.bookdhara.auth.PhoneNumbers;
import com.bookdhara.book.Book;
import com.bookdhara.book.BookEnums.Status;
import com.bookdhara.book.BookPhotoRepository;
import com.bookdhara.book.BookRepository;
import com.bookdhara.book.BookService;
import com.bookdhara.common.ApiException;
import com.bookdhara.order.Order.DeliveryMethod;
import com.bookdhara.order.OrderDtos.Actions;
import com.bookdhara.order.OrderDtos.Address;
import com.bookdhara.order.OrderDtos.BookInfo;
import com.bookdhara.order.OrderDtos.OrderView;
import com.bookdhara.order.OrderDtos.PlaceOrderRequest;
import com.bookdhara.order.OrderDtos.Quote;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import com.bookdhara.wallet.CreditEntry.Kind;
import com.bookdhara.wallet.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Buying a book, in test-payment mode until a payment gateway is connected.
 * Money flow: the buyer pays book price (part of it may be credits) + shipping + platform fee.
 * The seller is paid only when the order completes: pickup code entered, or buyer confirms delivery.
 */
@Service
public class OrderService {

    static final int SHIPPING_FEE = 60;
    static final int PLATFORM_FEE_PICKUP = 5;
    static final int PLATFORM_FEE_SHIPPING = 15;
    static final int HANDOVER_BONUS = 5;

    private final OrderRepository orders;
    private final BookRepository books;
    private final BookPhotoRepository photos;
    private final UserRepository users;
    private final WalletService wallet;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final OrderMessageRepository messages;
    private final SecureRandom random = new SecureRandom();

    public OrderService(OrderRepository orders, BookRepository books, BookPhotoRepository photos,
                        UserRepository users, WalletService wallet, Clock clock, ApplicationEventPublisher events,
                        OrderMessageRepository messages) {
        this.orders = orders;
        this.books = books;
        this.photos = photos;
        this.users = users;
        this.wallet = wallet;
        this.clock = clock;
        this.events = events;
        this.messages = messages;
    }

    /**
     * Orders waiting for this person: sellers must hand over or ship, buyers must confirm delivery,
     * and any open order with chat messages they haven't read.
     */
    @Transactional(readOnly = true)
    public List<OrderView> needingAttention(Long userId) {
        java.util.LinkedHashMap<Long, Order> byId = new java.util.LinkedHashMap<>();
        orders.findBySellerIdAndStatusOrderByCreatedAtDesc(userId, Order.Status.PLACED).forEach(o -> byId.put(o.getId(), o));
        orders.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, Order.Status.SHIPPED).forEach(o -> byId.put(o.getId(), o));
        java.util.stream.Stream.concat(
                        orders.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, Order.Status.PLACED).stream(),
                        orders.findBySellerIdAndStatusOrderByCreatedAtDesc(userId, Order.Status.SHIPPED).stream())
                .filter(o -> unread(o, userId) > 0)
                .forEach(o -> byId.putIfAbsent(o.getId(), o));
        List<Order> list = List.copyOf(byId.values());
        Map<Long, String> covers = coverUrls(list.stream().map(o -> o.getBook().getId()).toList());
        return list.stream().map(o -> view(o, userId, covers)).toList();
    }

    /** Emails the other person once this transaction commits. */
    private void tell(OrderEvent.Type type, Order o, User to, User other, String note) {
        events.publishEvent(new OrderEvent(type, o.getId(), to.getEmail(), to.getName(), other.getName(),
                o.getBook().getTitle(), o.getDeliveryMethod() == DeliveryMethod.PICKUP, note));
    }

    @Transactional(readOnly = true)
    public Quote quote(Long buyerId, Long bookId, DeliveryMethod method, int creditsWanted) {
        Book book = books.findById(bookId).orElseThrow(OrderService::bookGone);
        long balance = wallet.balance(buyerId);
        int maxCredits = (int) Math.min(balance, book.getPrice());
        int credits = Math.max(0, Math.min(creditsWanted, maxCredits));
        int shipping = method == DeliveryMethod.SHIPPING ? SHIPPING_FEE : 0;
        int fee = platformFee(method);
        return new Quote(book.getPrice(), credits, shipping, fee, book.getPrice() - credits + shipping + fee,
                balance, maxCredits);
    }

    @Transactional
    public OrderView place(Long buyerId, PlaceOrderRequest r) {
        User buyer = users.findByIdForUpdate(buyerId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Please log in again"));
        Book book = books.findByIdForUpdate(r.bookId()).orElseThrow(OrderService::bookGone);

        if (book.getSeller().getId().equals(buyerId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWN_BOOK", "You can't buy your own book.");
        }
        if (book.getStatus() != Status.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOK_NOT_AVAILABLE", "Sorry, someone else just bought this book.");
        }
        if (r.method() == DeliveryMethod.PICKUP && !book.isAllowsPickup()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PICKUP_NOT_OFFERED", "This seller doesn't offer local pickup.");
        }
        if (r.method() == DeliveryMethod.SHIPPING && !book.isAllowsShipping()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SHIPPING_NOT_OFFERED", "This seller doesn't offer delivery.");
        }
        if (r.method() == DeliveryMethod.SHIPPING && r.address() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADDRESS_REQUIRED", "Add a delivery address.");
        }

        long balance = wallet.balance(buyerId);
        if (r.creditsToUse() > balance) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_ENOUGH_CREDITS", "You don't have that many credits.");
        }
        if (r.creditsToUse() > book.getPrice()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_CREDITS",
                    "Credits can pay for the book price only, not delivery or fees.");
        }

        Instant now = clock.instant();
        String code = r.method() == DeliveryMethod.PICKUP ? String.format("%04d", random.nextInt(10_000)) : null;
        Order order = new Order(book, buyer, r.method(), r.creditsToUse(),
                r.method() == DeliveryMethod.SHIPPING ? SHIPPING_FEE : 0, platformFee(r.method()),
                book.getPayout(), BookService.payoutAmount(book.getPrice(), book.getPayout()), code, now);
        if (r.method() == DeliveryMethod.SHIPPING) {
            Address a = r.address();
            order.shipTo(a.name().trim(), PhoneNumbers.normalize(a.phone()), a.address().trim(), a.city().trim(), a.pincode());
        }
        order = orders.save(order);
        book.changeStatus(Status.RESERVED, now);
        wallet.record(buyerId, -r.creditsToUse(), Kind.PURCHASE, order.getId(), "Bought " + book.getTitle());
        tell(OrderEvent.Type.NEW_ORDER, order, book.getSeller(), buyer, null);
        return view(order, buyerId);
    }

    @Transactional(readOnly = true)
    public List<OrderView> mine(Long userId, String role) {
        List<Order> list = "selling".equalsIgnoreCase(role)
                ? orders.findBySellerIdOrderByCreatedAtDesc(userId)
                : orders.findByBuyerIdOrderByCreatedAtDesc(userId);
        Map<Long, String> covers = coverUrls(list.stream().map(o -> o.getBook().getId()).toList());
        return list.stream().map(o -> view(o, userId, covers)).toList();
    }

    @Transactional(readOnly = true)
    public OrderView get(Long orderId, Long userId) {
        return view(loadFor(orderId, userId), userId);
    }

    /** Delivery orders: the seller has handed the book to the courier. */
    @Transactional
    public OrderView ship(Long orderId, Long userId, String note) {
        Order o = loadFor(orderId, userId);
        requireSeller(o, userId);
        if (o.getDeliveryMethod() != DeliveryMethod.SHIPPING || o.getStatus() != Order.Status.PLACED) {
            throw invalidStep();
        }
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        o.markShipped(cleanNote, clock.instant());
        tell(OrderEvent.Type.SHIPPED, o, o.getBuyer(), o.getSeller(), cleanNote);
        return view(o, userId);
    }

    /** Delivery orders: the buyer got the book, so the seller is paid. */
    @Transactional
    public OrderView confirmReceived(Long orderId, Long userId) {
        Order o = loadFor(orderId, userId);
        requireBuyer(o, userId);
        if (o.getDeliveryMethod() != DeliveryMethod.SHIPPING || !o.isOpen()) {
            throw invalidStep();
        }
        complete(o);
        return view(o, userId);
    }

    /** Pickup orders: the seller enters the code the buyer shows once they have the book. */
    @Transactional(noRollbackFor = ApiException.class)
    public OrderView handover(Long orderId, Long userId, String code) {
        Order o = loadFor(orderId, userId);
        requireSeller(o, userId);
        if (o.getDeliveryMethod() != DeliveryMethod.PICKUP || o.getStatus() != Order.Status.PLACED) {
            throw invalidStep();
        }
        if (!o.getHandoverCode().equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CODE", "That code doesn't match. Ask the buyer to check it in their app.");
        }
        complete(o);
        wallet.record(o.getBuyer().getId(), HANDOVER_BONUS, Kind.BONUS, o.getId(), "Pickup bonus");
        wallet.record(o.getSeller().getId(), HANDOVER_BONUS, Kind.BONUS, o.getId(), "Pickup bonus");
        return view(o, userId);
    }

    /** Either side can cancel before the book is shipped or handed over. Credits go back to the buyer. */
    @Transactional
    public OrderView cancel(Long orderId, Long userId, String reason) {
        Order o = loadFor(orderId, userId);
        if (o.getStatus() != Order.Status.PLACED) {
            throw new ApiException(HttpStatus.CONFLICT, "CANT_CANCEL",
                    o.getStatus() == Order.Status.SHIPPED
                            ? "The book is already on its way. Wait for it, or contact us if there's a problem."
                            : "This order can't be cancelled.");
        }
        Instant now = clock.instant();
        boolean byBuyer = o.getBuyer().getId().equals(userId);
        o.cancel((byBuyer ? "Cancelled by buyer" : "Cancelled by seller")
                + (reason == null || reason.isBlank() ? "" : ": " + reason.trim()), now);
        tell(OrderEvent.Type.CANCELLED, o, byBuyer ? o.getSeller() : o.getBuyer(),
                byBuyer ? o.getBuyer() : o.getSeller(), reason == null || reason.isBlank() ? null : reason.trim());
        o.getBook().changeStatus(Status.AVAILABLE, now);
        wallet.record(o.getBuyer().getId(), o.getCreditsUsed(), Kind.REFUND, o.getId(),
                "Refund for " + o.getBook().getTitle());
        return view(o, userId);
    }

    private void complete(Order o) {
        Instant now = clock.instant();
        o.complete(now);
        tell(OrderEvent.Type.COMPLETED, o, o.getSeller(), o.getBuyer(), null);
        o.getBook().changeStatus(Status.SOLD, now);
        if (o.getSellerPayout() == com.bookdhara.book.BookEnums.Payout.CREDITS) {
            wallet.record(o.getSeller().getId(), o.getSellerPayoutAmount(), Kind.SALE, o.getId(),
                    "Sold " + o.getBook().getTitle());
        }
        // Cash payouts are recorded on the order; real bank transfers come with the payment gateway.
    }

    private Order loadFor(Long orderId, Long userId) {
        Order o = orders.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (!o.getBuyer().getId().equals(userId) && !o.getSeller().getId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found");
        }
        return o;
    }

    private static void requireSeller(Order o, Long userId) {
        if (!o.getSeller().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SELLER_ONLY", "Only the seller can do this.");
        }
    }

    private static void requireBuyer(Order o, Long userId) {
        if (!o.getBuyer().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BUYER_ONLY", "Only the buyer can do this.");
        }
    }

    private OrderView view(Order o, Long userId) {
        return view(o, userId, coverUrls(List.of(o.getBook().getId())));
    }

    private OrderView view(Order o, Long userId, Map<Long, String> covers) {
        boolean buyer = o.getBuyer().getId().equals(userId);
        boolean pickup = o.getDeliveryMethod() == DeliveryMethod.PICKUP;
        Book b = o.getBook();
        Address address = o.getShipAddress() == null ? null
                : new Address(o.getShipName(), o.getShipPhone(), o.getShipAddress(), o.getShipCity(), o.getShipPincode());
        Actions actions = new Actions(
                !buyer && !pickup && o.getStatus() == Order.Status.PLACED,
                buyer && !pickup && o.isOpen(),
                !buyer && pickup && o.getStatus() == Order.Status.PLACED,
                o.getStatus() == Order.Status.PLACED);
        return new OrderView(o.getId(), o.getStatus(), o.getDeliveryMethod(), buyer ? "BUYER" : "SELLER",
                new BookInfo(b.getId(), b.getTitle(), b.getAuthor(), covers.get(b.getId())),
                o.getBuyer().getName(), o.getSeller().getName(), b.getCity(),
                o.getBookPrice(), o.getCreditsUsed(), o.getShippingFee(), o.getPlatformFee(), o.getCashPaid(),
                o.getPaymentMode().name(),
                buyer && pickup && o.getStatus() == Order.Status.PLACED ? o.getHandoverCode() : null,
                buyer ? null : o.getSellerPayout(), buyer ? null : o.getSellerPayoutAmount(),
                address, o.getTrackingNote(), o.getCancelReason(), actions, o.getCreatedAt(), o.getCompletedAt(),
                unread(o, userId));
    }

    private long unread(Order o, Long userId) {
        return messages.countUnread(o.getId(), userId, o.readUptoFor(userId));
    }

    private Map<Long, String> coverUrls(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return photos.findRefsByBookIds(bookIds).stream().collect(Collectors.toMap(
                BookPhotoRepository.PhotoRef::getBookId, ref -> BookService.photoUrl(ref.getId()), (first, other) -> first));
    }

    private static int platformFee(DeliveryMethod method) {
        return method == DeliveryMethod.SHIPPING ? PLATFORM_FEE_SHIPPING : PLATFORM_FEE_PICKUP;
    }

    private static ApiException bookGone() {
        return new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "This book is no longer listed.");
    }

    private static ApiException invalidStep() {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_STEP", "This order isn't at that step.");
    }
}
