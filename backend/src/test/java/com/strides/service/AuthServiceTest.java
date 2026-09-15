package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.domain.User;
import com.strides.repository.UserRepository;
import com.strides.security.JwtService;
import com.strides.security.LoginRateLimiter;
import com.strides.security.PasswordHasher;
import com.strides.security.RefreshTokenService;
import com.strides.security.RefreshTokenService.IssuedToken;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Repositories/security collaborators are mocked - no DB, no real bcrypt/JWT work. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository users;
    @Mock
    PasswordHasher hasher;
    @Mock
    JwtService jwt;
    @Mock
    RefreshTokenService refreshTokens;
    @Mock
    LoginRateLimiter rateLimiter;

    @InjectMocks
    AuthService service;

    private static User activeUser(String email, String passwordHash) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setFullName("Test User");
        u.setActive(true);
        return u;
    }

    @Test
    void login_validCredentials_issuesTokensAndRecordsSuccess() {
        User user = activeUser("qc@strides.com", "hash");
        when(users.findByEmailWithRoles("qc@strides.com")).thenReturn(Optional.of(user));
        when(hasher.verify("correct", "hash")).thenReturn(true);
        when(jwt.issue(user.getId().toString())).thenReturn("access-token");
        when(refreshTokens.issue(user)).thenReturn(new IssuedToken(UUID.randomUUID(), "refresh-raw", user.getId()));

        var result = service.login("qc@strides.com", "correct");

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-raw", result.refreshToken());
        assertEquals("qc@strides.com", result.user().email());
        verify(rateLimiter).recordSuccess("qc@strides.com");
        verify(rateLimiter, never()).recordFailure(anyString());
    }

    @Test
    void login_wrongPassword_throwsAndRecordsFailure() {
        User user = activeUser("qc@strides.com", "hash");
        when(users.findByEmailWithRoles("qc@strides.com")).thenReturn(Optional.of(user));
        when(hasher.verify("wrong", "hash")).thenReturn(false);

        assertThrows(NotAuthorizedException.class, () -> service.login("qc@strides.com", "wrong"));
        verify(rateLimiter).recordFailure("qc@strides.com");
        verify(jwt, never()).issue(anyString());
    }

    @Test
    void login_unknownEmail_verifiesAgainstDummyHash_notShortCircuited() {
        when(users.findByEmailWithRoles("ghost@strides.com")).thenReturn(Optional.empty());
        when(hasher.verifyAgainstDummy("whatever")).thenReturn(false);

        assertThrows(NotAuthorizedException.class, () -> service.login("ghost@strides.com", "whatever"));

        // The whole point of DUMMY_HASH is that an unknown email still pays for a bcrypt verify,
        // so response timing can't distinguish "no such account" from "wrong password".
        verify(hasher, times(1)).verifyAgainstDummy("whatever");
        verify(hasher, never()).verify(anyString(), anyString());
        verify(rateLimiter).recordFailure("ghost@strides.com");
    }

    @Test
    void login_inactiveUser_rejectedEvenWithCorrectPassword() {
        User user = activeUser("qc@strides.com", "hash");
        user.setActive(false);
        when(users.findByEmailWithRoles("qc@strides.com")).thenReturn(Optional.of(user));
        when(hasher.verify("correct", "hash")).thenReturn(true);

        assertThrows(NotAuthorizedException.class, () -> service.login("qc@strides.com", "correct"));
        verify(jwt, never()).issue(anyString());
    }

    @Test
    void login_lockedOut_shortCircuitsBeforeTouchingUserRepository() {
        when(rateLimiter.isLocked("qc@strides.com")).thenReturn(true);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.login("qc@strides.com", "anything"));

        assertEquals(429, ex.getResponse().getStatus());
        verify(users, never()).findByEmailWithRoles(anyString());
    }

    @Test
    void refresh_validToken_issuesNewAccessTokenForOwningUser() {
        UUID userId = UUID.randomUUID();
        when(refreshTokens.rotate("old-raw"))
                .thenReturn(Optional.of(new IssuedToken(UUID.randomUUID(), "new-raw", userId)));
        when(jwt.issue(userId.toString())).thenReturn("new-access");

        var result = service.refresh("old-raw");

        assertTrue(result.isPresent());
        assertEquals("new-access", result.get().accessToken());
        assertEquals("new-raw", result.get().refreshToken());
    }

    @Test
    void refresh_invalidToken_returnsEmpty() {
        when(refreshTokens.rotate("bad")).thenReturn(Optional.empty());

        assertTrue(service.refresh("bad").isEmpty());
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsWithoutMutating() {
        User user = activeUser("qc@strides.com", "old-hash");
        when(users.findById(user.getId())).thenReturn(user);
        when(hasher.verify("wrong-current", "old-hash")).thenReturn(false);

        assertThrows(NotAuthorizedException.class,
                () -> service.changePassword(user, "wrong-current", "new-password"));
        assertEquals("old-hash", user.getPasswordHash());
    }

    @Test
    void changePassword_correctCurrent_updatesHash() {
        User user = activeUser("qc@strides.com", "old-hash");
        when(users.findById(user.getId())).thenReturn(user);
        when(hasher.verify("old-password", "old-hash")).thenReturn(true);
        when(hasher.hash("new-password")).thenReturn("new-hash");

        service.changePassword(user, "old-password", "new-password");

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(user.getUpdatedAt());
        // A refresh token issued before the change must not outlive it - otherwise a stolen
        // session survives the exact remediation meant to kill it.
        verify(refreshTokens).revokeAllActiveForUser(user.getId());
    }
}
