package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AuditEntry;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MockAuditServiceTest {

    @Test
    public void testAuditService() {
        MockAuditService service = new MockAuditService();
        AuditEntry entry = new AuditEntry("id1", "test", "actor", "u1", System.currentTimeMillis(), System.currentTimeMillis(), Map.of(), "sig", "signer");
        service.log(entry);
        assertFalse(service.getLogs().isEmpty());
    }
}
