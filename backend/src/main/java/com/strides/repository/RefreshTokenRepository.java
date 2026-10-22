package com.strides.repository;

import com.strides.domain.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, UUID> {

    /** Row-locked so two concurrent refreshes of the same token serialize instead of both rotating it. */
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    public List<RefreshToken> findActiveForUser(UUID userId) {
        return find("user.id = ?1 and revokedAt is null and expiresAt > ?2", userId, OffsetDateTime.now()).list();
    }
}
