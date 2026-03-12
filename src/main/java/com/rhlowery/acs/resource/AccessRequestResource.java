package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
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

@Path("/api/storage")
@Produces(MediaType.APPLICATION_JSON)
public class AccessRequestResource {

    @Inject
    AccessRequestService accessRequestService;

    @Inject
    JsonWebToken jwt;

    private static final List<SseEventSink> sinks = new CopyOnWriteArrayList<>();

    @GET
    @Path("/requests/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context Sse sse, @Context SseEventSink eventSink) {
        eventSink.send(sse.newEventBuilder().data("{\"type\": \"CONNECTED\"}").build());
        sinks.add(eventSink);
    }

    private void notifyClients(Sse sse) {
        for (SseEventSink sink : sinks) {
            if (sink.isClosed()) {
                sinks.remove(sink);
                continue;
            }
            OutboundSseEvent event = sse.newEventBuilder()
                    .name("message")
                    .data("{\"type\": \"UPDATE\"}")
                    .build();
            sink.send(event);
        }
    }

    private List<String> getGroups() {
        try {
            return (jwt != null && jwt.getGroups() != null) ? new ArrayList<>(jwt.getGroups()) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isUserAdmin() {
        try {
            if (jwt == null) return false;
            List<String> groups = getGroups();
            String role = jwt.getClaim("role") != null ? jwt.getClaim("role").toString() : "STANDARD_USER";
            return "ADMIN".equals(role) || groups.contains("admins") || groups.contains("admin");
        } catch (Exception e) {
            return false;
        }
    }

    @GET
    @Path("/requests")
    public Response getRequests(@Context SecurityContext securityContext) {
        String userId = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        List<String> groups = getGroups();
        boolean isAdmin = isUserAdmin();

        List<AccessRequest> requests = accessRequestService.getAllRequests(userId, groups, isAdmin);
        
        List<Map<String, Object>> halRequests = requests.stream().map(this::toHal).collect(Collectors.toList());

        return Response.ok(halRequests).build();
    }

    @POST
    @Path("/requests")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRequests(List<AccessRequest> requests, @Context SecurityContext securityContext, @Context Sse sse) {
        String userId = securityContext.getUserPrincipal() != null ? securityContext.getUserPrincipal().getName() : "anonymous";
        List<String> groups = getGroups();
        boolean isAdmin = isUserAdmin();

        accessRequestService.saveRequests(requests, userId, groups, isAdmin);
        notifyClients(sse);
        return Response.ok(Map.of("status", "success", "count", requests.size())).build();
    }

    @POST
    @Path("/requests/{id}/approve")
    public Response approveRequest(@PathParam("id") String id, @Context SecurityContext securityContext, @Context Sse sse) {
        AccessRequest existing = accessRequestService.getRequestById(id)
            .orElseThrow(() -> new NotFoundException("Request not found: " + id));
        
        AccessRequest approved = new AccessRequest(
            existing.id(), existing.requesterId(), existing.userId(),
            existing.catalogName(), existing.schemaName(), existing.tableName(),
            existing.privileges(), "APPROVED", existing.createdAt(), System.currentTimeMillis(),
            existing.justification(), existing.approverGroups(), existing.metadata()
        );
        
        accessRequestService.saveRequests(List.of(approved), "system", List.of("admins"), true);
        notifyClients(sse);
        return Response.ok(Map.of("status", "success")).build();
    }

    @POST
    @Path("/requests/{id}/reject")
    public Response rejectRequest(@PathParam("id") String id, @Context SecurityContext securityContext, @Context Sse sse) {
        AccessRequest existing = accessRequestService.getRequestById(id)
            .orElseThrow(() -> new NotFoundException("Request not found: " + id));
        
        AccessRequest rejected = new AccessRequest(
            existing.id(), existing.requesterId(), existing.userId(),
            existing.catalogName(), existing.schemaName(), existing.tableName(),
            existing.privileges(), "REJECTED", existing.createdAt(), System.currentTimeMillis(),
            existing.justification(), existing.approverGroups(), existing.metadata()
        );
        
        accessRequestService.saveRequests(List.of(rejected), "system", List.of("admins"), true);
        notifyClients(sse);
        return Response.ok(Map.of("status", "success")).build();
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
