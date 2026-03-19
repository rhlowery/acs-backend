package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AuditEntry;
import com.rhlowery.acs.service.AuditService;
import java.util.ArrayList;
import java.util.List;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockAuditService implements AuditService {
    private final List<AuditEntry> logs = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor<AuditEntry> processor = io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor.create();

    @Override
    public void log(AuditEntry entry) {
        logs.add(0, entry);
        if (logs.size() > 5000) {
            logs.remove(logs.size() - 1);
        }
        processor.onNext(entry);
    }

    @Override
    public List<AuditEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    @Override
    public io.smallrye.mutiny.Multi<AuditEntry> streamLogs() {
        return processor;
    }
}
