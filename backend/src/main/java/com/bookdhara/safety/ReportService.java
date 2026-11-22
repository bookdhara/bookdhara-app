package com.bookdhara.safety;

import com.bookdhara.book.BookRepository;
import com.bookdhara.common.ApiException;
import com.bookdhara.notify.EmailNotifier;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Instant;

/** Anyone can report a listing or a person once. The team is emailed and reviews it. */
@Service
public class ReportService {

    static final int MAX_PER_DAY = 20;

    public record ReportFiled(Long reportId, String reporterName, Report.TargetType type, Long targetId,
                              String targetLabel, Report.Reason reason, String details) {
    }

    private final ReportRepository reports;
    private final BookRepository books;
    private final UserRepository users;
    private final AdminProperties admins;
    private final EmailNotifier notifier;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReportService(ReportRepository reports, BookRepository books, UserRepository users, AdminProperties admins,
                         EmailNotifier notifier, ApplicationEventPublisher events, Clock clock) {
        this.reports = reports;
        this.books = books;
        this.users = users;
        this.admins = admins;
        this.notifier = notifier;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void report(Long reporterId, Report.TargetType type, Long targetId, Report.Reason reason, String rawDetails) {
        String label;
        if (type == Report.TargetType.BOOK) {
            var book = books.findById(targetId).orElseThrow(() -> notFound("Listing not found"));
            if (book.getSeller().getId().equals(reporterId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_SELF", "You can't report your own listing.");
            }
            label = "listing \"" + book.getTitle() + "\" (#" + targetId + ") by user #" + book.getSeller().getId();
        } else {
            var user = users.findById(targetId).orElseThrow(() -> notFound("User not found"));
            if (user.getId().equals(reporterId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_SELF", "You can't report yourself.");
            }
            label = "user " + (user.getName() == null ? "" : user.getName() + " ") + "(#" + targetId + ")";
        }
        if (reports.existsByReporterIdAndTargetTypeAndTargetId(reporterId, type, targetId)) {
            return; // Already reported: nothing more to do.
        }
        Instant now = clock.instant();
        if (reports.countByReporterIdAndCreatedAtAfter(reporterId, now.minusSeconds(86_400)) >= MAX_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REPORTS", "You've sent a lot of reports today. Try again tomorrow.");
        }
        String details = rawDetails == null || rawDetails.isBlank() ? null : rawDetails.strip();
        if (details != null && details.length() > 500) {
            details = details.substring(0, 500);
        }
        Report saved = reports.save(new Report(reporterId, type, targetId, reason, details, now));
        String reporterName = users.findById(reporterId).map(u -> u.getName()).orElse(null);
        events.publishEvent(new ReportFiled(saved.getId(), reporterName, type, targetId, label, reason, details));
    }

    @Async
    @TransactionalEventListener
    public void emailTeam(ReportFiled r) {
        String text = "New report #" + r.reportId() + " about " + r.targetLabel() + "\nReason: " + r.reason()
                + (r.details() == null ? "" : "\nDetails: " + r.details())
                + "\nFrom: " + (r.reporterName() == null ? "a user" : r.reporterName());
        String html = "<pre style=\"font-family:Arial,sans-serif;white-space:pre-wrap\">" + HtmlUtils.htmlEscape(text) + "</pre>";
        for (String admin : admins.emailList()) {
            notifier.send(admin, "🚩 BookDhara report #" + r.reportId() + ": " + r.reason(), text, html);
        }
    }

    private static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
