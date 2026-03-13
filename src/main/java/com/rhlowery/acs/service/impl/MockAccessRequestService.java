package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

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
                boolean isOwner = Objects.equals(existing.requesterId(), userId) || Objects.equals(existing.userId(), userId);
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
                    req.updatedAt() != null ? req.updatedAt() : System.currentTimeMillis(),
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
