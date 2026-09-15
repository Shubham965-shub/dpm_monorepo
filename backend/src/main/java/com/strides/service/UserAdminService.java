package com.strides.service;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.dto.UsersDtos.CreateUserRequest;
import com.strides.dto.UsersDtos.CreateUserResponse;
import com.strides.dto.UsersDtos.RolePublic;
import com.strides.dto.UsersDtos.UpdateUserRequest;
import com.strides.dto.UsersDtos.UserAdmin;
import com.strides.repository.RoleRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Admin-only user CRUD - every endpoint calling into this is gated by @RequiresPermission("admin:*"). */
@ApplicationScoped
public class UserAdminService {

    private static final String ADMIN_ROLE_NAME = "Admin";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher hasher;
    private final TempPasswordGenerator tempPasswords;
    private final RefreshTokenService refreshTokens;

    public UserAdminService(UserRepository users, RoleRepository roles, PasswordHasher hasher,
            TempPasswordGenerator tempPasswords, RefreshTokenService refreshTokens) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.tempPasswords = tempPasswords;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public List<UserAdmin> listUsers() {
        return users.listAllWithRoles().stream().map(UserAdminService::toUserAdmin).toList();
    }

    public List<RolePublic> listRoles() {
        return roles.listAllOrdered().stream()
                .map(r -> new RolePublic(r.getId(), r.getName(), r.getDescription()))
                .toList();
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
        List<Role> assignedRoles = resolveRoles(req.roleNames());

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

        if (req.roleNames() != null) {
            List<Role> newRoles = resolveRoles(req.roleNames());
            boolean hadAdmin = existing.getRoles().stream().anyMatch(r -> r.getName().equals(ADMIN_ROLE_NAME));
            boolean keepsAdmin = newRoles.stream().anyMatch(r -> r.getName().equals(ADMIN_ROLE_NAME));
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
        // Same reasoning as AuthService.changePassword: this is typically used to respond to a
        // suspected compromise, so any refresh token issued before the reset must not survive it.
        refreshTokens.revokeAllActiveForUser(id);
        return tempPassword;
    }

    static UserAdmin toUserAdmin(User u) {
        return new UserAdmin(u.getId(), u.getEmail(), u.getFullName(), u.getDisplayName(), u.getTitle(),
                u.getDepartment(), u.getEmployeeCode(), u.isActive(), u.getLastLoginAt(),
                u.getRoles().stream().map(Role::getName).sorted().toList());
    }

    private List<Role> resolveRoles(List<String> roleNames) {
        List<String> requested = roleNames == null ? List.of() : roleNames.stream().distinct().toList();
        if (requested.isEmpty()) {
            return List.of();
        }
        List<Role> found = roles.findByNames(requested);
        if (found.size() != requested.size()) {
            Set<String> foundNames = found.stream().map(Role::getName).collect(Collectors.toSet());
            String missing = requested.stream().filter(n -> !foundNames.contains(n)).collect(Collectors.joining(", "));
            throw badRequest("Unknown role(s): " + missing);
        }
        return found;
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
