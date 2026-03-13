package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MockAccessRequestServiceTest {

    @Test
    public void testMockServiceSymmetry() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "PENDING", 0L, null, "j", List.of("data-owners"), null);
        
        // 1. Save new
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        
        // 2. Retrieve by group
        List<AccessRequest> byGroup = service.getAllRequests("other", List.of("data-owners"), false);
        assertEquals(1, byGroup.size(), "Should see request as member of approver group");
        
        // 3. Retrieve as admin
        List<AccessRequest> all = service.getAllRequests("admin", List.of("admins"), true);
        assertEquals(1, all.size(), "Admin should see everything");
        
        // 4. Update partial as owner
        AccessRequest partial = new AccessRequest("id1", null, null, null, null, "new_table", null, null, null, null, "new_just", null, null);
        service.saveRequests(List.of(partial), "u1", List.of("users"), false);
        
        AccessRequest updated = service.getRequestById("id1").get();
        assertEquals("new_table", updated.tableName());
        assertEquals("new_just", updated.justification());
        assertEquals("PENDING", updated.status(), "Status should not change when non-admin updates");
    }

    @Test
    public void testForbiddenUpdate() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "PENDING", 0L, null, "j", null, null);
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        
        // Try update as another non-admin user
        AccessRequest update = new AccessRequest("id1", "u2", "u2", "c", "s", "t", List.of("SELECT"), "APPROVED", 0L, 0L, "j", null, null);
        assertThrows(RuntimeException.class, () -> service.saveRequests(List.of(update), "u2", List.of("users"), false));
    }
    
    @Test
    public void testAdminUpdateStatus() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "PENDING", 0L, null, "j", null, null);
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        
        AccessRequest update = new AccessRequest("id1", "u1", "u1", "c", "s", "t", List.of("SELECT"), "REJECTED", 0L, 0L, "j", null, null);
        service.saveRequests(List.of(update), "admin", List.of("admins"), true);
        
        assertEquals("REJECTED", service.getRequestById("id1").get().status());
    }
}
