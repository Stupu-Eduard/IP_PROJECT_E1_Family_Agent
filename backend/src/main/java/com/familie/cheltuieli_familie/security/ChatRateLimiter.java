package com.familie.cheltuieli_familie.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for the chat endpoint.
 * Limits each user to a configurable number of requests per minute.
 */
@Component
public class ChatRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, UserRequestWindow> windows = new ConcurrentHashMap<>();

    public boolean allowRequest(String userId) {
        long now = Instant.now().toEpochMilli();
        UserRequestWindow window = windows.compute(userId, (k, v) -> {
            if (v == null || now - v.windowStart > WINDOW_MILLIS) {
                return new UserRequestWindow(now, 1);
            }
            v.count++;
            return v;
        });
        return window.count <= MAX_REQUESTS_PER_MINUTE;
    }

    public long getRemainingSeconds(String userId) {
        UserRequestWindow window = windows.get(userId);
        if (window == null) return 0;
        long elapsed = Instant.now().toEpochMilli() - window.windowStart;
        long remaining = WINDOW_MILLIS - elapsed;
        return Math.max(0, remaining / 1000);
    }

    private static class UserRequestWindow {
        long windowStart;
        int count;

        UserRequestWindow(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
