package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MockAccessRequestServiceTest {

    @Test
    public void testMockService() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "PENDING", 0L, null, "j", null, null);
        
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        assertEquals(1, service.getAllRequests("u1", List.of("users"), false).size());
        
        AccessRequest update = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "APPROVED", 0L, 0L, "j", null, null);
        service.saveRequests(List.of(update), "admin", List.of("admins"), true);
        
        assertTrue(service.getRequestById("id1").isPresent());
        assertEquals("APPROVED", service.getRequestById("id1").get().status());
    }

    @Test
    public void testForbiddenUpdate() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "PENDING", 0L, null, "j", null, null);
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        
        AccessRequest update = new AccessRequest("id1", "u2", "u2", "c", "s", "t", List.of("SELECT"), "APPROVED", 0L, 0L, "j", null, null);
        assertThrows(RuntimeException.class, () -> service.saveRequests(List.of(update), "u2", List.of("users"), false));
    }
}
