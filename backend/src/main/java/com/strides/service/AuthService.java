package com.strides.service;

import com.strides.domain.Permission;
import com.strides.domain.Role;
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

    @Transactional
    public void changePassword(User currentUser, String currentPassword, String newPassword) {
        // Re-fetch the managed entity: currentUser (from CurrentUser) was loaded in the filter's own
        // transaction and may be detached here.
        User user = users.findById(currentUser.getId());
        if (!hasher.verify(currentPassword, user.getPasswordHash())) {
            throw new NotAuthorizedException("Current password is incorrect", "Bearer");
        }
        user.setPasswordHash(hasher.hash(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        // Otherwise a refresh token issued before this change stays valid for its full remaining
        // TTL, regardless of the password change - defeats "someone else might know my password"
        // as an incident response. This also signs the current session out; that's intentional.
        refreshTokens.revokeAllActiveForUser(user.getId());
    }

    public static UserPublic toPublic(User user) {
        return new UserPublic(user.getId(), user.getEmail(), user.getFullName(), user.getDisplayName(),
                user.getTitle(), user.getDepartment(), user.isActive(), roleNames(user), permissionCodes(user));
    }

    private static List<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).sorted().toList();
    }

    private static List<String> permissionCodes(User user) {
        return user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
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
