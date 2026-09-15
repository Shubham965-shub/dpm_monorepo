package com.strides.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * HS256 access tokens. Deliberately minimal claims - just subject + iat/exp, no roles/permissions
 * embedded. Authorization is always re-derived from a live DB lookup (AuthenticationFilter), so a
 * roles claim here would just be an unused staleness surface.
 */
@ApplicationScoped
public class JwtService {

    private final SecretKey key;
    private final long expireMinutes;

    public JwtService(
            @ConfigProperty(name = "auth.jwt.secret") String secret,
            @ConfigProperty(name = "auth.jwt.access-token-expire-minutes") long expireMinutes) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expireMinutes = expireMinutes;
    }

    public String issue(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Throws JwtException (bad signature, expired, malformed) if the token isn't valid right now. */
    public Claims verify(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
