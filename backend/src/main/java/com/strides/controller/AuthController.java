package com.strides.controller;

import com.strides.dto.AuthDtos.ChangePasswordRequest;
import com.strides.dto.AuthDtos.FeedbackTokenResponse;
import com.strides.dto.AuthDtos.LoginRequest;
import com.strides.dto.AuthDtos.RefreshResponse;
import com.strides.dto.AuthDtos.RefreshTokenRequest;
import com.strides.dto.AuthDtos.TeamMember;
import com.strides.dto.AuthDtos.TokenResponse;
import com.strides.dto.AuthDtos.UserPublic;
import com.strides.security.CurrentUser;
import com.strides.security.FeedbackJwtService;
import com.strides.service.AuthService;
import com.strides.service.AuthService.LoginResult;
import com.strides.service.AuthService.RefreshResult;
import com.strides.service.TeamService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

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
    private final FeedbackJwtService feedbackJwt;
    private final TeamService team;

    public AuthController(AuthService auth, CurrentUser currentUser, FeedbackJwtService feedbackJwt, TeamService team) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.feedbackJwt = feedbackJwt;
        this.team = team;
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

    /** Self-service password change - distinct from UsersController's admin-only reset (see
     * AuthService.changePassword). */
    @POST
    @Path("/change-password")
    public TokenResponse changePassword(@Valid ChangePasswordRequest request) {
        LoginResult result = auth.changePassword(currentUser.get(), request.currentPassword(), request.newPassword());
        return new TokenResponse(result.accessToken(), result.refreshToken(), result.user());
    }

    /** The current user's own direct-report analysts (TeamWorkloadOverview's real roster) - open to
     * any authenticated user, not @RequiresAdmin like UsersController, since this is about the
     * caller's own team, not admin user-management. Empty for anyone nobody reports to. */
    @GET
    @Path("/me/analysts")
    public List<TeamMember> myAnalysts() {
        return team.myAnalysts(currentUser.get());
    }

    /** Requires an already-valid session (not in AuthenticationFilter's PUBLIC_PATHS) - see FeedbackJwtService. */
    @GET
    @Path("/feedback-token")
    public FeedbackTokenResponse feedbackToken() {
        return new FeedbackTokenResponse(feedbackJwt.issue(currentUser.get().getId().toString(), currentUser.get().getFullName()));
    }
}
