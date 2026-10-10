package com.strides.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Auth request/response payloads. Field names are camelCase - Jackson serializes them as-is. */
public final class AuthDtos {

    /** Length-only policy (business decision, 2026-10-09) - no character-class requirements. */
    public static final int MIN_NEW_PASSWORD_LENGTH = 8;

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    /** Self-service password change (see AuthService.changePassword) - distinct from
     * UsersController's admin-only reset, which is for a lost/compromised account the user can't
     * log into themselves. */
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = MIN_NEW_PASSWORD_LENGTH) String newPassword) {
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
     * checks the exact same thing server-side for every admin-only endpoint. `departments` is the
     * fixed Department enum's names (e.g. "QC", "QA") - a user can hold more than one (see
     * Department.java) - the frontend gates which dashboard(s) it shows the same way, mirroring
     * RequiresDepartment's server-side check.
     */
    public record UserPublic(
            UUID id,
            String email,
            String fullName,
            String displayName,
            String title,
            List<String> departments,
            boolean isActive,
            List<String> roles) {
    }

    /** One row of a supervisor's own "Team workload overview" roster (see TeamService) - just
     * enough to identify and label a card; no email/employeeCode, unlike UsersDtos.UserAdmin, since
     * this is served to the supervisor themselves, not an admin. */
    public record TeamMember(UUID id, String fullName, String title) {
    }
}
