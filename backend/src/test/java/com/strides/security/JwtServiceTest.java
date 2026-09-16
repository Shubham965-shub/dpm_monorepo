package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Constructed directly with a plain secret/expiry, mirroring how @ConfigProperty would inject
 * them - no CDI container, no DB.
 */
class JwtServiceTest {

    // >= 32 bytes, as HS256 requires - see JwtService's javadoc.
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789";

    @Test
    void issueThenVerify_roundTripsTheSubject() {
        JwtService service = new JwtService(SECRET, 15);
        String subject = UUID.randomUUID().toString();

        String token = service.issue(subject);
        Claims claims = service.verify(token);

        assertEquals(subject, claims.getSubject());
    }

    @Test
    void verify_expiredToken_throwsJwtException() {
        JwtService service = new JwtService(SECRET, -1); // already-expired by construction
        String token = service.issue(UUID.randomUUID().toString());

        assertThrows(JwtException.class, () -> service.verify(token));
    }

    @Test
    void verify_tamperedSignature_throwsJwtException() {
        JwtService service = new JwtService(SECRET, 15);
        String token = service.issue(UUID.randomUUID().toString());
        // Flips a character well inside the signature, not its last one - base64url has no padding,
        // so the final character only carries a few significant bits and can decode to the same byte
        // even when its text representation changes, making that specific position flaky to tamper.
        int flipIndex = token.length() - 6;
        char original = token.charAt(flipIndex);
        char replacement = original == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, flipIndex) + replacement + token.substring(flipIndex + 1);

        assertThrows(JwtException.class, () -> service.verify(tampered));
    }

    @Test
    void verify_tokenSignedWithADifferentSecret_throwsJwtException() {
        JwtService issuer = new JwtService(SECRET, 15);
        JwtService verifier = new JwtService("different-secret-that-is-also-32-bytes-plus", 15);
        String token = issuer.issue(UUID.randomUUID().toString());

        assertThrows(JwtException.class, () -> verifier.verify(token));
    }

    @Test
    void issue_differentSubjects_produceDifferentTokens() {
        JwtService service = new JwtService(SECRET, 15);

        String first = service.issue(UUID.randomUUID().toString());
        String second = service.issue(UUID.randomUUID().toString());

        assertNotEquals(first, second);
    }
}
