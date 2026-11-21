package com.bookdhara.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/** Emails people when an order needs them. Runs only after the order is saved, in the background. */
@Component
public class OrderEmails {

    static final String APP_URL = "https://bookdhara.github.io/bookdhara-app/";
    private static final Logger log = LoggerFactory.getLogger(OrderEmails.class);

    private final EmailNotifier notifier;

    public OrderEmails(EmailNotifier notifier) {
        this.notifier = notifier;
    }

    @Async
    @TransactionalEventListener
    public void on(OrderEvent e) {
        if (e.toEmail() == null || e.toEmail().isBlank()) {
            return;
        }
        try {
            Message m = message(e);
            notifier.send(e.toEmail(), m.subject(), m.text() + "\n\nOpen BookDhara: " + APP_URL
                    + "\n\nKitaabein behti rahein.", html(e, m));
        } catch (RuntimeException ex) {
            log.warn("Could not prepare order email for order {}: {}", e.orderId(), ex.getMessage());
        }
    }

    record Message(String subject, String heading, String text) {
    }

    static Message message(OrderEvent e) {
        String title = oneLine(e.bookTitle());
        String other = e.otherName() == null || e.otherName().isBlank() ? "Someone" : oneLine(e.otherName());
        return switch (e.type()) {
            case NEW_ORDER -> new Message(
                    "📦 Your book sold: " + title,
                    "Someone bought your book!",
                    other + " bought \"" + title + "\". " + (e.pickup()
                            ? "It's a local pickup: meet them, and when they have the book, ask for the 4-digit code "
                              + "in their app and enter it in BookDhara (Profile > My orders > Sold) to get paid."
                            : "It's a delivery: send it to the address in the app (Profile > My orders > Sold), "
                              + "then tap \"Mark as shipped\"."));
            case SHIPPED -> new Message(
                    "🚚 Your book is on its way: " + title,
                    "Your book is on its way",
                    other + " has shipped \"" + title + "\"."
                            + (e.note() == null || e.note().isBlank() ? "" : " Tracking: " + oneLine(e.note()) + ".")
                            + " When it arrives, open the order in BookDhara and tap \"I got it\".");
            case COMPLETED -> new Message(
                    "✅ Sale complete: " + title,
                    "Sale complete",
                    "The sale of \"" + title + "\" is complete. Your earnings are in Profile > Wallet.");
            case CANCELLED -> new Message(
                    "Order cancelled: " + title,
                    "Order cancelled",
                    "The order for \"" + title + "\" was cancelled by " + other + "."
                            + (e.note() == null || e.note().isBlank() ? "" : " Reason: " + oneLine(e.note()) + "."));
        };
    }

    private static String html(OrderEvent e, Message m) {
        String hi = e.toName() == null || e.toName().isBlank() ? "Hi," : "Hi " + oneLine(e.toName()) + ",";
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;color:#15201E">
                  <h2 style="color:#0B6B5D;margin:0 0 16px">BookDhara</h2>
                  <p>%s</p>
                  <h3 style="margin:0 0 8px">%s</h3>
                  <p>%s</p>
                  <p style="margin:24px 0"><a href="%s" style="background:#0B6B5D;color:#fff;padding:12px 20px;border-radius:24px;text-decoration:none">Open BookDhara</a></p>
                  <p style="color:#5B6763">Kitaabein behti rahein.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(hi), HtmlUtils.htmlEscape(m.heading()),
                HtmlUtils.htmlEscape(m.text()), APP_URL);
    }

    /** User-typed text goes into a subject line: no line breaks, limited length. */
    private static String oneLine(String s) {
        if (s == null) {
            return "";
        }
        String flat = s.replaceAll("[\\r\\n\\t]+", " ").strip();
        return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
    }
}
