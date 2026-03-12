package com.rhlowery.acs.resource;

import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @POST
    @Path("/login")
    public Response login(Map<String, Object> body) {
        String userId = (String) body.get("userId");
        String userName = (String) body.get("userName");
        String role = (String) body.getOrDefault("role", "STANDARD_USER");
        @SuppressWarnings("unchecked")
        List<String> groups = (List<String>) body.getOrDefault("groups", List.of());
        
        List<String> permissions = "ADMIN".equals(role) 
            ? List.of("can_request", "can_approve", "can_audit", "can_configure", "can_manage_users")
            : List.of("can_request");

        String token = Jwt.issuer("unity-catalog-acs-bff")
            .audience("unity-catalog-acs-ui")
            .subject(userId)
            .upn(userId)
            .claim("name", userName)
            .claim("role", role)
            .claim("groups", groups)
            .claim("permissions", permissions)
            .groups(new HashSet<>(groups))
            .claim("jti", UUID.randomUUID().toString())
            .expiresAt(Instant.now().plusSeconds(8 * 3600))
            .sign();

        NewCookie bffJwtCookie = new NewCookie.Builder("bff_jwt")
            .value(token)
            .path("/")
            .httpOnly(true)
            .maxAge(8 * 3600)
            .build();

        String csrfToken = UUID.randomUUID().toString();
        NewCookie csrfCookie = new NewCookie.Builder("csrf_token")
            .value(csrfToken)
            .path("/")
            .httpOnly(false)
            .maxAge(3600)
            .build();

        return Response.ok(Map.of(
            "status", "success",
            "user", body, // Simplified for now
            "csrfToken", csrfToken
        ))
        .cookie(bffJwtCookie, csrfCookie)
        .build();
    }

    @GET
    @Path("/me")
    public Response me(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        // Extract user info from principal
        return Response.ok(Map.of("user", securityContext.getUserPrincipal().getName())).build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie bffJwtCookie = new NewCookie.Builder("bff_jwt")
            .value("")
            .path("/")
            .maxAge(0)
            .build();
        return Response.ok(Map.of("status", "success")).cookie(bffJwtCookie).build();
    }
}
