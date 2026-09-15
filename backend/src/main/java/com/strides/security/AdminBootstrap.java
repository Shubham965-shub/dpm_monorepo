package com.strides.security;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.repository.UserRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Creates exactly one ADMIN-role user on first startup, so there's a way into an otherwise-empty
 * users table without hand-writing SQL. Gated by ADMIN_BOOTSTRAP_EMAIL/PASSWORD - unset (the
 * normal steady-state once any account exists) means this is a no-op.
 */
@ApplicationScoped
public class AdminBootstrap {

    private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

    @Inject
    UserRepository users;
    @Inject
    PasswordHasher hasher;

    @ConfigProperty(name = "admin.bootstrap.email")
    Optional<String> bootstrapEmail;
    @ConfigProperty(name = "admin.bootstrap.password")
    Optional<String> bootstrapPassword;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        String email = bootstrapEmail.map(String::trim).filter(s -> !s.isBlank()).orElse(null);
        String password = bootstrapPassword.filter(s -> !s.isBlank()).orElse(null);
        if (email == null || password == null) {
            return;
        }
        if (users.count() > 0) {
            LOG.info("Admin bootstrap skipped - the users table already has data");
            return;
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(hasher.hash(password));
        user.setFullName("Administrator");
        user.setActive(true);
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(Role.ADMIN);
        users.persist(user);
        LOG.infof("Bootstrap admin account created: %s", user.getEmail());
    }
}
