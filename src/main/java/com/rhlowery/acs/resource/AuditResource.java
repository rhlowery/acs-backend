package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.AuditEntry;
import com.rhlowery.acs.service.AuditService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ForbiddenException;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;

@Path("/api/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Audit", description = "Endpoints for logging and retrieving audit events")
public class AuditResource {

    private static final Logger LOG = Logger.getLogger(AuditResource.class);

    @Inject
    AuditService auditService;
    
    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/log")
    @Operation(summary = "Log an audit event", description = "Stores a new audit entry in the central log")
    @APIResponse(responseCode = "200", description = "Event logged successfully")
    public Response log(AuditEntry entry, @Context SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        
        AuditEntry signedEntry = new AuditEntry(
            entry.id() != null ? entry.id() : UUID.randomUUID().toString(),
            entry.type(),
            entry.actor(),
            userId,
            entry.timestamp() != null ? entry.timestamp() : System.currentTimeMillis(),
            System.currentTimeMillis(),
            entry.details(),
            "MOCK_SIGNATURE",
            "unity-catalog-acs-bff"
        );

        auditService.log(signedEntry);
        return Response.ok(Map.of("status", "success")).build();
    }

    @GET
    @Path("/log")
    @Operation(summary = "Get audit log", description = "Returns a list of all audit events")
    public Response getLog() {
        return Response.ok(auditService.getLogs()).build();
    }

    @POST
    @Path("/log/ui")
    @Operation(summary = "Log UI event", description = "Logs client-side events to the server console")
    public Response logUi(Map<String, Object> body) {
        LOG.infof("[UI-LOG] level=%s message=%s", body.get("level"), body.get("message"));
        return Response.noContent().build();
    }

    @Inject
    io.quarkus.security.identity.SecurityIdentity securityIdentity;

    @GET
    @Path("/log/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Stream audit logs", description = "Real-time SSE stream of audit events")
    public Multi<AuditEntry> streamLogs() {
        if (!securityIdentity.hasRole("AUDITOR") && 
            !securityIdentity.hasRole("REVIEWER") && 
            !securityIdentity.hasRole("ADMIN")) {
            throw new ForbiddenException("Access denied: AUDITOR, REVIEWER, or ADMIN persona required");
        }
        return auditService.streamLogs();
    }
}
