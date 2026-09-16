package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UserRepository/PasswordHasher are mocked - no DB. Fields are package-private (CDI @Inject
 * targets), so this test sets them directly instead of going through a container; onStart() is
 * called directly rather than firing a real StartupEvent.
 */
class AdminBootstrapTest {

    private UserRepository users;
    private PasswordHasher hasher;
    private AdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        hasher = mock(PasswordHasher.class);
        bootstrap = new AdminBootstrap();
        bootstrap.users = users;
        bootstrap.hasher = hasher;
        bootstrap.bootstrapEmail = Optional.empty();
        bootstrap.bootstrapPassword = Optional.empty();
    }

    @Test
    void onStart_noBootstrapEmailOrPassword_doesNothing() {
        bootstrap.onStart(null);

        verify(users, never()).persist(any(User.class));
    }

    @Test
    void onStart_onlyEmailSet_doesNothing() {
        bootstrap.bootstrapEmail = Optional.of("admin@strides.com");

        bootstrap.onStart(null);

        verify(users, never()).persist(any(User.class));
    }

    @Test
    void onStart_usersTableAlreadyHasData_skipsBootstrap() {
        bootstrap.bootstrapEmail = Optional.of("admin@strides.com");
        bootstrap.bootstrapPassword = Optional.of("SomeTempPass123");
        when(users.count()).thenReturn(1L);

        bootstrap.onStart(null);

        verify(users, never()).persist(any(User.class));
    }

    @Test
    void onStart_emptyTableWithBothVarsSet_createsHashedAdminAccount() {
        bootstrap.bootstrapEmail = Optional.of("  Admin@Strides.com  ");
        bootstrap.bootstrapPassword = Optional.of("SomeTempPass123");
        when(users.count()).thenReturn(0L);
        when(hasher.hash("SomeTempPass123")).thenReturn("bcrypt-hash");

        bootstrap.onStart(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).persist(captor.capture());
        User created = captor.getValue();
        assertEquals("admin@strides.com", created.getEmail()); // trimmed and lowercased
        assertEquals("bcrypt-hash", created.getPasswordHash()); // never the plain password
        assertTrue(created.getRoles().contains(Role.ADMIN));
        assertTrue(created.isActive());
    }

    @Test
    void onStart_blankBootstrapPassword_isTreatedAsUnset() {
        bootstrap.bootstrapEmail = Optional.of("admin@strides.com");
        bootstrap.bootstrapPassword = Optional.of("   ");

        bootstrap.onStart(null);

        verify(users, never()).persist(any(User.class));
    }
}
