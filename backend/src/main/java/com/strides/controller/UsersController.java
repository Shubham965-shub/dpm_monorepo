package com.strides.controller;

import com.strides.dto.UsersDtos.CreateUserRequest;
import com.strides.dto.UsersDtos.CreateUserResponse;
import com.strides.dto.UsersDtos.RolePublic;
import com.strides.dto.UsersDtos.TempPasswordResponse;
import com.strides.dto.UsersDtos.UpdateUserRequest;
import com.strides.dto.UsersDtos.UserAdmin;
import com.strides.security.CurrentUser;
import com.strides.security.RequiresPermission;
import com.strides.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only user management: create/update/activate-deactivate/reset-password. Class-level
 * @RequiresPermission means every endpoint here is protected even if a future addition forgets to
 * annotate it individually - enforced by AuthenticationFilter.
 */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresPermission("admin:*")
public class UsersController {

    private final UserAdminService service;
    private final CurrentUser currentUser;

    public UsersController(UserAdminService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GET
    public List<UserAdmin> list() {
        return service.listUsers();
    }

    @GET
    @Path("/roles")
    public List<RolePublic> listRoles() {
        return service.listRoles();
    }

    @POST
    public Response create(@Valid CreateUserRequest request) {
        CreateUserResponse created = service.createUser(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /** Also how activate/deactivate happens - PATCH { "isActive": false }. */
    @PATCH
    @Path("/{id}")
    public UserAdmin update(@PathParam("id") UUID id, @Valid UpdateUserRequest request) {
        return service.updateUser(id, request, currentUser.get());
    }

    @POST
    @Path("/{id}/reset-password")
    public TempPasswordResponse resetPassword(@PathParam("id") UUID id) {
        return new TempPasswordResponse(service.resetPassword(id));
    }
}
