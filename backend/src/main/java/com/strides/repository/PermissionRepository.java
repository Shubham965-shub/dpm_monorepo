package com.strides.repository;

import com.strides.domain.Permission;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PermissionRepository implements PanacheRepositoryBase<Permission, UUID> {

    public Optional<Permission> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
