package com.strides.security;

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
 * Mints tokens for the embedded feedback-widget to send to the external ARGUS Lighthouse feedback
 * service, signed with auth.jwt.feedback-secret rather than this app's own auth.jwt.secret - see
 * JwtService's javadoc for the base claim shape (subject + iat/exp), mirrored here so it matches
 * whatever the shared argus convention already expects, plus a `name` claim (see InvestigationAI's
 * own session JWT, which embeds the same) so the feedback service can resolve who actually
 * submitted something beyond the bare subject UUID. Kept as its own key/class (not a second mode
 * on JwtService) so the feedback secret can rotate without touching session auth at all.
 */
@ApplicationScoped
public class FeedbackJwtService {

    private final SecretKey key;
    private final long expireMinutes;

    public FeedbackJwtService(
            @ConfigProperty(name = "auth.jwt.feedback-secret") String secret,
            @ConfigProperty(name = "auth.jwt.access-token-expire-minutes") long expireMinutes) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expireMinutes = expireMinutes;
    }

    public String issue(String subject, String name) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("name", name)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expireMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
