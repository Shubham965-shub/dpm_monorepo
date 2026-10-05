package com.strides.service;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.dto.UsersDtos.CreateUserRequest;
import com.strides.dto.UsersDtos.CreateUserResponse;
import com.strides.dto.UsersDtos.UpdateUserRequest;
import com.strides.dto.UsersDtos.UserAdmin;
import com.strides.repository.UserRepository;
import com.strides.security.PasswordHasher;
import com.strides.security.RefreshTokenService;
import com.strides.security.TempPasswordGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Admin-only user CRUD - every endpoint calling into this is gated by @RequiresAdmin. */
@ApplicationScoped
public class UserAdminService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TempPasswordGenerator tempPasswords;
    private final RefreshTokenService refreshTokens;

    public UserAdminService(UserRepository users, PasswordHasher hasher,
            TempPasswordGenerator tempPasswords, RefreshTokenService refreshTokens) {
        this.users = users;
        this.hasher = hasher;
        this.tempPasswords = tempPasswords;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public List<UserAdmin> listUsers() {
        return users.listAllWithRoles().stream().map(UserAdminService::toUserAdmin).toList();
    }

    /** The fixed set of assignable role names - not a DB query, just the Role enum's values. */
    public List<String> listRoles() {
        return Arrays.stream(Role.values()).map(Enum::name).toList();
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        String employeeCode = blankToNull(req.employeeCode());
        if (users.emailExists(email)) {
            throw conflict("Email already in use");
        }
        if (employeeCode != null && users.employeeCodeExists(employeeCode)) {
            throw conflict("Employee code already in use");
        }
        Set<Role> assignedRoles = resolveRoles(req.roleNames());
        if (req.managerId() != null && users.findByIdWithRoles(req.managerId()).isEmpty()) {
            throw badRequest("Manager not found");
        }

        String tempPassword = tempPasswords.generate();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(hasher.hash(tempPassword));
        user.setFullName(req.fullName().trim());
        user.setDisplayName(blankToNull(req.displayName()));
        user.setTitle(blankToNull(req.title()));
        user.setDepartment(blankToNull(req.department()));
        user.setEmployeeCode(employeeCode);
        user.setManagerId(req.managerId());
        user.setActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().addAll(assignedRoles);
        users.persist(user);

        return new CreateUserResponse(toUserAdmin(user), tempPassword);
    }

    @Transactional
    public UserAdmin updateUser(UUID id, UpdateUserRequest req, User actor) {
        User existing = users.findByIdWithRoles(id).orElseThrow(() -> new NotFoundException("User not found"));
        boolean self = existing.getId().equals(actor.getId());

        String fullName = req.fullName() != null ? req.fullName().trim() : existing.getFullName();
        if (fullName.isBlank()) {
            throw badRequest("Full name must not be blank");
        }
        boolean isActive = req.isActive() != null ? req.isActive() : existing.isActive();
        if (self && !isActive) {
            throw badRequest("You cannot deactivate your own account");
        }

        String employeeCode = req.employeeCode() != null ? blankToNull(req.employeeCode()) : existing.getEmployeeCode();
        if (employeeCode != null && users.employeeCodeExistsForOther(employeeCode, id)) {
            throw conflict("Employee code already in use");
        }
        if (req.managerId() != null) {
            if (req.managerId().equals(id)) {
                throw badRequest("A user cannot be their own manager");
            }
            if (users.findByIdWithRoles(req.managerId()).isEmpty()) {
                throw badRequest("Manager not found");
            }
        }

        if (req.roleNames() != null) {
            Set<Role> newRoles = resolveRoles(req.roleNames());
            boolean hadAdmin = existing.getRoles().contains(Role.ADMIN);
            boolean keepsAdmin = newRoles.contains(Role.ADMIN);
            if (self && hadAdmin && !keepsAdmin) {
                throw badRequest("You cannot remove your own Admin role");
            }
            existing.getRoles().clear();
            existing.getRoles().addAll(newRoles);
        }

        existing.setFullName(fullName);
        if (req.displayName() != null) {
            existing.setDisplayName(blankToNull(req.displayName()));
        }
        if (req.title() != null) {
            existing.setTitle(blankToNull(req.title()));
        }
        if (req.department() != null) {
            existing.setDepartment(blankToNull(req.department()));
        }
        existing.setEmployeeCode(employeeCode);
        if (req.managerId() != null) {
            existing.setManagerId(req.managerId());
        }
        if (existing.isActive() && !isActive) {
            refreshTokens.revokeAllActiveForUser(id);
        }
        existing.setActive(isActive);
        existing.setUpdatedAt(OffsetDateTime.now());

        return toUserAdmin(existing);
    }

    @Transactional
    public String resetPassword(UUID id) {
        User user = users.findByIdWithRoles(id).orElseThrow(() -> new NotFoundException("User not found"));
        String tempPassword = tempPasswords.generate();
        user.setPasswordHash(hasher.hash(tempPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        // This is typically used to respond to a suspected compromise (only admins can reset a
        // password - there's no self-service change), so any refresh token issued before the
        // reset must not survive it.
        refreshTokens.revokeAllActiveForUser(id);
        return tempPassword;
    }

    static UserAdmin toUserAdmin(User u) {
        return new UserAdmin(u.getId(), u.getEmail(), u.getFullName(), u.getDisplayName(), u.getTitle(),
                u.getDepartment(), u.getEmployeeCode(), u.isActive(), u.getLastLoginAt(),
                u.getRoles().stream().map(Enum::name).sorted().toList(), u.getManagerId());
    }

    /** Case-insensitive match against the fixed Role enum - not a DB lookup, so no ordering/dedup query needed. */
    private static Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        Set<Role> resolved = EnumSet.noneOf(Role.class);
        List<String> unknown = new ArrayList<>();
        for (String name : roleNames) {
            try {
                resolved.add(Role.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            }
            catch (IllegalArgumentException e) {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            throw badRequest("Unknown role(s): " + String.join(", ", unknown));
        }
        return resolved;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static WebApplicationException conflict(String message) {
        return error(Response.Status.CONFLICT, message);
    }

    private static WebApplicationException badRequest(String message) {
        return error(Response.Status.BAD_REQUEST, message);
    }

    private static WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(Response.status(status)
                .entity(Map.of("detail", message)).type(MediaType.APPLICATION_JSON).build());
    }
}
