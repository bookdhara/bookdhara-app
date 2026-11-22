package com.bookdhara.wishlist;

import com.bookdhara.book.Book;
import com.bookdhara.book.BookDtos.BookSummary;
import com.bookdhara.book.BookEnums.Status;
import com.bookdhara.book.BookRepository;
import com.bookdhara.book.BookService;
import com.bookdhara.common.ApiException;
import com.bookdhara.notify.EmailNotifier;
import com.bookdhara.safety.UserBlockRepository;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Saved books, and "tell me when it's listed" alerts that email people when a match goes up. */
@Service
public class WishlistService {

    static final int MAX_SAVED = 200;
    static final int MAX_ALERTS = 20;
    /** At most one email per alert in this window, even if several copies are listed. */
    static final Duration ALERT_GAP = Duration.ofHours(1);
    public static final String APP_URL = "https://bookdhara.github.io/bookdhara-app/";

    private static final Logger log = LoggerFactory.getLogger(WishlistService.class);

    public record AlertView(Long id, String query, String city, Instant createdAt) {
    }

    private final WishlistRepository wishlist;
    private final BookAlertRepository alerts;
    private final BookRepository books;
    private final BookService bookService;
    private final UserRepository users;
    private final UserBlockRepository blocks;
    private final EmailNotifier notifier;
    private final Clock clock;

    public WishlistService(WishlistRepository wishlist, BookAlertRepository alerts, BookRepository books,
                           BookService bookService, UserRepository users, UserBlockRepository blocks,
                           EmailNotifier notifier, Clock clock) {
        this.wishlist = wishlist;
        this.alerts = alerts;
        this.books = books;
        this.bookService = bookService;
        this.users = users;
        this.blocks = blocks;
        this.notifier = notifier;
        this.clock = clock;
    }

    // ---- Saved books ----

    @Transactional
    public void save(Long userId, Long bookId) {
        Book book = books.findById(bookId).orElseThrow(WishlistService::bookNotFound);
        if (book.getStatus() == Status.REMOVED || blocks.existsBetween(userId, book.getSeller().getId())) {
            throw bookNotFound();
        }
        if (wishlist.existsByUserIdAndBookId(userId, bookId)) {
            return;
        }
        if (wishlist.countByUserId(userId) >= MAX_SAVED) {
            throw new ApiException(HttpStatus.CONFLICT, "WISHLIST_FULL", "Your saved list is full. Remove a few books first.");
        }
        wishlist.save(new WishlistItem(userId, bookId, clock.instant()));
    }

    @Transactional
    public void unsave(Long userId, Long bookId) {
        wishlist.remove(userId, bookId);
    }

    @Transactional(readOnly = true)
    public boolean isSaved(Long userId, Long bookId) {
        return wishlist.existsByUserIdAndBookId(userId, bookId);
    }

    /** Saved books that are still listed (sold ones show as sold; removed ones drop out). */
    @Transactional(readOnly = true)
    public List<BookSummary> saved(Long userId) {
        List<Long> ids = wishlist.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WishlistItem::getBookId).toList();
        return bookService.summariesInOrder(ids).stream()
                .filter(b -> b.status() != Status.REMOVED)
                .toList();
    }

    // ---- Alerts ----

    @Transactional
    public AlertView addAlert(Long userId, String rawQuery, String rawCity) {
        String query = rawQuery == null ? "" : rawQuery.strip().replaceAll("\\s+", " ");
        if (query.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ALERT_TOO_SHORT", "Type at least 3 letters of the title or author.");
        }
        if (query.length() > 120) {
            query = query.substring(0, 120);
        }
        String city = rawCity == null || rawCity.isBlank() ? null : rawCity.strip();
        if (alerts.existsByUserIdAndQueryLower(userId, query.toLowerCase())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALERT_EXISTS", "You already have an alert for this.");
        }
        if (alerts.countByUserId(userId) >= MAX_ALERTS) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_ALERTS", "You can have up to 20 alerts. Remove one first.");
        }
        BookAlert saved = alerts.save(new BookAlert(userId, query, city, clock.instant()));
        return view(saved);
    }

    @Transactional
    public void removeAlert(Long userId, Long alertId) {
        alerts.remove(alertId, userId);
    }

    @Transactional(readOnly = true)
    public List<AlertView> alertsOf(Long userId) {
        return alerts.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WishlistService::view).toList();
    }

    /** After a new listing is saved: email everyone whose alert matches. Runs in the background. */
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookListed(BookListed event) {
        Book book = books.findById(event.bookId()).orElse(null);
        if (book == null || book.getStatus() != Status.AVAILABLE) {
            return;
        }
        Long sellerId = book.getSeller().getId();
        String isbn = book.getIsbn() == null ? "-" : book.getIsbn();
        String author = book.getAuthor() == null ? "" : book.getAuthor().toLowerCase();
        Instant now = clock.instant();
        for (BookAlert alert : alerts.matching(sellerId, book.getTitle().toLowerCase(), author, isbn,
                book.getCity().toLowerCase())) {
            if (alert.getLastNotifiedAt() != null && alert.getLastNotifiedAt().isAfter(now.minus(ALERT_GAP))) {
                continue;
            }
            if (blocks.existsBetween(alert.getUserId(), sellerId)) {
                continue;
            }
            User user = users.findById(alert.getUserId()).orElse(null);
            if (user == null || user.isDeleted() || user.getEmail() == null) {
                continue;
            }
            alert.markNotified(now);
            sendAlertEmail(user, alert, book);
        }
    }

    private void sendAlertEmail(User user, BookAlert alert, Book book) {
        try {
            String link = APP_URL + "?book=" + book.getId();
            String title = book.getTitle().replaceAll("[\\r\\n\\t]+", " ").strip();
            String subject = "📚 \"" + (title.length() > 80 ? title.substring(0, 80) + "…" : title)
                    + "\" is on BookDhara for ₹" + book.getPrice();
            String text = "Good news! A book matching your alert \"" + alert.getQuery() + "\" was just listed: \""
                    + title + "\"" + (book.getAuthor() == null ? "" : " by " + book.getAuthor())
                    + ", ₹" + book.getPrice() + " in " + book.getCity() + ".\n\nSee it: " + link
                    + "\n\nTo stop these emails, remove the alert in Profile > Book alerts.";
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#15201E">
                      <h2 style="color:#0B6B5D;margin:0 0 16px">BookDhara</h2>
                      <p>%s</p>
                      <p style="margin:24px 0"><a href="%s" style="background:#0B6B5D;color:#fff;padding:12px 20px;border-radius:24px;text-decoration:none">See the book</a></p>
                      <p style="color:#5B6763;font-size:13px">To stop these emails, remove the alert in Profile &gt; Book alerts.</p>
                    </div>
                    """.formatted(HtmlUtils.htmlEscape(text.substring(0, text.indexOf("\n\nSee it")))
                    , link);
            notifier.send(user.getEmail(), subject, text, html);
        } catch (RuntimeException e) {
            log.warn("Could not send alert email for book {}: {}", book.getId(), e.getMessage());
        }
    }

    private static AlertView view(BookAlert a) {
        return new AlertView(a.getId(), a.getQuery(), a.getCity(), a.getCreatedAt());
    }

    private static ApiException bookNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "This book is no longer listed.");
    }
}
