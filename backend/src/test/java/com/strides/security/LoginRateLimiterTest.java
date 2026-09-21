package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pure in-memory state, no collaborators to mock and no DB involved. */
class LoginRateLimiterTest {

    private final LoginRateLimiter limiter = new LoginRateLimiter();

    @Test
    void isLocked_forAnUnknownEmail_isFalse() {
        assertFalse(limiter.isLocked("nobody@strides.com"));
    }

    @Test
    void recordFailure_belowMaxAttempts_doesNotLock() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("user@strides.com");
        }

        assertFalse(limiter.isLocked("user@strides.com"));
    }

    @Test
    void recordFailure_reachingMaxAttempts_locksTheAccount() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user@strides.com");
        }

        assertTrue(limiter.isLocked("user@strides.com"));
    }

    @Test
    void recordSuccess_clearsPriorFailures() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("user@strides.com");
        }

        limiter.recordSuccess("user@strides.com");
        limiter.recordFailure("user@strides.com"); // would be the 5th if not cleared

        assertFalse(limiter.isLocked("user@strides.com"));
    }

    @Test
    void emailMatching_isCaseAndWhitespaceInsensitive() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("  User@Strides.com  ");
        }

        assertTrue(limiter.isLocked("user@strides.com"));
    }

    @Test
    void differentEmails_areTrackedIndependently() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("locked@strides.com");
        }

        assertFalse(limiter.isLocked("other@strides.com"));
    }
}
