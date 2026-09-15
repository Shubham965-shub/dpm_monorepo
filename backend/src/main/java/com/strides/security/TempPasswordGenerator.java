package com.strides.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;

/**
 * One-time temp passwords for admin-created accounts (there's no email infrastructure to send
 * reset links, so the admin relays the password out-of-band). Unambiguous alphabet: no 0/O, 1/l/I/i.
 */
@ApplicationScoped
public class TempPasswordGenerator {

    private static final String LETTERS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALPHABET = LETTERS + DIGITS;
    private static final int LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        char[] chars = new char[LENGTH];
        chars[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        for (int i = 2; i < LENGTH; i++) {
            chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        // Fisher-Yates shuffle so the guaranteed letter/digit aren't always the first two chars.
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
