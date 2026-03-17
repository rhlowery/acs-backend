package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockAccessRequestService implements AccessRequestService {
    private final Map<String, AccessRequest> storage = new ConcurrentHashMap<>();

    @Override
    public List<AccessRequest> getAllRequests(String userId, List<String> groups, boolean isAdmin) {
        if (isAdmin) {
            return new ArrayList<>(storage.values());
        }
        return storage.values().stream()
            .filter(r -> Objects.equals(r.requesterId(), userId) || 
                         Objects.equals(r.userId(), userId) || 
                         (r.approverGroups() != null && r.approverGroups().stream().anyMatch(groups::contains)))
            .toList();
    }

    @Override
    public void saveRequests(List<AccessRequest> requests, String userId, List<String> groups, boolean isAdmin) {
        for (AccessRequest req : requests) {
            AccessRequest existing = storage.get(req.id());
            if (existing == null) {
                AccessRequest newReq = new AccessRequest(
                    req.id(),
                    userId,
                    req.userId() != null ? req.userId() : userId,
                    req.principalType() != null ? req.principalType() : "USER",
                    req.catalogName(),
                    req.schemaName(),
                    req.tableName(),
                    req.resourceType() != null ? req.resourceType() : "TABLE",
                    req.privileges(),
                    isAdmin && req.status() != null ? req.status() : "PENDING",
                    System.currentTimeMillis(),
                    null,
                    req.justification(),
                    req.rejectionReason(),
                    req.approverGroups(),
                    req.metadata(),
                    req.expirationTime()
                );
                storage.put(req.id(), newReq);
            } else {
                boolean isOwner = Objects.equals(existing.requesterId(), userId) || Objects.equals(existing.userId(), userId);
                boolean isDesignatedApprover = existing.approverGroups() != null && groups != null && existing.approverGroups().stream().anyMatch(groups::contains);
                if (!isOwner && !isAdmin && !isDesignatedApprover) {
                    throw new RuntimeException("Forbidden: You do not have permission to update request " + req.id());
                }
                
                String status = (isAdmin || isDesignatedApprover) ? req.status() : existing.status();
                String requesterId = existing.requesterId();
                
                AccessRequest updated = new AccessRequest(
                    existing.id(),
                    requesterId,
                    req.userId() != null ? req.userId() : existing.userId(),
                    req.principalType() != null ? req.principalType() : existing.principalType(),
                    req.catalogName() != null ? req.catalogName() : existing.catalogName(),
                    req.schemaName() != null ? req.schemaName() : existing.schemaName(),
                    req.tableName() != null ? req.tableName() : existing.tableName(),
                    req.resourceType() != null ? req.resourceType() : existing.resourceType(),
                    req.privileges() != null ? req.privileges() : existing.privileges(),
                    status,
                    existing.createdAt(),
                    req.updatedAt() != null ? req.updatedAt() : System.currentTimeMillis(),
                    req.justification() != null ? req.justification() : existing.justification(),
                    req.rejectionReason() != null ? req.rejectionReason() : existing.rejectionReason(),
                    req.approverGroups() != null ? req.approverGroups() : existing.approverGroups(),
                    req.metadata() != null ? req.metadata() : existing.metadata(),
                    req.expirationTime() != null ? req.expirationTime() : existing.expirationTime()
                );
                storage.put(req.id(), updated);
            }
        }
    }

    @Override
    public Optional<AccessRequest> getRequestById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
