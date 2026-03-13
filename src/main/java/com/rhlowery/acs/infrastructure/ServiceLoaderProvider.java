package com.rhlowery.acs.infrastructure;

import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.service.AuditService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.ServiceLoader;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ServiceLoaderProvider {

    private static final Logger LOG = Logger.getLogger(ServiceLoaderProvider.class);

    @Produces
    @ApplicationScoped
    public AccessRequestService produceAccessRequestService() {
        LOG.info("Loading AccessRequestService via ServiceLoader");
        return ServiceLoader.load(AccessRequestService.class)
            .findFirst()
            .orElseThrow(() -> {
                LOG.error("No implementation found for AccessRequestService");
                return new RuntimeException("No implementation found for AccessRequestService");
            });
    }

    @Produces
    @ApplicationScoped
    public AuditService produceAuditService() {
        LOG.info("Loading AuditService via ServiceLoader");
        return ServiceLoader.load(AuditService.class)
            .findFirst()
            .orElseThrow(() -> {
                LOG.error("No implementation found for AuditService");
                return new RuntimeException("No implementation found for AuditService");
            });
    }
}
