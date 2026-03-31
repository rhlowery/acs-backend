package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.infrastructure.LineageService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.OutboundSseEvent;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST Resource for managing the lifecycle of data access requests.
 * Supports submission, approval, rejection, and verification of requests.
 * Provides real-time updates via Server-Sent Events (SSE).
 */
@jakarta.enterprise.context.ApplicationScoped
@Path("/api/storage/requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Access Requests", description = "Endpoints for managing the lifecycle of data access requests")
public class AccessRequestResource {

    private static final Logger LOG = Logger.getLogger(AccessRequestResource.class);

    @Inject
    AccessRequestService accessRequestService;

    @Inject
    LineageService lineageService;

    @Inject
    com.rhlowery.acs.service.CatalogService catalogService;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwt;

    @Context
    Sse sse;

    private final List<SseEventSink> sinks = new CopyOnWriteArrayList<>();

    public static class HalAccessRequest {
        public String id;
        public String catalogName;
        public String schemaName;
        public String tableName;
        public List<String> privileges;
        public String status;
        public String justification;
        public String requesterId;
        public String userId;
        public String principalType;
        public String resourceType;
        public String rejectionReason;
        public List<String> approverGroups;
        public Long expirationTime;
        public Map<String, Object> _links;
        
        public HalAccessRequest(AccessRequest r) {
            this.id = r.id();
            this.catalogName = r.catalogName();
            this.schemaName = r.schemaName();
            this.tableName = r.tableName();
            this.privileges = r.privileges();
            this.status = r.status();
            this.justification = r.justification();
            this.rejectionReason = r.rejectionReason();
            this.requesterId = r.requesterId();
            this.userId = r.userId();
            this.principalType = r.principalType();
            this.resourceType = r.resourceType();
            this.approverGroups = r.approverGroups();
            this.expirationTime = r.expirationTime();
            Map<String, Object> links = new java.util.HashMap<>();
            links.put("self", Map.of("href", "/api/storage/requests/" + r.id()));
            if ("PENDING".equals(r.status()) || "PARTIALLY_APPROVED".equals(r.status())) {
                links.put("approve", Map.of("href", "/api/storage/requests/" + r.id() + "/approve"));
                links.put("reject", Map.of("href", "/api/storage/requests/" + r.id() + "/reject"));
            } else if ("APPROVED".equals(r.status())) {
                links.put("verify", Map.of("href", "/api/storage/requests/" + r.id() + "/verify"));
            }
            this._links = links;
        }
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream requests", description = "Provides a Server-Sent Events stream for real-time access request updates")
    public Response stream(@Context SseEventSink eventSink) {
        if (sse != null) {
            sinks.add(eventSink);
            OutboundSseEvent event = sse.newEventBuilder()
                .name("connected")
                .data("SSE stream active")
                .build();
            eventSink.send(event);
        }
        return Response.ok().build();
    }

    private void broadcast(String name, Object data) {
        if (sse != null) {
            OutboundSseEvent event = sse.newEventBuilder()
                .name(name)
                .data(data)
                .build();
            sinks.removeIf(sink -> {
                try {
                    sink.send(event);
                    return false;
                } catch (Exception e) {
                    return true;
                }
            });
        }
    }

