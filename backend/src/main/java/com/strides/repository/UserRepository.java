package com.strides.repository;

import com.strides.domain.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    private static final String FETCH_ROLES =
            "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions ";

    /** Case-insensitive on both sides: safe as long as no two accounts differ only by email case. */
    public Optional<User> findByEmailWithRoles(String email) {
        return find(FETCH_ROLES + "WHERE lower(u.email) = lower(?1)", email).list().stream().findFirst();
    }

    public Optional<User> findByIdWithRoles(UUID id) {
        return find(FETCH_ROLES + "WHERE u.id = ?1", id).list().stream().findFirst();
    }

    private static final String FETCH_ROLES_ONLY = "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles ";

    public List<User> listAllWithRoles() {
        return find(FETCH_ROLES_ONLY + "ORDER BY u.fullName").list();
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
}
