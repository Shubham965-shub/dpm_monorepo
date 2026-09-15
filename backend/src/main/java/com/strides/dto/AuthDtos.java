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

    public record TokenResponse(String accessToken, String tokenType, UserPublic user) {
        public TokenResponse(String accessToken, UserPublic user) {
            this(accessToken, "bearer", user);
        }
    }

    public record RefreshResponse(String accessToken) {
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
