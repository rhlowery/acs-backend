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
import org.jboss.logging.Logger;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.inject.Inject;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
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

            String token = Jwt.issuer("unity-catalog-acs-bff")
                .upn(userId)
                .groups(new HashSet<>(groups))
                .claim("userId", userId)
                .claim("userName", userName)
                .claim("role", role)
                .sign();

            NewCookie cookie = new NewCookie.Builder("bff_jwt")
                .value(token)
                .path("/")
                .httpOnly(true)
                .secure(false) 
                .maxAge(3600)
                .build();

            LOG.info("Login successful for " + userId + ". Token first chars: " + token.substring(0, Math.min(token.length(), 10)));
            return Response.ok(Map.of("status", "success", "userId", userId, "role", role))
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
    public Response me(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            LOG.warn("No principal found in /me");
            return Response.status(401).entity(Map.of("error", "Not authenticated")).build();
        }
        return Response.ok(Map.of("authenticated", true, "userId", securityContext.getUserPrincipal().getName())).build();
    }
}
