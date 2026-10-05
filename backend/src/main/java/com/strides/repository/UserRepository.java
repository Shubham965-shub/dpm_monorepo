package com.strides.repository;

import com.strides.domain.Role;
import com.strides.domain.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    // roles is an eager @ElementCollection (see User.java) - always populated on load, no JOIN
    // FETCH needed. Method names keep the "WithRoles" suffix for call-site clarity even though
    // that's now just "load a user" - roles come along for free either way.

    /** Case-insensitive on both sides: safe as long as no two accounts differ only by email case. */
    public Optional<User> findByEmailWithRoles(String email) {
        return find("lower(email) = lower(?1)", email).firstResultOptional();
    }

    public Optional<User> findByIdWithRoles(UUID id) {
        return findByIdOptional(id);
    }

    public List<User> listAllWithRoles() {
        return find("ORDER BY fullName").list();
    }

    public boolean emailExists(String email) {
        return count("lower(email) = lower(?1)", email) > 0;
    }

    public boolean employeeCodeExists(String employeeCode) {
        return count("employeeCode = ?1", employeeCode) > 0;
    }

    public boolean employeeCodeExistsForOther(String employeeCode, UUID excludeId) {
        return count("employeeCode = ?1 and id <> ?2", employeeCode, excludeId) > 0;
    }

    /** A supervisor's own "Team workload overview" roster (see TeamService) - ANALYST specifically,
     * not every direct report, since a MANAGER/HOD could in principle also carry managerId without
     * meaning "show up on someone's analyst roster". */
    public List<User> findAnalystsManagedBy(UUID managerId) {
        return find("managerId = ?1 and ?2 member of roles ORDER BY fullName", managerId, Role.ANALYST).list();
    }
}
