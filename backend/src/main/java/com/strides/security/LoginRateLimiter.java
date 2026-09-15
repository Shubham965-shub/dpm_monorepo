package com.strides.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-email failed-login lockout - the reference implementation this was ported from
 * has NO brute-force protection at all (unlimited login attempts), which this closes. In-memory is
 * sufficient for this app's single-instance, low-concurrency deployment (see backend/CLAUDE.md);
 * a multi-instance deployment would need this moved to a shared store instead.
 */
@ApplicationScoped
public class LoginRateLimiter {

    // OWASP Authentication Cheat Sheet: lock out after a small fixed number of failures rather than
    // allowing unlimited guesses.
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        AttemptState state = attempts.get(normalize(email));
        return state != null && state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil);
    }

    public void recordFailure(String email) {
        Instant now = Instant.now();
        evictStale(now);
        attempts.compute(normalize(email), (key, state) -> {
            if (state == null || state.windowStart.plus(ATTEMPT_WINDOW).isBefore(now)) {
                state = new AttemptState(now);
            }
            state.count++;
            if (state.count >= MAX_ATTEMPTS) {
                state.lockedUntil = now.plus(LOCKOUT_DURATION);
            }
            return state;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(normalize(email));
    }

    /** Bounds map growth without a scheduled job: sweeps entries whose lockout/window has lapsed. */
    private void evictStale(Instant now) {
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            Instant staleAfter = state.lockedUntil != null ? state.lockedUntil : state.windowStart.plus(ATTEMPT_WINDOW);
            return staleAfter.isBefore(now);
        });
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        final Instant windowStart;
        int count;
        Instant lockedUntil;

        AttemptState(Instant windowStart) {
            this.windowStart = windowStart;
        }
    }
}