    @GET
    @Operation(summary = "List access requests", description = "Returns a list of all access requests with HATEOAS links")
    public Response getRequests(@Context SecurityContext securityContext) {
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName() : "anonymous";
        List<String> groups = new ArrayList<>(securityIdentity.getRoles());
        String persona = securityIdentity.getAttribute("persona");
        boolean isAdmin = !"REQUESTER".equals(persona) && (securityIdentity.hasRole("ADMIN") || securityIdentity.hasRole("SECURITY_ADMIN") || securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN") || securityIdentity.hasRole("AUDITOR") || groups.contains("admins"));
        
        LOG.infof("Listing requests for user: %s, persona=%s, isAdmin=%b", userId, persona, isAdmin);
        List<HalAccessRequest> halRequests = accessRequestService.getAllRequests(userId, groups, isAdmin).stream()
            .map(HalAccessRequest::new)
            .collect(Collectors.toList());
            
        return Response.ok(halRequests).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get access request details", description = "Returns details for a specific access request with HATEOAS links")
    public Response getRequest(@PathParam("id") String id) {
        return accessRequestService.getRequestById(id)
            .map(r -> Response.ok(new HalAccessRequest(r)))
            .orElse(Response.status(404))
            .build();
    }

    @POST
    @Operation(summary = "Submit access requests", description = "Submits one or more access requests for review")
    public Response createRequests(List<AccessRequest> requests, @Context SecurityContext securityContext) {
        if (requests == null || requests.isEmpty()) {
            return Response.status(400).entity(Map.of("error", "Request list cannot be empty")).build();
        }
        
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName() : "anonymous";
        List<String> groups = new ArrayList<>(securityIdentity.getRoles());
        String persona = securityIdentity.getAttribute("persona");
        boolean isAdmin = !"REQUESTER".equals(persona) && (securityIdentity.hasRole("ADMIN") || securityIdentity.hasRole("SECURITY_ADMIN") || securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN") || securityIdentity.hasRole("AUDITOR") || groups.contains("admins"));

        for (AccessRequest r : requests) {
            String path = "/" + r.catalogName() + "/" + r.schemaName() + "/" + r.tableName();
            List<String> requiredApprovers = new ArrayList<>();
            
            // 1. Resolve approvers from catalog
            List<String> catalogApprovers = catalogService.getRequiredApprovers(r.catalogName(), path);
            if (catalogApprovers != null) {
                requiredApprovers.addAll(catalogApprovers);
            }

            // 2. Add governance-team if not present (Mandatory Governance)
            if (!requiredApprovers.contains("governance-team")) {
                requiredApprovers.add("governance-team");
            }

            AccessRequest enriched = new AccessRequest(
                r.id(),
                userId,
                r.userId() != null ? r.userId() : userId,
                r.principalType() != null ? r.principalType() : "USER",
                r.catalogName(),
                r.schemaName(),
                r.tableName(),
                r.resourceType() != null ? r.resourceType() : "TABLE",
                r.privileges(),
                (isAdmin && r.status() != null) ? r.status() : "PENDING",
                System.currentTimeMillis(),
                null,
                r.justification(),
                null, // rejectionReason is null for new requests
                requiredApprovers,
                r.metadata() != null ? r.metadata() : new java.util.HashMap<>(),
                r.expirationTime()
            );
            accessRequestService.saveRequests(List.of(enriched), userId, groups, isAdmin);
            lineageService.emitAccessRequestEvent(enriched, userId);
        }
        
        broadcast("request-created", requests);
        return Response.ok(Map.of("status", "success", "count", requests.size())).build();
    }

    @POST
    @Path("/{id}/approve")
    @Operation(summary = "Approve access request", description = "Approves a pending access request (Admins only)")
    public Response approveRequest(@PathParam("id") String id, @Context SecurityContext securityContext) {
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName() : "anonymous";
        List<String> groups = new ArrayList<>(securityIdentity.getRoles());
        String persona = securityIdentity.getAttribute("persona");
        boolean isAdmin = !"REQUESTER".equals(persona) && (securityIdentity.hasRole("ADMIN") || securityIdentity.hasRole("SECURITY_ADMIN") || securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN") || securityIdentity.hasRole("AUDITOR") || groups.contains("admins"));

        return accessRequestService.getRequestById(id)
            .map(r -> {
                if (!"PENDING".equals(r.status()) && !"PARTIALLY_APPROVED".equals(r.status())) {
                    return Response.status(400).entity(Map.of("error", "Request is not in a state that can be approved")).build();
                }

                List<String> userGroups = new ArrayList<>(securityIdentity.getRoles());
                boolean isDesignatedApprover = !"REQUESTER".equals(persona) && r.approverGroups() != null && r.approverGroups().stream().anyMatch(userGroups::contains);
                
                boolean isAuthorized = isAdmin || isDesignatedApprover;

                if (!isAuthorized) {
                    return Response.status(403).entity(Map.of("error", "You are not an authorized approver for this request")).build();
                }

                Map<String, Object> meta = r.metadata() != null ? new java.util.HashMap<>(r.metadata()) : new java.util.HashMap<>();
                @SuppressWarnings("unchecked")
                List<String> signs = (List<String>) meta.getOrDefault("approvals", new ArrayList<String>());
                
                // Track which group this user is approving for
                if (isDesignatedApprover && !isAdmin) {
                    String approvingGroup = r.approverGroups().stream().filter(userGroups::contains).findFirst().get();
                    if (!signs.contains(approvingGroup)) {
                        signs.add(approvingGroup);
                    }
                }
                meta.put("approvals", signs);

                boolean isSystemApprover = securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN");
                boolean fullyApproved = isAdmin || isSystemApprover || (r.approverGroups() == null || r.approverGroups().isEmpty() || signs.size() >= r.approverGroups().size());
                String newStatus = fullyApproved ? "APPROVED" : "PARTIALLY_APPROVED";

                AccessRequest updated = new AccessRequest(
                    r.id(), r.requesterId(), r.userId(), r.principalType(), r.catalogName(), r.schemaName(), r.tableName(), 
                    r.resourceType(), r.privileges(), newStatus, r.createdAt(), System.currentTimeMillis(), 
                    r.justification(), r.rejectionReason(), r.approverGroups(), meta, r.expirationTime()
                );

                if (fullyApproved) {
                    try {
                        String principal = r.userId() != null ? r.userId() : r.requesterId();
                        String path = "/" + r.catalogName() + "/" + r.schemaName() + "/" + r.tableName();
                        
                        catalogService.applyPolicy(r.catalogName(), path, r.privileges().get(0), principal);
                    } catch (Exception e) {
                        LOG.error("Failed to grant access on UC: " + e.getMessage(), e);
                    }
                }

                accessRequestService.saveRequests(List.of(updated), userId, groups, isAdmin);
                broadcast("request-updated", List.of(updated));
                return Response.ok(new HalAccessRequest(updated)).build();
            })
            .orElse(Response.status(404).entity(Map.of("error", "Request not found")).build());
    }

    public static class RejectionRequest {
        public String reason;
    }

    @POST
    @Path("/{id}/reject")
    @Operation(summary = "Reject access request", description = "Rejects a pending access request (Admins or designated approvers)")
    public Response rejectRequest(@PathParam("id") String id, RejectionRequest rejection, @Context SecurityContext securityContext) {
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName() : "anonymous";
        List<String> groups = new ArrayList<>(securityIdentity.getRoles());
        String persona = securityIdentity.getAttribute("persona");
        boolean isAdmin = !"REQUESTER".equals(persona) && (securityIdentity.hasRole("ADMIN") || securityIdentity.hasRole("SECURITY_ADMIN") || securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN") || securityIdentity.hasRole("AUDITOR") || groups.contains("admins"));

        if (rejection == null || rejection.reason == null || rejection.reason.isBlank()) {
            return Response.status(400).entity(Map.of("error", "Rejection reason is mandatory")).build();
        }

        return accessRequestService.getRequestById(id)
            .map(r -> {
                List<String> userGroups = new ArrayList<>(securityIdentity.getRoles());
                boolean isDesignatedApprover = !"REQUESTER".equals(persona) && r.approverGroups() != null && r.approverGroups().stream().anyMatch(userGroups::contains);
                
                boolean isAuthorized = isAdmin || isDesignatedApprover;

                if (!isAuthorized) {
                    return Response.status(403).entity(Map.of("error", "You are not an authorized approver for this request")).build();
                }

                AccessRequest updated = new AccessRequest(
                    r.id(), r.requesterId(), r.userId(), r.principalType(), r.catalogName(), r.schemaName(), r.tableName(), 
                    r.resourceType(), r.privileges(), "REJECTED", r.createdAt(), System.currentTimeMillis(), 
                    r.justification(), rejection.reason, r.approverGroups(), r.metadata(), r.expirationTime()
                );
                accessRequestService.saveRequests(List.of(updated), userId, groups, isAdmin);
                broadcast("request-updated", List.of(updated));
                return Response.ok(updated).build();
            })
            .orElse(Response.status(404).entity(Map.of("error", "Request not found")).build());
    }

    @POST
    @Path("/{id}/verify")
    @Operation(summary = "Verify access request", description = "Verifies that an approved access request has been implemented in the target catalog")
    public Response verifyRequest(@PathParam("id") String id, @Context SecurityContext securityContext) {
        String userId = securityIdentity.getPrincipal() != null ? securityIdentity.getPrincipal().getName() : "anonymous";
        List<String> groups = new ArrayList<>(securityIdentity.getRoles());
        String persona = securityIdentity.getAttribute("persona");
        boolean isAdmin = !"REQUESTER".equals(persona) && (securityIdentity.hasRole("ADMIN") || securityIdentity.hasRole("SECURITY_ADMIN") || securityIdentity.hasRole("APPROVER") || securityIdentity.hasRole("GOVERNANCE_ADMIN") || securityIdentity.hasRole("AUDITOR") || groups.contains("admins"));

        return accessRequestService.getRequestById(id)
            .map(r -> {
                if (!"APPROVED".equals(r.status())) {
                    return Response.status(400).entity(Map.of("error", "Only approved requests can be verified")).build();
                }
                
                String principal = r.userId() != null ? r.userId() : r.requesterId();
                String path = "/" + r.catalogName() + "/" + r.schemaName() + "/" + r.tableName();
                boolean verified = catalogService.verifyPolicy(r.catalogName(), path, r.privileges().get(0), principal);
                
                if (verified) {
                    AccessRequest verifiedReq = new AccessRequest(
                        r.id(), r.requesterId(), r.userId(), r.principalType(), r.catalogName(), r.schemaName(), r.tableName(), 
                        r.resourceType(), r.privileges(), "VERIFIED", r.createdAt(), System.currentTimeMillis(), 
                        r.justification(), r.rejectionReason(), r.approverGroups(), r.metadata(), r.expirationTime()
                    );
                    accessRequestService.saveRequests(List.of(verifiedReq), userId, groups, isAdmin);
                    broadcast("request-verified", verifiedReq);
                    return Response.ok(new HalAccessRequest(verifiedReq)).build();
                } else {
                    return Response.status(409).entity(Map.of("error", "Drift detected: Permissions not yet implemented in target catalog")).build();
                }
            })
            .orElse(Response.status(404).build());
    }
}
