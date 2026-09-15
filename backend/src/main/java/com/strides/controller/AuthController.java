package com.strides.controller;

import com.strides.dto.AuthDtos.LoginRequest;
import com.strides.dto.AuthDtos.RefreshResponse;
import com.strides.dto.AuthDtos.RefreshTokenRequest;
import com.strides.dto.AuthDtos.TokenResponse;
import com.strides.dto.AuthDtos.UserPublic;
import com.strides.security.CurrentUser;
import com.strides.service.AuthService;
import com.strides.service.AuthService.LoginResult;
import com.strides.service.AuthService.RefreshResult;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Plain bearer-token transport throughout - no cookies. This app is deployed inside a VNet over
 * plain http, where a cookie's Secure attribute (required for a browser to ever send it) can't be
 * satisfied; the client stores both tokens itself (localStorage) and sends the refresh token back
 * explicitly. See AUTHENTICATION.md.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;

    public AuthController(AuthService auth, CurrentUser currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @POST
    @Path("/login")
    public TokenResponse login(@Valid LoginRequest request) {
        LoginResult result = auth.login(request.email(), request.password());
        return new TokenResponse(result.accessToken(), result.refreshToken(), result.user());
    }

    @POST
    @Path("/refresh")
    public RefreshResponse refresh(@Valid RefreshTokenRequest request) {
        RefreshResult result = auth.refresh(request.refreshToken())
                .orElseThrow(() -> new NotAuthorizedException("Invalid or expired refresh token", "Bearer"));
        return new RefreshResponse(result.accessToken(), result.refreshToken());
    }

    @POST
    @Path("/logout")
    public void logout(@Valid RefreshTokenRequest request) {
        auth.logout(request.refreshToken());
    }

    @GET
    @Path("/me")
    public UserPublic me() {
        return AuthService.toPublic(currentUser.get());
    }
}
