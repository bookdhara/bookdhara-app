package com.bookdhara.book;

import com.bookdhara.book.BookDtos.BookDetail;
import com.bookdhara.book.BookDtos.BookSummary;
import com.bookdhara.book.BookDtos.CreateBookRequest;
import com.bookdhara.book.BookDtos.SellerInfo;
import com.bookdhara.book.BookEnums.Payout;
import com.bookdhara.book.BookEnums.Status;
import com.bookdhara.common.ApiException;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    static final int MAX_PHOTOS = 3;
    static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int PAGE_SIZE = 20;

    private final BookRepository books;
    private final BookPhotoRepository photos;
    private final UserRepository users;
    private final Clock clock;

    public BookService(BookRepository books, BookPhotoRepository photos, UserRepository users, Clock clock) {
        this.books = books;
        this.photos = photos;
        this.users = users;
        this.clock = clock;
    }

    /** Credits come with a 25% bonus over the cash price, to keep books moving inside BookDhara. */
    public static int payoutAmount(int price, Payout payout) {
        return payout == Payout.CREDITS ? (int) Math.round(price * 1.25) : price;
    }

    public static String photoUrl(Long photoId) {
        return "/api/photos/" + photoId;
    }

    @Transactional
    public BookDetail create(Long sellerId, CreateBookRequest r) {
        User seller = users.findById(sellerId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Please log in again"));
        Book book = books.save(new Book(seller, Isbns.normalizeOrNull(r.isbn()), r.title().trim(), trimToNull(r.author()),
                r.mrp(), r.price(), r.condition(), trimToNull(r.note()), r.city().trim(), r.pincode(),
                r.allowsPickup(), r.allowsShipping(), r.payout(), clock.instant()));
        return toDetail(book, sellerId);
    }

    /** How Home can order results. */
    public enum SortBy { NEWEST, PRICE_LOW, PRICE_HIGH }

    /** Home filters. Every field is optional. */
    public record BrowseFilter(String q, String city, BookEnums.Condition condition, Integer maxPrice, SortBy sort) {
    }

    @Transactional(readOnly = true)
    public List<BookSummary> browse(Long viewerId, BrowseFilter f, int page) {
        Specification<Book> spec = BookSpecs.available().and(BookSpecs.notSoldBy(viewerId));
        if (f.q() != null && !f.q().isBlank()) {
            spec = spec.and(BookSpecs.matches(f.q().trim()));
        }
        if (f.city() != null && !f.city().isBlank()) {
            spec = spec.and(BookSpecs.inCity(f.city()));
        }
        if (f.condition() != null) {
            spec = spec.and(BookSpecs.withCondition(f.condition()));
        }
        if (f.maxPrice() != null && f.maxPrice() > 0) {
            spec = spec.and(BookSpecs.priceAtMost(f.maxPrice()));
        }
        Sort sort = switch (f.sort() == null ? SortBy.NEWEST : f.sort()) {
            case PRICE_LOW -> Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createdAt"));
            case PRICE_HIGH -> Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createdAt"));
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        var pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort);
        return toSummaries(books.findAll(spec, pageable).getContent());
    }

    /** Cities that currently have books for sale, for the Home city filter. */
    @Transactional(readOnly = true)
    public List<String> cities() {
        return books.findAvailableCities();
    }

    @Transactional(readOnly = true)
    public List<BookSummary> listingsOf(Long sellerId) {
        return toSummaries(books.findBySellerIdAndStatusNotOrderByCreatedAtDesc(sellerId, Status.REMOVED));
    }

    @Transactional(readOnly = true)
    public BookDetail detail(Long bookId, Long viewerId) {
        Book book = load(bookId);
        boolean mine = book.getSeller().getId().equals(viewerId);
        if (book.getStatus() == Status.REMOVED && !mine) {
            throw notFound();
        }
        return toDetail(book, viewerId);
    }

    @Transactional
    public void remove(Long bookId, Long userId) {
        Book book = loadOwned(bookId, userId);
        if (book.getStatus() != Status.AVAILABLE) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOK_NOT_AVAILABLE",
                    "This book is part of an order and can't be removed right now.");
        }
        book.changeStatus(Status.REMOVED, clock.instant());
    }

    @Transactional
    public BookPhoto addPhoto(Long bookId, Long userId, String contentType, byte[] data) {
        Book book = loadOwned(bookId, userId);
        if (contentType == null || !PHOTO_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_TYPE", "Photos must be JPG, PNG or WebP.");
        }
        if (data.length == 0 || data.length > MAX_PHOTO_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_SIZE", "Each photo must be under 2 MB.");
        }
        long existing = photos.countByBookId(book.getId());
        if (existing >= MAX_PHOTOS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PHOTO_LIMIT", "You can add up to 3 photos.");
        }
        return photos.save(new BookPhoto(book.getId(), (int) existing, contentType, data, clock.instant()));
    }

    @Transactional(readOnly = true)
    public BookPhoto photo(Long photoId) {
        return photos.findById(photoId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Photo not found"));
    }

    Book load(Long bookId) {
        return books.findById(bookId).orElseThrow(BookService::notFound);
    }

    private Book loadOwned(Long bookId, Long userId) {
        Book book = load(bookId);
        if (!book.getSeller().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_YOUR_BOOK", "Only the seller can change this listing.");
        }
        return book;
    }

    private List<BookSummary> toSummaries(List<Book> list) {
        Map<Long, List<Long>> photoIds = photoIdsByBook(list.stream().map(Book::getId).toList());
        return list.stream().map(b -> {
            List<Long> ids = photoIds.getOrDefault(b.getId(), List.of());
            return new BookSummary(b.getId(), b.getTitle(), b.getAuthor(), b.getPrice(), b.getMrp(), b.getCondition(),
                    b.getCity(), ids.isEmpty() ? null : photoUrl(ids.get(0)), b.getCreatedAt());
        }).toList();
    }

    private BookDetail toDetail(Book b, Long viewerId) {
        boolean mine = b.getSeller().getId().equals(viewerId);
        List<String> urls = photoIdsByBook(List.of(b.getId())).getOrDefault(b.getId(), List.of())
                .stream().map(BookService::photoUrl).toList();
        return new BookDetail(b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPrice(), b.getMrp(),
                b.getCondition(), b.getNote(), b.getCity(), b.isAllowsPickup(), b.isAllowsShipping(), b.getStatus(),
                urls, new SellerInfo(b.getSeller().getId(), b.getSeller().getName()), mine,
                mine ? b.getPayout() : null, mine ? payoutAmount(b.getPrice(), b.getPayout()) : null, b.getCreatedAt());
    }

    private Map<Long, List<Long>> photoIdsByBook(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return photos.findRefsByBookIds(bookIds).stream().collect(Collectors.groupingBy(
                BookPhotoRepository.PhotoRef::getBookId, LinkedHashMap::new,
                Collectors.mapping(BookPhotoRepository.PhotoRef::getId, Collectors.toList())));
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "This book is no longer listed.");
    }
}
