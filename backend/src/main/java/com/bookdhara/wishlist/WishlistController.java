package com.bookdhara.wishlist;

import com.bookdhara.book.BookDtos.BookSummary;
import com.bookdhara.wishlist.WishlistService.AlertView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WishlistController {

    public record AlertRequest(@NotBlank @Size(max = 200) String query, @Size(max = 60) String city) {
    }

    private final WishlistService service;

    public WishlistController(WishlistService service) {
        this.service = service;
    }

    @GetMapping("/api/me/wishlist")
    public List<BookSummary> saved(@AuthenticationPrincipal Long userId) {
        return service.saved(userId);
    }

    @PutMapping("/api/me/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@AuthenticationPrincipal Long userId, @PathVariable Long bookId) {
        service.save(userId, bookId);
    }

    @DeleteMapping("/api/me/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@AuthenticationPrincipal Long userId, @PathVariable Long bookId) {
        service.unsave(userId, bookId);
    }

    @GetMapping("/api/me/alerts")
    public List<AlertView> alerts(@AuthenticationPrincipal Long userId) {
        return service.alertsOf(userId);
    }

    @PostMapping("/api/me/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertView addAlert(@AuthenticationPrincipal Long userId, @Valid @RequestBody AlertRequest r) {
        return service.addAlert(userId, r.query(), r.city());
    }

    @DeleteMapping("/api/me/alerts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAlert(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.removeAlert(userId, id);
    }
}
