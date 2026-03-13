package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MockServiceDeepTest {

    @Test
    public void testEdgeBranches() {
        MockAccessRequestService service = new MockAccessRequestService();
        List<String> emptyGroups = Collections.emptyList();
        List<String> adminGroups = List.of("admins");
        
        // 1. Partial update with nulls
        AccessRequest req = new AccessRequest("p", "u", "u", "c", "s", "t", List.of("S"), "PENDING", 0L, null, "j", null, null);
        service.saveRequests(List.of(req), "u", emptyGroups, false);
        
        AccessRequest update = new AccessRequest("p", null, null, null, null, null, null, "APPROVED", null, null, null, null, null);
        service.saveRequests(List.of(update), "admin", adminGroups, true);
        
        AccessRequest retrieved = service.getRequestById("p").orElseThrow();
        assertEquals("APPROVED", retrieved.status());
        assertEquals("u", retrieved.userId()); // userId should NOT have changed if update.userId was null

        // 2. Filter by owner only
        List<AccessRequest> owned = service.getAllRequests("u", emptyGroups, false);
        assertEquals(1, owned.size());
        
        // 3. Filter by group (admin sees all)
        List<AccessRequest> all = service.getAllRequests("other", adminGroups, true);
        assertEquals(1, all.size());
        
        // 4. Unauthorized update
        assertThrows(RuntimeException.class, () -> {
            service.saveRequests(List.of(update), "other", emptyGroups, false);
        });

        // 5. Delete/Save empty (covered by loop)
        service.saveRequests(Collections.emptyList(), "u", emptyGroups, false);
    }
}
