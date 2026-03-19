package com.rhlowery.acs.service;

import com.rhlowery.acs.domain.AuditEntry;
import java.util.List;

public interface AuditService {
    void log(AuditEntry entry);
    List<AuditEntry> getLogs();
    io.smallrye.mutiny.Multi<AuditEntry> streamLogs();
}
