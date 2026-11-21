package com.bookdhara.safety;

import com.bookdhara.auth.OtpCodeRepository;
import com.bookdhara.book.Book;
import com.bookdhara.book.BookEnums.Status;
import com.bookdhara.book.BookPhotoRepository;
import com.bookdhara.book.BookRepository;
import com.bookdhara.common.ApiException;
import com.bookdhara.order.Order;
import com.bookdhara.order.OrderRepository;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * "Delete my account": takes down their listings and photos, removes personal details and login
 * methods, and ends every session. Past orders stay for the other person, shown as "Deleted user".
 */
@Service
public class AccountDeletionService {

    private static final List<Order.Status> OPEN = List.of(Order.Status.PLACED, Order.Status.SHIPPED);

    private final UserRepository users;
    private final OrderRepository orders;
    private final BookRepository books;
    private final BookPhotoRepository photos;
    private final OtpCodeRepository otpCodes;
    private final UserBlockRepository blocks;
    private final Clock clock;

    public AccountDeletionService(UserRepository users, OrderRepository orders, BookRepository books,
                                  BookPhotoRepository photos, OtpCodeRepository otpCodes,
                                  UserBlockRepository blocks, Clock clock) {
        this.users = users;
        this.orders = orders;
        this.books = books;
        this.photos = photos;
        this.otpCodes = otpCodes;
        this.blocks = blocks;
        this.clock = clock;
    }

    @Transactional
    public void delete(Long userId) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Please log in again"));
        if (orders.existsByBuyerIdAndStatusIn(userId, OPEN) || orders.existsBySellerIdAndStatusIn(userId, OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "OPEN_ORDERS",
                    "Finish or cancel your open orders first, then you can delete your account.");
        }
        Instant now = clock.instant();
        List<Book> forSale = books.findBySellerIdAndStatus(userId, Status.AVAILABLE);
        forSale.forEach(b -> b.changeStatus(Status.REMOVED, now));
        if (!forSale.isEmpty()) {
            photos.deleteByBookIds(forSale.stream().map(Book::getId).toList());
        }
        if (user.getEmail() != null) {
            otpCodes.deleteByEmail(user.getEmail());
        }
        blocks.deleteAllFor(userId);
        user.deleteAccount(now);
    }
}
