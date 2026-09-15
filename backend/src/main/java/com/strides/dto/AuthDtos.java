package com.strides.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
     * `permissions` is the flattened set of permission codes across the user's roles - the same set
     * AuthenticationFilter checks @RequiresPermission against. Lets the frontend decide whether to
     * fire a permission-gated request at all, without needing role-name knowledge of what each role
     * can do.
     */
    public record UserPublic(
            UUID id,
            String email,
            String fullName,
            String displayName,
            String title,
            String department,
            boolean isActive,
            List<String> roles,
            List<String> permissions) {
    }

    // NIST 800-63B baseline: >=8 characters, no arbitrary complexity rules that just push users
    // toward predictable patterns.
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 255) String newPassword) {
    }
}
