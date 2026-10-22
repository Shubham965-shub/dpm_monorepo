package com.strides.security;

import com.strides.domain.RefreshToken;
import com.strides.domain.User;
import com.strides.repository.RefreshTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Opaque, DB-backed refresh tokens with rotation and reuse detection - unlike a second long-lived
 * JWT, these can actually be revoked. The raw token is only ever handed to the client (as the
 * httpOnly cookie value); the database stores nothing but its SHA-256 hash, so a leaked row can't
 * be replayed.
 */
@ApplicationScoped
public class RefreshTokenService {

    private static final Logger LOG = Logger.getLogger(RefreshTokenService.class);

    // 256-bit random token - well beyond brute-force range, matches the entropy of the tokenHash's
    // SHA-256 storage.
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository tokens;
    private final long expireDays;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens,
            @ConfigProperty(name = "auth.refresh-token-expire-days") long expireDays) {
        this.tokens = tokens;
        this.expireDays = expireDays;
    }

    @Transactional
    public IssuedToken issue(User user) {
        String raw = generateRaw();
        RefreshToken entity = new RefreshToken();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(raw));
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plusDays(expireDays));
        tokens.persist(entity);
        return new IssuedToken(id, raw, user.getId());
    }

    /**
     * Validates and rotates a presented refresh token: the old row is revoked and a new one issued
     * in the same transaction. Returns empty if the token is unknown/expired/revoked or its user is
     * deactivated. If the token was already rotated once before (it has a successor), that's a
     * signal of theft - every other active refresh token for that user is revoked too, forcing a
     * full re-login on all devices.
     */
    @Transactional
    public Optional<IssuedToken> rotate(String rawToken) {
        RefreshToken existing = tokens.findByTokenHash(sha256Hex(rawToken)).orElse(null);
        if (existing == null) {
            return Optional.empty();
        }
        UUID userId = existing.getUser().getId();
        if (existing.isRevoked()) {
            // Revoked by logout (no successor) just fails - a stale tab after logout must not wipe
            // the user's sessions on other devices.
            if (existing.getReplacedById() != null) {
                LOG.warnf("Refresh token reuse detected for user %s - revoking all active sessions", userId);
                revokeAllActiveForUser(userId);
            }
            return Optional.empty();
        }
        if (existing.isExpired()) {
            return Optional.empty();
        }
        if (!existing.getUser().isActive()) {
            existing.setRevokedAt(OffsetDateTime.now());
            return Optional.empty();
        }

        existing.setRevokedAt(OffsetDateTime.now());
        IssuedToken next = issue(existing.getUser());
        existing.setReplacedById(next.id());
        return Optional.of(next);
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(sha256Hex(rawToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.setRevokedAt(OffsetDateTime.now()));
    }

    /**
     * Kills every active session for this user. Called on reuse detection (above), and must also
     * be called anywhere a password is changed/reset or a user is deactivated - otherwise a refresh token issued before the
     * change stays valid for its full remaining 7-day TTL, completely defeating the point of a
     * password change as an incident response ("someone else might have my password").
     */
    @Transactional
    public void revokeAllActiveForUser(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        tokens.findActiveForUser(userId).forEach(t -> t.setRevokedAt(now));
    }

    private String generateRaw() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record IssuedToken(UUID id, String rawToken, UUID userId) {
    }
}
