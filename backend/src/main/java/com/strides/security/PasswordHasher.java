package com.strides.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordHasher {

    // OWASP Password Storage Cheat Sheet (current guidance): bcrypt work factor >= 10; 12 gives a
    // wider safety margin against offline cracking while staying well under the ~100ms/login budget
    // this app can afford.
    private static final int BCRYPT_COST = 12;

    /**
     * A precomputed, valid bcrypt hash with no matching real password. Verifying an unknown email
     * against this (instead of short-circuiting straight to "invalid") keeps a failed login's
     * response time similar whether the account exists or not, so timing can't be used to enumerate
     * registered emails.
     */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("no-matching-password", BCRYPT_COST);

    public String hash(String plainPassword) {
        return BcryptUtil.bcryptHash(plainPassword, BCRYPT_COST);
    }

    public boolean verify(String plainPassword, String passwordHash) {
        return BcryptUtil.matches(plainPassword, passwordHash);
    }

    /** Always call this (never skip straight to `false`) when the account isn't found - see DUMMY_HASH. */
    public boolean verifyAgainstDummy(String plainPassword) {
        return BcryptUtil.matches(plainPassword, DUMMY_HASH);
    }
}
