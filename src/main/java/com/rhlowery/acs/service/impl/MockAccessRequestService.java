package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockAccessRequestService implements AccessRequestService {
    private final Map<String, AccessRequest> storage = new ConcurrentHashMap<>();

    @Override
    public List<AccessRequest> getAllRequests(String userId, List<String> groups, boolean isAdmin) {
        if (isAdmin) {
            return new ArrayList<>(storage.values());
        }
        return storage.values().stream()
            .filter(r -> r.requesterId().equals(userId) || 
                         r.userId().equals(userId) || 
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
                    req.userId(),
                    req.catalogName(),
                    req.schemaName(),
                    req.tableName(),
                    req.privileges(),
                    "PENDING",
                    System.currentTimeMillis(),
                    null,
                    req.justification(),
                    req.approverGroups(),
                    req.metadata()
                );
                storage.put(req.id(), newReq);
            } else {
                boolean isOwner = existing.requesterId().equals(userId) || existing.userId().equals(userId);
                if (!isOwner && !isAdmin) {
                    throw new RuntimeException("Forbidden: You do not have permission to update request " + req.id());
                }
                
                String status = isAdmin ? req.status() : existing.status();
                String requesterId = existing.requesterId();
                
                AccessRequest updated = new AccessRequest(
                    existing.id(),
                    requesterId,
                    req.userId() != null ? req.userId() : existing.userId(),
                    req.catalogName() != null ? req.catalogName() : existing.catalogName(),
                    req.schemaName() != null ? req.schemaName() : existing.schemaName(),
                    req.tableName() != null ? req.tableName() : existing.tableName(),
                    req.privileges() != null ? req.privileges() : existing.privileges(),
                    status,
                    existing.createdAt(),
                    System.currentTimeMillis(),
                    req.justification() != null ? req.justification() : existing.justification(),
                    req.approverGroups() != null ? req.approverGroups() : existing.approverGroups(),
                    req.metadata() != null ? req.metadata() : existing.metadata()
                );
                storage.put(req.id(), updated);
            }
        }
    }

    @Override
    public Optional<AccessRequest> getRequestById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
