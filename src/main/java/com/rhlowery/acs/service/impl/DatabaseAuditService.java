package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AuditEntry;
import com.rhlowery.acs.infrastructure.entity.AuditEntryEntity;
import com.rhlowery.acs.service.AuditService;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DatabaseAuditService implements AuditService {
    private static final Logger LOG = Logger.getLogger(DatabaseAuditService.class);
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private final io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor<AuditEntry> processor = io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor.create();

    @Override
    @jakarta.transaction.Transactional
    public void log(AuditEntry entry) {
        LOG.debug("Logging audit entry: " + entry.type() + " by " + entry.actor());
        String detailsStr = "";
        try {
            detailsStr = mapper.writeValueAsString(entry.details());
        } catch (Exception e) {}
        
        AuditEntryEntity entity = new AuditEntryEntity(
            entry.id(), entry.type(), entry.actor(), entry.userId(), 
            entry.timestamp(), entry.serverTimestamp(), detailsStr, 
            entry.signature(), entry.signer()
        );
        entity.persist();
        processor.onNext(entry); // Broadcast to active streams
    }

    @Override
    public List<AuditEntry> getLogs() {
        return AuditEntryEntity.<AuditEntryEntity>listAll().stream().map(this::mapToDomain).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public io.smallrye.mutiny.Multi<AuditEntry> streamLogs() {
        return processor;
    }

    private AuditEntry mapToDomain(AuditEntryEntity entity) {
        java.util.Map<String, Object> details = java.util.Map.of();
        try {
            details = mapper.readValue(entity.details, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        } catch (Exception e) {}
        
        return new AuditEntry(
            entity.id, entity.type, entity.actor, entity.userId, 
            entity.timestamp, entity.serverTimestamp, details, 
            entity.signature, entity.signer
        );
    }
}

