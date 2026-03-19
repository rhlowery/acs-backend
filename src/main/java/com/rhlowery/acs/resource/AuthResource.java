package com.rhlowery.acs.resource;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import com.rhlowery.acs.service.IdentityProvider;
import com.rhlowery.acs.service.UserService;
import com.rhlowery.acs.domain.Persona;
import com.rhlowery.acs.domain.User;

/**
 * REST Resource for user authentication and profile management.
 * Handles login, logout, and token-based session management using JWT.
 * Supports both local authentication and pluggable identity providers.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Endpoints for user login, logout and profile")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    UserService userService;

    @Inject
    jakarta.enterprise.inject.Instance<IdentityProvider> providersInstance;

    private List<IdentityProvider> providers;

    @jakarta.annotation.PostConstruct
    void init() {
        this.providers = providersInstance.stream().collect(Collectors.toList());
    }

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT in a cookie")
    @APIResponse(responseCode = "200", description = "Login successful")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public Response login(Map<String, Object> body) {
        try {
            String userId = (String) body.get("userId");
            if (userId == null || userId.trim().isEmpty()) {
                return Response.status(400).entity(Map.of("error", "userId is required")).build();
            }
            LOG.info("Login request for user: " + userId);
            String userName = (String) body.getOrDefault("userName", userId);
            String role = (String) body.getOrDefault("role", "STANDARD_USER");
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) body.getOrDefault("groups", List.of());
            String personaInBody = (String) body.get("persona");

            String providerId = (String) body.get("providerId");
            if (providerId != null) {
                LOG.info("Login via provider: " + providerId);
                IdentityProvider provider = providers.stream()
                    .filter(p -> providerId.equals(p.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (provider == null) {
                    return Response.status(400).entity(Map.of("error", "Unknown provider: " + providerId)).build();
                }

                Optional<Map<String, Object>> authResult = provider.authenticate(body);
                if (authResult.isEmpty()) {
                    return Response.status(401).entity(Map.of("error", "Invalid credentials for " + providerId)).build();
                }

                userId = (String) authResult.get().get("userId");
                groups = provider.getGroups(userId);
                // The role will be determined later based on persona and groups
            }

            // Check for locally assigned persona (User or Group)
            Optional<User> localUser = userService.getUser(userId);
            String persona = localUser.isPresent() ? localUser.get().persona() : personaInBody;
            
        if (persona == null && groups != null) {
            for (String groupId : groups) {
                Optional<com.rhlowery.acs.domain.Group> g = userService.getGroup(groupId);
                if (g.isPresent() && g.get().persona() != null) {
                    persona = g.get().persona();
                    break;
                }
            }
        }

        // Persona takes precedence for capability, but role should still reflect access level
        role = (groups != null && groups.contains("admins")) ? "ADMIN" : "STANDARD_USER";
        if ("ADMIN".equals(persona) || "SECURITY_ADMIN".equals(persona) || "PLATFORM_ADMIN".equals(persona)) {
            role = "ADMIN";
        }

        io.smallrye.jwt.build.JwtClaimsBuilder tokenBuilder = Jwt.issuer("unity-catalog-acs-bff")
                .upn(userId)
                .subject(userId)
                .groups(new HashSet<>(groups))
                .claim("userId", userId)
                .claim("userName", userName)
                .claim("role", role);
            
            if (persona != null) {
                tokenBuilder.claim("persona", persona);
            }
            
            String token = tokenBuilder.sign();

            NewCookie cookie = new NewCookie.Builder("bff_jwt")
                .value(token)
                .path("/")
                .httpOnly(true)
                .secure(false) 
                .maxAge(3600)
                .build();

            LOG.info("Login successful for " + userId + (providerId != null ? " via " + providerId : "") + ". Persona: " + persona + ". Token first chars: " + token.substring(0, Math.min(token.length(), 10)));
            return Response.ok(Map.of("status", "success", "userId", userId, "role", role, "persona", persona != null ? persona : "NONE", "providerId", providerId != null ? providerId : "local"))
                .cookie(cookie)
                .build();
        } catch (Exception e) {
            LOG.error("Error in login", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/logout")
    @PermitAll
    @Operation(summary = "Logout", description = "Clears the authentication cookie")
    @APIResponse(responseCode = "200", description = "Logout successful")
    public Response logout() {
        NewCookie cookie = new NewCookie.Builder("bff_jwt")
            .value("")
            .path("/")
            .maxAge(0)
            .build();
        return Response.ok(Map.of("status", "logged out")).cookie(cookie).build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "GetCurrentUser", description = "Returns the profile of the currently authenticated user")
    @APIResponse(responseCode = "200", description = "Success")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response me(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            LOG.warn("No principal found in /me");
            return Response.status(401).entity(Map.of("error", "Not authenticated")).build();
        }
        String userId = securityContext.getUserPrincipal().getName();
        Optional<User> localUser = userService.getUser(userId);
        
        String persona = localUser.isPresent() ? localUser.get().persona() : null;
        if (persona == null && jwt.getGroups() != null) {
            for (String groupId : jwt.getGroups()) {
                Optional<com.rhlowery.acs.domain.Group> g = userService.getGroup(groupId);
                if (g.isPresent() && g.get().persona() != null) {
                    persona = g.get().persona();
                    break;
                }
            }
        }

        return Response.ok(Map.of(
            "authenticated", true, 
            "userId", userId,
            "groups", jwt.getGroups() != null ? jwt.getGroups() : List.of(),
            "persona", persona != null ? persona : "NONE"
        )).build();
    }

    @GET
    @Path("/providers")
    @Operation(summary = "List identity providers", description = "Returns a list of all supported 3rd-party identity providers")
    public Response listProviders() {
        List<Map<String, String>> providerList = providers.stream()
            .map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "type", p.getType()
            ))
            .collect(Collectors.toList());
        return Response.ok(providerList).build();
    }
    @GET
    @Path("/personas")
    @Operation(summary = "List available personas", description = "Returns a list of all available system-wide personas")
    public Response listAvailablePersonas() {
        return Response.ok(Persona.all()).build();
    }

    @PUT
    @Path("/users/{userId}/persona")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Assign persona to user", description = "Explicitly assigns a persona to a specific user")
    public Response assignPersonaToUser(@PathParam("userId") String userId, String persona) {
        try {
            User updated = userService.updateUserPersona(userId, persona);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @PUT
    @Path("/groups/{groupId}/persona")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Assign persona to group", description = "Explicitly assigns a persona to a specific group")
    public Response assignPersonaToGroup(@PathParam("groupId") String groupId, String persona) {
        try {
            com.rhlowery.acs.domain.Group updated = userService.updateGroupPersona(groupId, persona);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
