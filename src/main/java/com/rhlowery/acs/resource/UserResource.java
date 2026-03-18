package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;

/**
 * REST Resource for managing users and group memberships.
 * Provides endpoints to list users, groups, and perform partial updates on user group assignments.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Management", description = "Endpoints for managing users and group memberships")
public class UserResource {

    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject
    UserService userService;

    @GET
    @Path("/users")
    @Operation(summary = "List all users", description = "Returns a list of all users and their group memberships")
    public Response listUsers() {
        LOG.info("Listing all users");
        return Response.ok(userService.listUsers()).build();
    }

    @GET
    @Path("/groups")
    @Operation(summary = "List all groups", description = "Returns a list of all available groups")
    public Response listGroups() {
        LOG.info("Listing all groups");
        return Response.ok(userService.listGroups()).build();
    }

    @PATCH
    @Path("/users/{id}/groups")
    @Operation(summary = "Update user groups", description = "Updates the list of groups for a specific user")
    public Response updateUserGroups(@PathParam("id") String userId, List<String> groups) {
        LOG.info("Updating groups for user: " + userId);
        try {
            User updatedUser = userService.updateUserGroups(userId, groups);
            return Response.ok(updatedUser).build();
        } catch (IllegalArgumentException e) {
            LOG.warn("User not found: " + userId);
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            LOG.error("Error updating user groups", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/users/{id}")
    @Operation(summary = "Get user details", description = "Returns details for a specific user")
    public Response getUser(@PathParam("id") String userId) {
        return userService.getUser(userId)
            .map(Response::ok)
            .orElse(Response.status(404).entity(Map.of("error", "User not found")))
            .build();
    }
}
