package com.rhlowery.acs.infrastructure;

import com.rhlowery.acs.service.AccessRequestService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ServiceLoaderProviderTest {

    @Test
    public void testProvideAccessRequestService() {
        ServiceLoaderProvider provider = new ServiceLoaderProvider();
        AccessRequestService service = provider.produceAccessRequestService();
        assertNotNull(service);
    }

    @Test
    void testProduceAuditService() {
        ServiceLoaderProvider provider = new ServiceLoaderProvider();
        assertNotNull(provider.produceAuditService());
    }
}
