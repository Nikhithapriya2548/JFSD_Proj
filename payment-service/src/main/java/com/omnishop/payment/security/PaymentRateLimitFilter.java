package com.omnishop.payment.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal abuse guard for direct payment posts: 30/minute per client IP.
 * In-memory by design for single-instance demo scope — deliberately NOT a
 * distributed limiter (see README).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PaymentRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_PER_MINUTE = 30;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(int count, long minute) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !(HttpMethod.POST.name().equals(req.getMethod())
                && "/api/v1/payments".equals(req.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        long minute = Instant.now().getEpochSecond() / 60;
        Window w = windows.compute(ip, (k, old) ->
                (old == null || old.minute() != minute) ? new Window(1, minute)
                        : new Window(old.count() + 1, minute));
        if (w.count() > MAX_PER_MINUTE) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Payment rate limit exceeded (30/min per IP)\",\"path\":\"%s\"}",
                    Instant.now(), req.getRequestURI()));
            return;
        }
        chain.doFilter(req, res);
    }
}
