package com.strides.controller;

import com.strides.dto.AuthDtos.LoginRequest;
import com.strides.dto.AuthDtos.RefreshResponse;
import com.strides.dto.AuthDtos.TokenResponse;
import com.strides.dto.AuthDtos.UserPublic;
import com.strides.security.CurrentUser;
import com.strides.service.AuthService;
import com.strides.service.AuthService.LoginResult;
import com.strides.service.AuthService.RefreshResult;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    // Scoped to /api/auth only: the cookie is never sent on ordinary KPI-endpoint requests, which
    // is the entire point of keeping it separate from the (short-lived) Authorization header token.
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService auth;
    private final CurrentUser currentUser;
    private final boolean cookieSecure;
    private final int refreshMaxAgeSeconds;

    public AuthController(AuthService auth, CurrentUser currentUser,
            @ConfigProperty(name = "auth.cookie.secure") boolean cookieSecure,
            @ConfigProperty(name = "auth.refresh-token-expire-days") long refreshExpireDays) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.cookieSecure = cookieSecure;
        this.refreshMaxAgeSeconds = Math.toIntExact(refreshExpireDays * 24 * 60 * 60);
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        LoginResult result = auth.login(request.email(), request.password());
        return Response.ok(new TokenResponse(result.accessToken(), result.user()))
                .cookie(refreshCookie(result.refreshToken(), refreshMaxAgeSeconds))
                .header("Cache-Control", "no-store")
                .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(REFRESH_COOKIE_NAME) Cookie refreshCookie) {
        String raw = refreshCookie != null ? refreshCookie.getValue() : null;
        if (raw == null) {
            throw new NotAuthorizedException("Missing refresh token", "Bearer");
        }
        RefreshResult result = auth.refresh(raw)
                .orElseThrow(() -> new NotAuthorizedException("Invalid or expired refresh token", "Bearer"));
        return Response.ok(new RefreshResponse(result.accessToken()))
                .cookie(refreshCookie(result.refreshToken(), refreshMaxAgeSeconds))
                .header("Cache-Control", "no-store")
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(REFRESH_COOKIE_NAME) Cookie refreshCookie) {
        auth.logout(refreshCookie != null ? refreshCookie.getValue() : null);
        return Response.noContent().cookie(refreshCookie("", 0)).build();
    }

    @GET
    @Path("/me")
    public UserPublic me() {
        return AuthService.toPublic(currentUser.get());
    }

    private NewCookie refreshCookie(String value, int maxAgeSeconds) {
        return new NewCookie.Builder(REFRESH_COOKIE_NAME)
                .value(value)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }
}
