package com.ecommerce.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_SECONDS = 120;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public long getRemainingLockSeconds(String email) {
        String key = normalize(email);
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil == null) return 0;

        long remaining = Duration.between(Instant.now(), attempt.lockedUntil).getSeconds() + 1;
        if (remaining <= 0) {
            attempts.remove(key);
            return 0;
        }
        return remaining;
    }

    public long recordFailure(String email) {
        String key = normalize(email);
        Attempt attempt = attempts.computeIfAbsent(key, ignored -> new Attempt());
        synchronized (attempt) {
            if (attempt.lockedUntil != null && Instant.now().isBefore(attempt.lockedUntil)) {
                return getRemainingLockSeconds(key);
            }
            attempt.failures++;
            if (attempt.failures >= MAX_FAILED_ATTEMPTS) {
                attempt.failures = 0;
                attempt.lockedUntil = Instant.now().plusSeconds(LOCK_SECONDS);
                return LOCK_SECONDS;
            }
            return 0;
        }
    }

    public void recordSuccess(String email) {
        attempts.remove(normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static final class Attempt {
        private int failures;
        private Instant lockedUntil;
    }
}
