package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.domain.RefreshToken;
import com.strides.domain.User;
import com.strides.repository.RefreshTokenRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * expireDays is a plain constructor long, not a mockable collaborator - constructed manually
 * (rather than via @InjectMocks) so the test controls it explicitly instead of relying on
 * Mockito's default-0L injection, which would make every issued token expire immediately.
 */
class RefreshTokenServiceTest {

    private static final long EXPIRE_DAYS = 7;

    private RefreshTokenRepository tokens;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        tokens = mock(RefreshTokenRepository.class);
        service = new RefreshTokenService(tokens, EXPIRE_DAYS);
    }

    private static User user(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private static RefreshToken existingToken(User owner, OffsetDateTime expiresAt, OffsetDateTime revokedAt) {
        RefreshToken t = new RefreshToken();
        t.setId(UUID.randomUUID());
        t.setUser(owner);
        t.setExpiresAt(expiresAt);
        t.setRevokedAt(revokedAt);
        return t;
    }

    @Test
    void issue_storesOnlyTheHash_neverTheRawToken() {
        User owner = user(UUID.randomUUID());

        var issued = service.issue(owner);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokens).persist(captor.capture());
        RefreshToken persisted = captor.getValue();

        assertNotEquals(issued.rawToken(), persisted.getTokenHash());
        assertEquals(64, persisted.getTokenHash().length()); // SHA-256 hex digest length
        assertEquals(owner, persisted.getUser());
        assertTrue(persisted.getExpiresAt().isAfter(OffsetDateTime.now().plusDays(EXPIRE_DAYS - 1)));
    }

    @Test
    void rotate_unknownToken_returnsEmpty() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertTrue(service.rotate("no-such-token").isEmpty());
        verify(tokens, never()).persist(any(RefreshToken.class));
    }

    @Test
    void rotate_validToken_revokesOldAndLinksReplacement() {
        User owner = user(UUID.randomUUID());
        RefreshToken existing = existingToken(owner, OffsetDateTime.now().plusDays(1), null);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        var result = service.rotate("old-raw-token");

        assertTrue(result.isPresent());
        assertEquals(owner.getId(), result.get().userId());
        assertNotEquals("old-raw-token", result.get().rawToken());
        assertTrue(existing.isRevoked());
        assertEquals(result.get().id(), existing.getReplacedById());
        verify(tokens, times(1)).persist(any(RefreshToken.class)); // only the new row - existing is a managed-entity mutation
    }

    @Test
    void rotate_expiredToken_returnsEmptyWithoutRevoking() {
        User owner = user(UUID.randomUUID());
        RefreshToken existing = existingToken(owner, OffsetDateTime.now().minusMinutes(1), null);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertTrue(service.rotate("expired-token").isEmpty());
        assertNull(existing.getRevokedAt());
    }

    @Test
    void rotate_alreadyRotatedToken_isReuseSignal_revokesAllActiveSessionsForUser() {
        User owner = user(UUID.randomUUID());
        RefreshToken existing = existingToken(owner, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusMinutes(5));
        existing.setReplacedById(UUID.randomUUID());
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        RefreshToken otherActiveSession = existingToken(owner, OffsetDateTime.now().plusDays(2), null);
        when(tokens.findActiveForUser(owner.getId())).thenReturn(List.of(otherActiveSession));

        assertTrue(service.rotate("stolen-and-replayed-token").isEmpty());

        assertTrue(otherActiveSession.isRevoked());
        verify(tokens, never()).persist(any(RefreshToken.class)); // no new token issued on a reuse/theft signal
    }

    @Test
    void rotate_tokenRevokedByLogout_failsWithoutRevokingOtherSessions() {
        User owner = user(UUID.randomUUID());
        RefreshToken loggedOut = existingToken(owner, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusMinutes(5));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(loggedOut));

        assertTrue(service.rotate("logged-out-token").isEmpty());

        verify(tokens, never()).findActiveForUser(any());
        verify(tokens, never()).persist(any(RefreshToken.class));
    }

    @Test
    void rotate_deactivatedUser_returnsEmptyAndRevokesToken() {
        User owner = user(UUID.randomUUID());
        owner.setActive(false);
        RefreshToken existing = existingToken(owner, OffsetDateTime.now().plusDays(1), null);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertTrue(service.rotate("deactivated-users-token").isEmpty());

        assertTrue(existing.isRevoked());
        verify(tokens, never()).persist(any(RefreshToken.class));
    }

    @Test
    void revoke_marksTokenRevoked() {
        RefreshToken existing = existingToken(user(UUID.randomUUID()), OffsetDateTime.now().plusDays(1), null);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        service.revoke("some-raw-token");

        assertTrue(existing.isRevoked());
    }
}
