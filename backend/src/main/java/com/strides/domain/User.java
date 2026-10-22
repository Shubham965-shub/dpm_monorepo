package com.strides.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "display_name")
    private String displayName;

    private String title;

    // Fixed, code-level Department enum, not a joined entity - see Department.java. A user can hold
    // more than one (e.g. the single CXO shared across Manufacturing, Engineering and Supply Chain
    // & Planning). Governs dashboard routing (see frontend roleAccess.ts), ADMIN bypasses it. Eager
    // for the same reason as roles below - a handful of enum values, not worth a second query.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_departments", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "department", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Department> departments = EnumSet.noneOf(Department.class);

    @Column(name = "employee_code")
    private String employeeCode;

    // Plain UUID, not a @ManyToOne self-reference - this entity only ever needs the id (to filter
    // "my direct reports" by it, see UserRepository.findAnalystsManagedBy), never the manager's own
    // full profile loaded alongside it.
    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Fixed, code-level Role enum, not a joined entity - see Role.java. A user can hold more than
    // one (e.g. both MANAGER and ANALYST). ADMIN is what gates UsersController (@RequiresAdmin,
    // checked as roles.contains(ADMIN)) - the other five currently just categorize users. Eager:
    // it's a handful of enum values, not worth a second query or lazy-proxy complexity.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Set<Department> getDepartments() {
        return departments;
    }
}
