package com.strides.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Admin-only user-management payloads (see UsersController, gated by @RequiresAdmin). */
public final class UsersDtos {

    private UsersDtos() {
    }

    public record UserAdmin(
            UUID id,
            String email,
            String fullName,
            String displayName,
            String title,
            List<String> departments,
            String employeeCode,
            boolean isActive,
            OffsetDateTime lastLoginAt,
            List<String> roles,
            UUID managerId) {
    }

    /** No password field - the server generates a one-time temp password and returns it once. */
    public record CreateUserRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 160) String fullName,
            @Size(max = 80) String displayName,
            @Size(max = 120) String title,
            List<String> departmentNames,
            @Size(max = 40) String employeeCode,
            List<String> roleNames,
            UUID managerId) {
    }

    /**
     * Partial update: null = leave unchanged, "" on optional text fields = clear to NULL,
     * roleNames/departmentNames null = unchanged / [] = remove all. Email is immutable (no field) -
     * it's the account's identity, not just a profile attribute. Activate/deactivate is just
     * isActive here rather than dedicated endpoints, to avoid endpoint sprawl for a single boolean
     * flip. managerId follows the same "null = leave unchanged" rule as the other optional fields -
     * there's no sentinel for "clear the manager" yet, since nothing has needed that so far.
     */
    public record UpdateUserRequest(
            @Size(max = 160) String fullName,
            @Size(max = 80) String displayName,
            @Size(max = 120) String title,
            List<String> departmentNames,
            @Size(max = 40) String employeeCode,
            Boolean isActive,
            List<String> roleNames,
            UUID managerId) {
    }

    public record CreateUserResponse(UserAdmin user, String tempPassword) {
    }

    public record TempPasswordResponse(String tempPassword) {
    }
}
