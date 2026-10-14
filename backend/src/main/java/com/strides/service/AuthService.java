package com.strides.service;

import com.strides.domain.User;
import com.strides.dto.AuthDtos.UserPublic;
import com.strides.repository.UserRepository;
import com.strides.security.JwtService;
import com.strides.security.LoginRateLimiter;
import com.strides.security.PasswordHasher;
import com.strides.security.RefreshTokenService;
import com.strides.security.RefreshTokenService.IssuedToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AuthService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final LoginRateLimiter rateLimiter;

    public AuthService(UserRepository users, PasswordHasher hasher, JwtService jwt,
            RefreshTokenService refreshTokens, LoginRateLimiter rateLimiter) {
        this.users = users;
        this.hasher = hasher;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public LoginResult login(String email, String password) {
        if (rateLimiter.isLocked(email)) {
            throw tooManyRequests("Too many failed attempts. Try again later.");
        }

        User user = users.findByEmailWithRoles(email).orElse(null);
        // Always run a bcrypt verify, even when the account doesn't exist, against a fixed dummy
        // hash - keeps response timing similar either way so it can't be used to enumerate emails.
        boolean passwordOk = user != null
                ? hasher.verify(password, user.getPasswordHash())
                : hasher.verifyAgainstDummy(password);

        if (user == null || !user.isActive() || !passwordOk) {
            rateLimiter.recordFailure(email);
            throw new NotAuthorizedException("Invalid email or password", "Bearer");
        }

        rateLimiter.recordSuccess(email);
        user.setLastLoginAt(OffsetDateTime.now());
        String accessToken = jwt.issue(user.getId().toString());
        IssuedToken refreshToken = refreshTokens.issue(user);
        return new LoginResult(accessToken, refreshToken.rawToken(), toPublic(user));
    }

    /**
     * Self-service password change - the caller is already authenticated (see AuthController), but
     * still must prove they know the CURRENT password, same as any "change password" flow, since a
     * stolen-but-still-valid access token shouldn't be enough on its own to lock the real owner out.
     * Every other refresh token for this user is revoked (same invariant as UserAdminService's
     * admin-triggered reset - see RefreshTokenService.revokeAllActiveForUser's own doc), but this
     * session continues uninterrupted: a fresh access/refresh pair is issued in the same response,
     * exactly like login(), rather than logging the user out immediately after they just typed a new
     * password.
     */
    @Transactional
    public LoginResult changePassword(User caller, String currentPassword, String newPassword) {
        // The caller was loaded by AuthenticationFilter outside this transaction, so mutating it
        // would never be flushed - re-load it into this transaction's session first.
        User user = users.findByIdWithRoles(caller.getId()).orElseThrow(() -> new NotAuthorizedException("Inactive user", "Bearer"));
        if (!hasher.verify(currentPassword, user.getPasswordHash())) {
            throw new NotAuthorizedException("Current password is incorrect", "Bearer");
        }
        user.setPasswordHash(hasher.hash(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        refreshTokens.revokeAllActiveForUser(user.getId());
        String accessToken = jwt.issue(user.getId().toString());
        IssuedToken refreshToken = refreshTokens.issue(user);
        return new LoginResult(accessToken, refreshToken.rawToken(), toPublic(user));
    }

    /** Returns empty if the refresh token is missing/expired/already-used (see RefreshTokenService). */
    public Optional<RefreshResult> refresh(String rawRefreshToken) {
        return refreshTokens.rotate(rawRefreshToken)
                .map(next -> new RefreshResult(jwt.issue(next.userId().toString()), next.rawToken()));
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokens.revoke(rawRefreshToken);
        }
    }

    public static UserPublic toPublic(User user) {
        return new UserPublic(user.getId(), user.getEmail(), user.getFullName(), user.getDisplayName(),
                user.getTitle(), departmentNames(user), user.isActive(), roleNames(user));
    }

    private static List<String> roleNames(User user) {
        return user.getRoles().stream().map(Enum::name).sorted().toList();
    }

    private static List<String> departmentNames(User user) {
        return user.getDepartments().stream().map(Enum::name).sorted().toList();
    }

    private static WebApplicationException tooManyRequests(String message) {
        return new WebApplicationException(Response.status(429)
                .entity(Map.of("detail", message)).type(MediaType.APPLICATION_JSON).build());
    }

    public record LoginResult(String accessToken, String refreshToken, UserPublic user) {
    }

    public record RefreshResult(String accessToken, String refreshToken) {
    }
}
