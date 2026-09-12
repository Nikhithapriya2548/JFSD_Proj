package com.omnishop.user.security;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Brute-force mitigation for login: max 5 attempts per 15 minutes per
 * lower-cased email. In-memory is deliberate here — single-instance demo
 * scope; a Redis-backed counter would be the clustered answer (noted in
 * README as the production follow-up).
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private record Attempt(int count, long windowStart) {}

    public synchronized void checkAllowed(String email) {
        String key = email.toLowerCase();
        Attempt a = attempts.get(key);
        long now = System.currentTimeMillis();
        if (a == null || now - a.windowStart() > WINDOW_MS) {
            return;
        }
        if (a.count() >= MAX_ATTEMPTS) {
            throw new TooManyAttemptsException(
                    "Too many login attempts — try again in 15 minutes");
        }
    }

    public synchronized void recordFailure(String email) {
        String key = email.toLowerCase();
        long now = System.currentTimeMillis();
        Attempt a = attempts.get(key);
        if (a == null || now - a.windowStart() > WINDOW_MS) {
            attempts.put(key, new Attempt(1, now));
        } else {
            attempts.put(key, new Attempt(a.count() + 1, a.windowStart()));
        }
    }

    public synchronized void recordSuccess(String email) {
        attempts.remove(email.toLowerCase());
    }

    public static class TooManyAttemptsException extends RuntimeException {
        public TooManyAttemptsException(String msg) { super(msg); }
    }
}
