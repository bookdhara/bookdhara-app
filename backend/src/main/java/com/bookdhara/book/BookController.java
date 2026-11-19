package com.bookdhara.book;

import com.bookdhara.book.BookDtos.BookDetail;
import com.bookdhara.book.BookDtos.BookSummary;
import com.bookdhara.book.BookDtos.CreateBookRequest;
import com.bookdhara.book.BookDtos.IsbnInfo;
import com.bookdhara.book.BookDtos.PhotoUploaded;
import com.bookdhara.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@RestController
public class BookController {

    private final BookService service;
    private final IsbnLookup isbnLookup;

    public BookController(BookService service, IsbnLookup isbnLookup) {
        this.service = service;
        this.isbnLookup = isbnLookup;
    }

    @GetMapping("/api/isbn/{isbn}")
    public IsbnInfo lookup(@PathVariable String isbn) {
        String normalized = Isbns.normalize(isbn);
        return isbnLookup.find(normalized)
                .map(i -> new IsbnInfo(i.isbn(), i.title(), i.author(), i.coverUrl()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ISBN_NOT_FOUND",
                        "We couldn't find this ISBN. You can type the title and author yourself."));
    }

    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDetail create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateBookRequest request) {
        return service.create(userId, request);
    }

    @GetMapping("/api/books")
    public List<BookSummary> browse(@AuthenticationPrincipal Long userId,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String city,
                                    @RequestParam(required = false) BookEnums.Condition condition,
                                    @RequestParam(required = false) Integer maxPrice,
                                    @RequestParam(required = false) BookService.SortBy sort,
                                    @RequestParam(defaultValue = "0") int page) {
        return service.browse(userId, new BookService.BrowseFilter(q, city, condition, maxPrice, sort), page);
    }

    @GetMapping("/api/books/cities")
    public List<String> cities() {
        return service.cities();
    }

    @GetMapping("/api/books/{id}")
    public BookDetail detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return service.detail(id, userId);
    }

    @DeleteMapping("/api/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.remove(id, userId);
    }

    @GetMapping("/api/me/books")
    public List<BookSummary> myListings(@AuthenticationPrincipal Long userId) {
        return service.listingsOf(userId);
    }

    @PostMapping(value = "/api/books/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoUploaded addPhoto(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        BookPhoto photo = service.addPhoto(id, userId, file.getContentType(), file.getBytes());
        return new PhotoUploaded(photo.getId(), BookService.photoUrl(photo.getId()));
    }

    /** Public, so the app can show photos with a plain image tag. Photos never change, so cache them. */
    @GetMapping("/api/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable Long photoId) {
        BookPhoto photo = service.photo(photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(photo.getData());
    }
}
