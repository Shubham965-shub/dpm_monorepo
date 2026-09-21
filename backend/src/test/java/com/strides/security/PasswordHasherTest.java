package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Real bcrypt, no mocking - this class IS the hashing primitive under test. No DB involved. */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashThenVerify_roundTrips() {
        String hash = hasher.hash("correct horse battery staple");

        assertTrue(hasher.verify("correct horse battery staple", hash));
    }

    @Test
    void verify_wrongPassword_returnsFalse() {
        String hash = hasher.hash("correct horse battery staple");

        assertFalse(hasher.verify("wrong password", hash));
    }

    @Test
    void hash_isSaltedSoTheSamePasswordHashesDifferently() {
        String first = hasher.hash("same-password");
        String second = hasher.hash("same-password");

        assertNotEquals(first, second);
        assertTrue(hasher.verify("same-password", first));
        assertTrue(hasher.verify("same-password", second));
    }

    @Test
    void verifyAgainstDummy_alwaysReturnsFalse_forAnyRealPassword() {
        // DUMMY_HASH is bcrypt("no-matching-password") - any distinct guess should fail, which is
        // the whole point (spending the bcrypt cost without ever matching a real account).
        assertFalse(hasher.verifyAgainstDummy("some guess"));
        assertFalse(hasher.verifyAgainstDummy(""));
    }
}
