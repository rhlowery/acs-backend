package com.rhlowery.acs.infrastructure;

import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.service.AuditService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.ServiceLoader;

@ApplicationScoped
public class ServiceLoaderProvider {

    @Produces
    @ApplicationScoped
    public AccessRequestService produceAccessRequestService() {
        return ServiceLoader.load(AccessRequestService.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No implementation found for AccessRequestService"));
    }

    @Produces
    @ApplicationScoped
    public AuditService produceAuditService() {
        return ServiceLoader.load(AuditService.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No implementation found for AuditService"));
    }
}
