package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.infrastructure.entity.AccessRequestEntity;
import com.rhlowery.acs.service.AccessRequestService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DatabaseAccessRequestService implements AccessRequestService {
    private static final Logger LOG = Logger.getLogger(DatabaseAccessRequestService.class);

    @Override
    @Transactional
    public List<AccessRequest> getAllRequests(String userId, List<String> groups, boolean isAdmin) {
        LOG.debug("Listing all access requests for " + userId);
        List<AccessRequestEntity> entities;
        if (isAdmin) {
            entities = AccessRequestEntity.listAll();
        } else {
            // For now, let's use a stream approach to handle collection-based filtering correctly
            List<AccessRequestEntity> all = AccessRequestEntity.listAll();
            entities = all.stream()
                .filter(r -> r.requesterId.equals(userId) || 
                                 r.userId.equals(userId) || 
                                 (r.approverGroups != null && groups != null && r.approverGroups.stream().anyMatch(groups::contains)))
                .collect(Collectors.toList());
        }

        return entities.stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveRequests(List<AccessRequest> requests, String userId, List<String> groups, boolean isAdmin) {
        for (AccessRequest req : requests) {
            AccessRequestEntity entity = AccessRequestEntity.findById(req.id());
            if (entity == null) {
                LOG.info("Creating new access request: " + req.id());
                entity = new AccessRequestEntity(
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
                    req.expirationTime()
                );
                entity.persist();
            } else {
                LOG.info("Updating existing access request: " + req.id());
                // Logic check from Mock
                boolean isOwner = entity.requesterId.equals(userId) || entity.userId.equals(userId);
                boolean isDesignatedApprover = entity.approverGroups != null && groups != null && entity.approverGroups.stream().anyMatch(groups::contains);
                
                if (!isOwner && !isAdmin && !isDesignatedApprover) {
                    throw new RuntimeException("Forbidden: You do not have permission to update request " + req.id());
                }
                
                if (isAdmin || isDesignatedApprover) {
                    LOG.info("Updating status to: " + req.status());
                    entity.status = req.status();
                }

                entity.userId = req.userId() != null ? req.userId() : entity.userId;
                entity.principalType = req.principalType() != null ? req.principalType() : entity.principalType;
                entity.catalogName = req.catalogName() != null ? req.catalogName() : entity.catalogName;
                entity.schemaName = req.schemaName() != null ? req.schemaName() : entity.schemaName;
                entity.tableName = req.tableName() != null ? req.tableName() : entity.tableName;
                entity.resourceType = req.resourceType() != null ? req.resourceType() : entity.resourceType;
                entity.privileges = req.privileges() != null ? req.privileges() : entity.privileges;
                entity.updatedAt = System.currentTimeMillis();
                entity.justification = req.justification() != null ? req.justification() : entity.justification;
                entity.rejectionReason = req.rejectionReason() != null ? req.rejectionReason() : entity.rejectionReason;
                entity.approverGroups = req.approverGroups() != null ? req.approverGroups() : entity.approverGroups;
                entity.expirationTime = req.expirationTime() != null ? req.expirationTime() : entity.expirationTime;
            }
        }
        AccessRequestEntity.getEntityManager().flush();
    }


    @Override
    public Optional<AccessRequest> getRequestById(String id) {
        AccessRequestEntity entity = AccessRequestEntity.findById(id);
        return Optional.ofNullable(entity).map(this::mapToDomain);
    }

    @Override
    @Transactional
    public void clear() {
        AccessRequestEntity.deleteAll();
        AccessRequestEntity.getEntityManager().flush();
    }

    private AccessRequest mapToDomain(AccessRequestEntity entity) {
        return new AccessRequest(
            entity.id,
            entity.requesterId,
            entity.userId,
            entity.principalType,
            entity.catalogName,
            entity.schemaName,
            entity.tableName,
            entity.resourceType,
            entity.privileges != null ? new java.util.ArrayList<>(entity.privileges) : java.util.List.of(),
            entity.status,
            entity.createdAt,
            entity.updatedAt,
            entity.justification,
            entity.rejectionReason,
            entity.approverGroups != null ? new java.util.ArrayList<>(entity.approverGroups) : java.util.List.of(),
            null, // metadata is mapped to individual fields or could be a map if needed
            entity.expirationTime
        );
    }
}
