package com.rhlowery.acs.resource;

import com.rhlowery.acs.service.IdentityProvider;
import com.rhlowery.acs.service.TokenService;
import com.rhlowery.acs.service.UserService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "ACS Authentication and Identity provider endpoints")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    Instance<IdentityProvider> providers;

    @Inject
    TokenService tokenService;

    @Inject
    UserService userService;

    @Inject
    JsonWebToken jwt;
    
    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Login", description = "Authenticates a user and returns a session cookie")
    public Response login(Map<String, Object> body) {
        if (body == null) {
            return Response.status(400).entity(Map.of("error", "Request body is required")).build();
        }
        String userId = (String) body.get("userId");
        String providerId = (String) body.get("providerId");
        String personaInBody = (String) body.get("persona");
        if (personaInBody == null) {
            String roleInBody = (String) body.get("role");
            if (roleInBody != null && !"STANDARD_USER".equals(roleInBody)) {
                personaInBody = roleInBody;
            }
        }

        // For demo/simplified auth
        String userName = userId != null ? userId : "Anonymous";
        List<String> groups = new ArrayList<>(List.of("standard-users"));
        Object groupsObj = body.get("groups");
        if (groupsObj instanceof List) {
            for (Object g : (List<?>) groupsObj) {
                if (g instanceof String) {
                    groups.add((String) g);
                }
            }
        }
        String finalProviderId = providerId;
        
        // JIT Provisioning for locally authenticated users (Tests)
        String finalUserId = userId;
        String finalUserName = userName;
        String finalPersona = personaInBody;

        if (userId != null) {
            Optional<com.rhlowery.acs.domain.User> localUser = userService.getUser(userId);
            if (localUser.isEmpty()) {
                com.rhlowery.acs.domain.User newUser = new com.rhlowery.acs.domain.User(
                    finalUserId, finalUserName, finalUserId + "@example.com", "STANDARD_USER", List.copyOf(groups), finalPersona
                );
                userService.saveUser(newUser);
                LOG.info("JIT provisioned user on login: " + finalUserId);
            }
        }

        try {
            if (providerId != null) {
                Optional<IdentityProvider> providerOpt = providers.stream()
                        .filter(p -> p.getId().equals(providerId))
                        .findFirst();
                if (providerOpt.isEmpty()) {
                    return Response.status(400).entity(Map.of("error", "Unknown provider: " + providerId)).build();
                }
                IdentityProvider provider = providerOpt.get();
                Optional<Map<String, Object>> authResult = provider.authenticate(body);
                if (authResult.isEmpty()) {
                    return Response.status(401).entity(Map.of("error", "Invalid credentials via " + finalProviderId)).build();
                }

                userId = (String) authResult.get().get("userId");
                userName = authResult.get().containsKey("userName") ? (String) authResult.get().get("userName")
                        : userName;
                groups = provider.getGroups(userId);
                String authPersona = (String) authResult.get().get("persona");
                if (authPersona != null && !authPersona.isBlank()) {
                    finalPersona = authPersona;
                }
            }

            if (userId == null) {
                return Response.status(400).entity(Map.of("error", "userId is required")).build();
            }

            // Group-based role (legacy)
            String role = (groups != null && groups.contains("admins")) ? "ADMIN" : "STANDARD_USER";

            // If still null, we'll let it be null in the token so Augmentor can resolve it.
            String token = tokenService.generateToken(userId, userName, groups, role, finalPersona);

            return Response.ok(Map.of(
                    "status", "success",
                    "userId", userId,
                    "providerId", finalProviderId != null ? finalProviderId : "mock",
                    "token", token))
                    .header("Set-Cookie", "bff_jwt=" + token + "; Path=/; HttpOnly; SameSite=Strict")
                    .build();
        } catch (Exception e) {
            LOG.error("Login error", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Operation(summary = "Logout", description = "Invalidates the current user session")
    public Response logout() {
        return Response.ok(Map.of("status", "success"))
                .header("Set-Cookie", "bff_jwt=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict")
                .build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "GetCurrentUser", description = "Returns the profile of the currently authenticated user")
    @APIResponse(responseCode = "200", description = "Success")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response me() {
        if (securityIdentity.isAnonymous()) {
            LOG.warn("Anonymous call to /me");
            return Response.status(401).entity(Map.of("error", "Not authenticated")).build();
        }
        
        String userId = securityIdentity.getPrincipal().getName();
        String persona = securityIdentity.getAttribute("persona");
        if (persona == null) persona = "NONE";

        return Response.ok(Map.of(
                "authenticated", true,
                "userId", userId,
                "groups", securityIdentity.getRoles(),
                "persona", persona)).build();
    }

    @GET
    @Path("/providers")
    @Operation(summary = "List identity providers", description = "Returns a list of all supported 3rd-party identity providers")
    public Response listProviders() {
        List<Map<String, String>> providerList = providers.stream()
                .map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "type", p.getType()))
                .collect(Collectors.toList());
        return Response.ok(providerList).build();
    }

    @GET
    @Path("/config")
    @PermitAll
    @Operation(summary = "Get Auth Configuration", description = "Returns public OIDC configuration for the frontend")
    public Response getConfig() {
        String authServerUrl = System.getenv("OIDC_AUTH_SERVER_URL");
        String clientId = System.getenv("OIDC_CLIENT_ID");

        if (authServerUrl == null || authServerUrl.isEmpty() || "mock".equals(authServerUrl)) {
            return Response.ok(Map.of(
                    "authServerUrl", "http://localhost:8080/realms/quarkus",
                    "clientId", "quarkus-app",
                    "discoveryEnabled", true)).build();
        }

        return Response.ok(Map.of(
                "authServerUrl", authServerUrl,
                "clientId", clientId,
                "discoveryEnabled", true)).build();
    }

    @GET
    @Path("/personas")
    @RolesAllowed({"ADMIN", "SECURITY_ADMIN", "STANDARD_USER", "REQUESTER"}) // Allow all authenticated users
    @Operation(summary = "List Personas", description = "Returns available personas for mapping")
    public Response listPersonas() {
        List<Map<String, String>> personas = List.of(
            Map.of("id", "ADMIN", "name", "Admin"),
            Map.of("id", "APPROVER", "name", "Approver"),
            Map.of("id", "REQUESTER", "name", "Requester"),
            Map.of("id", "AUDITOR", "name", "Auditor"),
            Map.of("id", "GOVERNANCE_ADMIN", "name", "Governance Admin"),
            Map.of("id", "SECURITY_ADMIN", "name", "Security Admin"),
            Map.of("id", "NONE", "name", "None")
        );
        return Response.ok(personas).build();
    }

    @PUT
    @Path("/users/{userId}/persona")
    @RolesAllowed({"ADMIN", "SECURITY_ADMIN"})
    @Operation(summary = "Update User Persona", description = "Updates the persona mapping for a specific user")
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response updateUserPersona(@PathParam("userId") String userId, String persona) {
        LOG.info("Updating persona for user: " + userId + " to " + persona);
        try {
            userService.updateUserPersona(userId, persona);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @PUT
    @Path("/groups/{groupId}/persona")
    @RolesAllowed({"ADMIN", "SECURITY_ADMIN"})
    @Operation(summary = "Update Group Persona", description = "Updates the persona mapping for a specific group")
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response updateGroupPersona(@PathParam("groupId") String groupId, String persona) {
        LOG.info("Updating persona for group: " + groupId + " to " + persona);
        try {
            userService.updateGroupPersona(groupId, persona);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
