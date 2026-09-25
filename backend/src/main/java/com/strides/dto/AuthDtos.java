package com.strides.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

/** Auth request/response payloads. Field names are camelCase - Jackson serializes them as-is. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    /**
     * refreshToken travels in the response body, not a cookie - this app runs inside a VNet over
     * plain http, where a cookie's Secure attribute (required for it to ever be sent) can't be
     * satisfied. The client is responsible for storing it (localStorage) and sending it back
     * explicitly on /refresh and /logout via RefreshTokenRequest.
     */
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, UserPublic user) {
        public TokenResponse(String accessToken, String refreshToken, UserPublic user) {
            this(accessToken, refreshToken, "bearer", user);
        }
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record RefreshResponse(String accessToken, String refreshToken) {
    }

    /** See FeedbackJwtService - a separate token from accessToken, signed for the external feedback service. */
    public record FeedbackTokenResponse(String token) {
    }

    /**
     * `roles` is the fixed Role enum's names (e.g. "ADMIN", "MANAGER") - the frontend can check
     * `roles.includes('ADMIN')` to decide whether to show the admin UI, since AuthenticationFilter
     * checks the exact same thing server-side for every admin-only endpoint.
     */
    public record UserPublic(
            UUID id,
            String email,
            String fullName,
            String displayName,
            String title,
            String department,
            boolean isActive,
            List<String> roles) {
    }
}
