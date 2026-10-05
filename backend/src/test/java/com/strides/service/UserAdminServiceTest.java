package com.strides.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.dto.UsersDtos.CreateUserRequest;
import com.strides.dto.UsersDtos.UpdateUserRequest;
import com.strides.repository.UserRepository;
import com.strides.security.PasswordHasher;
import com.strides.security.RefreshTokenService;
import com.strides.security.TempPasswordGenerator;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Repositories/security collaborators are mocked - no DB, no real bcrypt work. */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    UserRepository users;
    @Mock
    PasswordHasher hasher;
    @Mock
    TempPasswordGenerator tempPasswords;
    @Mock
    RefreshTokenService refreshTokens;

    @InjectMocks
    UserAdminService service;

    private static User withRoles(UUID id, Role... assignedRoles) {
        User u = new User();
        u.setId(id);
        u.setFullName("Existing User");
        u.setActive(true);
        u.getRoles().addAll(List.of(assignedRoles));
        return u;
    }

    @Test
    void createUser_emailAlreadyInUse_throwsConflict() {
        when(users.emailExists("taken@strides.com")).thenReturn(true);
        var req = new CreateUserRequest("taken@strides.com", "New User", null, null, null, null, null, null);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.createUser(req));

        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    void createUser_generatesAndHashesATempPassword_neverStoresItPlain() {
        when(users.emailExists(anyString())).thenReturn(false);
        when(tempPasswords.generate()).thenReturn("Temp9Pass2Word");
        when(hasher.hash("Temp9Pass2Word")).thenReturn("bcrypt-hash");
        var req = new CreateUserRequest("new@strides.com", "New User", null, null, null, null, null, null);

        var response = service.createUser(req);

        assertEquals("Temp9Pass2Word", response.tempPassword()); // returned once to the admin
        assertEquals("new@strides.com", response.user().email());
    }

    @Test
    void createUser_managerDoesNotExist_isRejected() {
        when(users.emailExists(anyString())).thenReturn(false);
        UUID managerId = UUID.randomUUID();
        when(users.findByIdWithRoles(managerId)).thenReturn(Optional.empty());
        var req = new CreateUserRequest("new@strides.com", "New User", null, null, null, null, null, managerId);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.createUser(req));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void createUser_withAnExistingManager_setsManagerId() {
        when(users.emailExists(anyString())).thenReturn(false);
        when(tempPasswords.generate()).thenReturn("Temp9Pass2Word");
        when(hasher.hash(anyString())).thenReturn("bcrypt-hash");
        UUID managerId = UUID.randomUUID();
        when(users.findByIdWithRoles(managerId)).thenReturn(Optional.of(withRoles(managerId, Role.SUPERVISOR)));
        var req = new CreateUserRequest("new@strides.com", "New User", null, null, null, null, null, managerId);

        var response = service.createUser(req);

        assertEquals(managerId, response.user().managerId());
    }

    @Test
    void updateUser_selfDeactivate_isRejected() {
        UUID id = UUID.randomUUID();
        User self = withRoles(id);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(self));
        var req = new UpdateUserRequest(null, null, null, null, null, false, null, null);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.updateUser(id, req, self));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void updateUser_selfRemovesOwnAdminRole_isRejected() {
        UUID id = UUID.randomUUID();
        User self = withRoles(id, Role.ADMIN);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(self));
        var req = new UpdateUserRequest(null, null, null, null, null, null, List.of(), null);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.updateUser(id, req, self));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void updateUser_deactivatingAnotherUser_isAllowed() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User actor = withRoles(actorId, Role.ADMIN);
        User target = withRoles(targetId);
        when(users.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        var req = new UpdateUserRequest(null, null, null, null, null, false, null, null);

        var updated = service.updateUser(targetId, req, actor);

        assertEquals(false, updated.isActive());
        verify(refreshTokens).revokeAllActiveForUser(targetId);
    }

    @Test
    void updateUser_setsOwnSelfAsManager_isRejected() {
        UUID id = UUID.randomUUID();
        User self = withRoles(id, Role.ADMIN);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(self));
        var req = new UpdateUserRequest(null, null, null, null, null, null, null, id);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> service.updateUser(id, req, self));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void updateUser_setsAnExistingManager_updatesManagerId() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        User actor = withRoles(actorId, Role.ADMIN);
        User target = withRoles(targetId, Role.ANALYST);
        when(users.findByIdWithRoles(targetId)).thenReturn(Optional.of(target));
        when(users.findByIdWithRoles(managerId)).thenReturn(Optional.of(withRoles(managerId, Role.SUPERVISOR)));
        var req = new UpdateUserRequest(null, null, null, null, null, null, null, managerId);

        var updated = service.updateUser(targetId, req, actor);

        assertEquals(managerId, updated.managerId());
    }

    @Test
    void resetPassword_revokesAllRefreshTokensForThatUser() {
        UUID id = UUID.randomUUID();
        User target = withRoles(id);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(target));
        when(tempPasswords.generate()).thenReturn("Temp9Pass2Word");
        when(hasher.hash("Temp9Pass2Word")).thenReturn("bcrypt-hash");

        String tempPassword = service.resetPassword(id);

        assertNotNull(tempPassword);
        // A refresh token issued before this reset must not outlive it - otherwise a stolen
        // session survives the exact remediation meant to kill it.
        verify(refreshTokens).revokeAllActiveForUser(id);
    }

    @Test
    void createUser_unknownRoleName_isRejected() {
        when(users.emailExists(anyString())).thenReturn(false);
        var req = new CreateUserRequest("new@strides.com", "New User", null, null, null,
                null, List.of("SUPERVISOR", "NOT_A_REAL_ROLE"), null);

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.createUser(req));

        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    void createUser_roleNameMatchingIsCaseInsensitive() {
        when(users.emailExists(anyString())).thenReturn(false);
        when(tempPasswords.generate()).thenReturn("Temp9Pass2Word");
        when(hasher.hash(anyString())).thenReturn("bcrypt-hash");
        var req = new CreateUserRequest("new@strides.com", "New User", null, null, null,
                null, List.of("supervisor", "Analyst"), null);

        var response = service.createUser(req);

        assertEquals(List.of("ANALYST", "SUPERVISOR"), response.user().roles());
    }
}
