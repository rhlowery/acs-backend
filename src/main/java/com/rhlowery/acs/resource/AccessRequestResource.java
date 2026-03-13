package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.infrastructure.LineageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.OutboundSseEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@Path("/api/storage")
@Produces(MediaType.APPLICATION_JSON)
public class AccessRequestResource {

    private static final Logger LOG = Logger.getLogger(AccessRequestResource.class);

    @Inject
    AccessRequestService accessRequestService;

    @Inject
    LineageService lineageService;

    @Inject
    JsonWebToken jwt;

    @Context
    Sse sse;

    @Context
    HttpHeaders headers;

    private static final List<SseEventSink> sinks = new CopyOnWriteArrayList<>();

    private void logInfo(String method) {
        LOG.info("Method: " + method);
        if (headers != null) {
            headers.getRequestHeaders().forEach((k, v) -> LOG.info("Header: " + k + " = " + v));
        }
        if (jwt != null) {
            LOG.info("JWT present: " + (jwt.getName() != null) + ", Name: " + jwt.getName());
        }
    }

    @GET
    @Path("/requests/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context SseEventSink eventSink) {
        if (sse != null) {
            try {
                eventSink.send(sse.newEventBuilder().data("{\"type\": \"CONNECTED\"}").build());
            } catch (Exception e) {
                LOG.error("Failed to send connection event", e);
            }
        }
        sinks.add(eventSink);
    }

    private void notifyClients() {
        if (sse == null) {
            LOG.warn("Sse context is null, cannot notify clients");
            return;
        }
        for (SseEventSink sink : sinks) {
            if (sink.isClosed()) {
                sinks.remove(sink);
                continue;
            }
            try {
                OutboundSseEvent event = sse.newEventBuilder()
                        .name("message")
                        .data("{\"type\": \"UPDATE\"}")
                        .build();
                sink.send(event);
            } catch (Exception e) {
                LOG.error("Failed to send SSE event", e);
                sinks.remove(sink);
            }
        }
    }

    private List<String> getGroups() {
        try {
            if (jwt == null) return List.of();
            return (jwt.getGroups() != null) ? new ArrayList<>(jwt.getGroups()) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isUserAdmin() {
        try {
            if (jwt == null || jwt.getName() == null) {
                LOG.warn("No valid JWT principal found");
                return false;
            }
            List<String> groups = getGroups();
            String role = jwt.getClaim("role") != null ? jwt.getClaim("role").toString() : "STANDARD_USER";
            LOG.info("Checking admin for user: " + jwt.getName() + " with role: " + role + " and groups: " + groups);
            return "ADMIN".equals(role) || groups.contains("admins") || groups.contains("admin");
        } catch (Exception e) {
            return false;
        }
    }

    @GET
    @Path("/requests")
    public Response getRequests(@Context SecurityContext securityContext) {
        logInfo("getRequests");
        String userId = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        List<String> groups = getGroups();
        boolean isAdmin = isUserAdmin();

        List<AccessRequest> requests = accessRequestService.getAllRequests(userId, groups, isAdmin);
        
        List<Map<String, Object>> halRequests = requests.stream().map(this::toHal).collect(Collectors.toList());

        return Response.ok(halRequests).build();
    }
    @GET
    @Path("/requests/{id}")
    public Response getRequest(@PathParam("id") String id, @Context SecurityContext securityContext) {
        logInfo("getRequest: " + id);
        return accessRequestService.getRequestById(id)
            .map(this::toHal)
            .map(Response::ok)
            .orElseGet(() -> Response.status(404).entity(Map.of("error", "Request not found")))
            .build();
    }

    @POST
    @Path("/requests")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRequests(List<AccessRequest> requests, @Context SecurityContext securityContext) {
        try {
            logInfo("createRequests");
            String userId = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
            List<String> groups = getGroups();
            boolean isAdmin = isUserAdmin();

            accessRequestService.saveRequests(requests, userId, groups, isAdmin);
            try {
                requests.forEach(r -> lineageService.emitAccessRequestEvent(r, "SUBMITTED"));
            } catch (Exception e) {
                LOG.error("Failed to emit lineage event", e);
            }
            notifyClients();
            return Response.ok(Map.of("status", "success", "count", requests.size())).build();
        } catch (Exception e) {
            LOG.error("Error creating requests", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/requests/{id}/approve")
    public Response approveRequest(@PathParam("id") String id, @Context SecurityContext securityContext) {
        try {
            logInfo("approveRequest");
            if (!isUserAdmin()) {
                return Response.status(403).entity(Map.of("error", "Forbidden: Only admins can approve requests")).build();
            }
            LOG.info("Approving request: " + id);
            AccessRequest existing = accessRequestService.getRequestById(id)
                .orElseThrow(() -> new NotFoundException("Request not found: " + id));
            
            AccessRequest approved = new AccessRequest(
                existing.id(), existing.requesterId(), existing.userId(),
                existing.catalogName(), existing.schemaName(), existing.tableName(),
                existing.privileges(), "APPROVED", existing.createdAt(), System.currentTimeMillis(),
                existing.justification(), existing.approverGroups(), existing.metadata()
            );
            
            accessRequestService.saveRequests(List.of(approved), "system", List.of("admins"), true);
            try {
                lineageService.emitAccessRequestEvent(approved, "APPROVED");
            } catch (Exception e) {
                LOG.error("Failed to emit lineage event", e);
            }
            notifyClients();
            return Response.ok(Map.of("status", "success")).build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error approving request: " + id, e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/requests/{id}/reject")
    public Response rejectRequest(@PathParam("id") String id, @Context SecurityContext securityContext) {
        try {
            logInfo("rejectRequest");
            if (!isUserAdmin()) {
                return Response.status(403).entity(Map.of("error", "Forbidden: Only admins can reject requests")).build();
            }
            LOG.info("Rejecting request: " + id);
            AccessRequest existing = accessRequestService.getRequestById(id)
                .orElseThrow(() -> new NotFoundException("Request not found: " + id));
            
            AccessRequest rejected = new AccessRequest(
                existing.id(), existing.requesterId(), existing.userId(),
                existing.catalogName(), existing.schemaName(), existing.tableName(),
                existing.privileges(), "REJECTED", existing.createdAt(), System.currentTimeMillis(),
                existing.justification(), existing.approverGroups(), existing.metadata()
            );
            
            accessRequestService.saveRequests(List.of(rejected), "system", List.of("admins"), true);
            try {
                lineageService.emitAccessRequestEvent(rejected, "REJECTED");
            } catch (Exception e) {
                LOG.error("Failed to emit lineage event", e);
            }
            notifyClients();
            return Response.ok(Map.of("status", "success")).build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error rejecting request: " + id, e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    private Map<String, Object> toHal(AccessRequest r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.id());
        map.put("requesterId", r.requesterId());
        map.put("userId", r.userId());
        map.put("catalogName", r.catalogName());
        map.put("schemaName", r.schemaName());
        map.put("tableName", r.tableName());
        map.put("privileges", r.privileges());
        map.put("status", r.status());
        map.put("createdAt", r.createdAt());
        map.put("justification", r.justification());
        
        Map<String, Object> links = new HashMap<>();
        links.put("self", Map.of("href", "/api/storage/requests/" + r.id()));
        if ("PENDING".equals(r.status())) {
            links.put("approve", Map.of("href", "/api/storage/requests/" + r.id() + "/approve"));
            links.put("reject", Map.of("href", "/api/storage/requests/" + r.id() + "/reject"));
        }
        map.put("_links", links);
        return map;
    }
}
