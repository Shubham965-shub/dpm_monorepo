package com.strides.repository;

import com.strides.domain.Role;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RoleRepository implements PanacheRepositoryBase<Role, UUID> {

    public Optional<Role> findByName(String name) {
        return find("name", name).firstResultOptional();
    }

    public List<Role> findByNames(List<String> names) {
        return find("name in ?1", names).list();
    }

    public List<Role> listAllOrdered() {
        return find("ORDER BY name").list();
    }
}
