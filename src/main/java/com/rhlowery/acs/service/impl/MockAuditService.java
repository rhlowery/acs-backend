package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AuditEntry;
import com.rhlowery.acs.service.AuditService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MockAuditService implements AuditService {
    private final List<AuditEntry> logs = new CopyOnWriteArrayList<>();

    @Override
    public void log(AuditEntry entry) {
        logs.add(0, entry);
        if (logs.size() > 5000) {
            logs.remove(logs.size() - 1);
        }
    }

    @Override
    public List<AuditEntry> getLogs() {
        return new ArrayList<>(logs);
    }
}
