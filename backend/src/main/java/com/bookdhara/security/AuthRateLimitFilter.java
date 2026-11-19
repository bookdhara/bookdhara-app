package com.bookdhara.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits how often one IP address can call the login endpoints, so bots can't guess passwords or
 * flood people's inboxes (and use up our free email quota) with codes. Counts are kept in memory,
 * which is fine for our single server.
 */
@Component
// Runs after Spring Security's filters (order -100), so a 429 still carries CORS headers.
@Order(0)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final int codeRequestsPerWindow;
    private final int loginAttemptsPerWindow;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(@Value("${bookdhara.rate-limit.code-requests:10}") int codeRequestsPerWindow,
                               @Value("${bookdhara.rate-limit.login-attempts:30}") int loginAttemptsPerWindow,
                               Clock clock) {
        this.codeRequestsPerWindow = codeRequestsPerWindow;
        this.loginAttemptsPerWindow = loginAttemptsPerWindow;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean codeRequest = request.getRequestURI().equals("/api/auth/otp/request");
        String bucket = (codeRequest ? "code:" : "login:") + clientIp(request);
        int limit = codeRequest ? codeRequestsPerWindow : loginAttemptsPerWindow;

        if (!allow(bucket, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"Too many tries from your network. Please wait a few minutes.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String bucket, int limit) {
        long now = clock.millis();
        Deque<Long> times = hits.computeIfAbsent(bucket, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < now - WINDOW_MS) {
                times.pollFirst();
            }
            if (times.size() >= limit) {
                return false;
            }
            times.addLast(now);
        }
        if (hits.size() > 50_000) {
            prune(now);
        }
        return true;
    }

    private void prune(long now) {
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < now - WINDOW_MS;
            }
        });
    }

    /**
     * Render runs behind Cloudflare, which sets CF-Connecting-IP to the real visitor and overwrites
     * any value a client sends. Without it (local runs, tests) we use the connection's address.
     */
    static String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        return cf != null && !cf.isBlank() ? cf.trim() : request.getRemoteAddr();
    }

    /** For tests. */
    void reset() {
        hits.clear();
    }
}
