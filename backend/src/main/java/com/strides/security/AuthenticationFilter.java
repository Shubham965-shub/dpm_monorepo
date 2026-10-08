package com.strides.security;

import com.strides.domain.Department;
import com.strides.domain.Role;
import com.strides.domain.User;
import com.strides.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticates the bearer access token and enforces @RequiresAdmin on every request except an
 * explicit allowlist. Default-deny: any endpoint not in PUBLIC_PATHS requires a valid, unexpired
 * token for an active user - this is what actually makes auth "required" across the app, not just
 * available infrastructure.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "api/auth/login", "api/auth/refresh", "api/auth/logout");

    @Inject
    JwtService jwtService;

    @Inject
    UserRepository users;

    @Inject
    CurrentUser currentUser;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (PUBLIC_PATHS.contains(path)) {
            return;
        }

        String header = ctx.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            abort(ctx, Response.Status.UNAUTHORIZED, "Missing bearer token");
            return;
        }

        // Only a token problem (bad signature/expired/malformed) is a 401. The DB lookup stays
        // outside this catch: a transient DB error must surface as 500, not 401 - otherwise a
        // stale-connection blip would look like an expired session and log the user out.
        UUID userId;
        try {
            Claims claims = jwtService.verify(header.substring(7));
            userId = UUID.fromString(claims.getSubject());
        }
        catch (JwtException | IllegalArgumentException ex) {
            abort(ctx, Response.Status.UNAUTHORIZED, "Invalid bearer token");
            return;
        }

        User user = users.findByIdWithRoles(userId).orElse(null);
        if (user == null || !user.isActive()) {
            abort(ctx, Response.Status.UNAUTHORIZED, "Inactive user");
            return;
        }
        currentUser.set(user);

        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        if (requiresAdmin() && !isAdmin) {
            abort(ctx, Response.Status.FORBIDDEN, "Admin role required");
            return;
        }

        Department requiredDepartment = requiredDepartment();
        if (requiredDepartment != null && !isAdmin && !user.getDepartments().contains(requiredDepartment)) {
            abort(ctx, Response.Status.FORBIDDEN, requiredDepartment + " department required");
        }
    }

    private boolean requiresAdmin() {
        var method = resourceInfo.getResourceMethod();
        if (method != null && method.isAnnotationPresent(RequiresAdmin.class)) {
            return true;
        }
        return resourceInfo.getResourceClass().isAnnotationPresent(RequiresAdmin.class);
    }

    private Department requiredDepartment() {
        var method = resourceInfo.getResourceMethod();
        RequiresDepartment annotation = method != null ? method.getAnnotation(RequiresDepartment.class) : null;
        if (annotation == null) {
            annotation = resourceInfo.getResourceClass().getAnnotation(RequiresDepartment.class);
        }
        return annotation != null ? annotation.value() : null;
    }

    private static void abort(ContainerRequestContext ctx, Response.Status status, String message) {
        ctx.abortWith(Response.status(status).entity("{\"detail\":\"" + message + "\"}")
                .type("application/json").build());
    }
}
