package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * JwtService/UserRepository/ResourceInfo/ContainerRequestContext are all mocked - no DB, no real
 * JWT signing. CurrentUser is a real (request-scoped in prod) instance so we can assert what the
 * filter actually stored on it. Fields are package-private CDI @Inject targets, set directly here
 * instead of going through a container.
 */
class AuthenticationFilterTest {

    private JwtService jwtService;
    private UserRepository users;
    private CurrentUser currentUser;
    private ResourceInfo resourceInfo;
    private ContainerRequestContext ctx;
    private UriInfo uriInfo;
    private AuthenticationFilter filter;

    private static class OpenResource {
        void openMethod() {}
    }

    private static class AdminResource {
        @RequiresAdmin
        void adminMethod() {}
    }

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        users = mock(UserRepository.class);
        currentUser = new CurrentUser();
        resourceInfo = mock(ResourceInfo.class);
        ctx = mock(ContainerRequestContext.class);
        uriInfo = mock(UriInfo.class);

        filter = new AuthenticationFilter();
        filter.jwtService = jwtService;
        filter.users = users;
        filter.currentUser = currentUser;
        filter.resourceInfo = resourceInfo;

        when(ctx.getUriInfo()).thenReturn(uriInfo);
    }

    private void givenPath(String path) {
        when(uriInfo.getPath()).thenReturn(path);
    }

    private void givenResourceMethod(Method method, Class<?> declaringClass) {
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        doReturn(declaringClass).when(resourceInfo).getResourceClass();
    }

    private static User activeUser(UUID id, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setActive(true);
        for (Role r : roles) {
            u.getRoles().add(r);
        }
        return u;
    }

    private Response capturedAbortResponse() {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        return captor.getValue();
    }

    @Test
    void publicPath_skipsAuthenticationEntirely() {
        givenPath("api/auth/login");

        filter.filter(ctx);

        verify(ctx, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingBearerHeader_returns401() {
        givenPath("api/qms/oos-trend");
        when(ctx.getHeaderString("Authorization")).thenReturn(null);

        filter.filter(ctx);

        assertEquals(401, capturedAbortResponse().getStatus());
    }

    @Test
    void nonBearerAuthScheme_returns401() {
        givenPath("api/qms/oos-trend");
        when(ctx.getHeaderString("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.filter(ctx);

        assertEquals(401, capturedAbortResponse().getStatus());
    }

    @Test
    void invalidOrExpiredToken_returns401() {
        givenPath("api/qms/oos-trend");
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.verify(anyString())).thenThrow(new JwtException("expired"));

        filter.filter(ctx);

        assertEquals(401, capturedAbortResponse().getStatus());
        verify(users, never()).findByIdWithRoles(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tokenSubjectNotAKnownUser_returns401() {
        UUID id = UUID.randomUUID();
        givenPath("api/qms/oos-trend");
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.verify("good-token")).thenReturn(claims);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.empty());

        filter.filter(ctx);

        assertEquals(401, capturedAbortResponse().getStatus());
        assertNull(currentUser.get());
    }

    @Test
    void inactiveUser_returns401() {
        UUID id = UUID.randomUUID();
        givenPath("api/qms/oos-trend");
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.verify("good-token")).thenReturn(claims);
        User inactive = activeUser(id);
        inactive.setActive(false);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(inactive));

        filter.filter(ctx);

        assertEquals(401, capturedAbortResponse().getStatus());
    }

    @Test
    void validActiveUser_onOpenEndpoint_setsCurrentUserAndAllowsThrough() throws NoSuchMethodException {
        UUID id = UUID.randomUUID();
        givenPath("api/qms/oos-trend");
        givenResourceMethod(OpenResource.class.getDeclaredMethod("openMethod"), OpenResource.class);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.verify("good-token")).thenReturn(claims);
        User user = activeUser(id);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(user));

        filter.filter(ctx);

        verify(ctx, never()).abortWith(org.mockito.ArgumentMatchers.any());
        assertSame(user, currentUser.get());
    }

    @Test
    void validUser_onAdminEndpoint_withoutAdminRole_returns403() throws NoSuchMethodException {
        UUID id = UUID.randomUUID();
        givenPath("api/users");
        givenResourceMethod(AdminResource.class.getDeclaredMethod("adminMethod"), AdminResource.class);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.verify("good-token")).thenReturn(claims);
        User nonAdmin = activeUser(id, Role.ANALYST);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(nonAdmin));

        filter.filter(ctx);

        assertEquals(403, capturedAbortResponse().getStatus());
        // requiresAdmin fails only after CurrentUser is already populated for this request.
        assertSame(nonAdmin, currentUser.get());
    }

    @Test
    void validUser_onAdminEndpoint_withAdminRole_allowsThrough() throws NoSuchMethodException {
        UUID id = UUID.randomUUID();
        givenPath("api/users");
        givenResourceMethod(AdminResource.class.getDeclaredMethod("adminMethod"), AdminResource.class);
        when(ctx.getHeaderString("Authorization")).thenReturn("Bearer good-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(jwtService.verify("good-token")).thenReturn(claims);
        User admin = activeUser(id, Role.ADMIN);
        when(users.findByIdWithRoles(id)).thenReturn(Optional.of(admin));

        filter.filter(ctx);

        verify(ctx, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }
}
