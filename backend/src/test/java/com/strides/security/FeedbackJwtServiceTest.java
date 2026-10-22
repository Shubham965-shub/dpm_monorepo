package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Constructed directly with a plain secret/expiry, mirroring how @ConfigProperty would inject
 * them - no CDI container, no DB (same style as JwtServiceTest). FeedbackJwtService has no
 * verify() of its own (verification happens on the feedback service's side, not here), so the
 * issued token is decoded directly via Jwts.parser() with the same secret.
 */
class FeedbackJwtServiceTest {

    // >= 32 bytes, as HS256 requires - see FeedbackJwtService's javadoc.
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789";

    private static Claims decode(String token) {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        return jws.getPayload();
    }

    @Test
    void issue_includesTheSubjectAndNameClaims() {
        FeedbackJwtService service = new FeedbackJwtService(SECRET, 15);
        String subject = UUID.randomUUID().toString();

        String token = service.issue(subject, "Ravi Shankar");
        Claims claims = decode(token);

        assertEquals(subject, claims.getSubject());
        assertEquals("Ravi Shankar", claims.get("name", String.class));
    }

    @Test
    void issue_differentNames_produceDifferentTokens() {
        FeedbackJwtService service = new FeedbackJwtService(SECRET, 15);
        String subject = UUID.randomUUID().toString();

        String first = service.issue(subject, "Ravi Shankar");
        String second = service.issue(subject, "Arjun Mehta");

        assertEquals("Ravi Shankar", decode(first).get("name", String.class));
        assertEquals("Arjun Mehta", decode(second).get("name", String.class));
    }
}
