package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.AccessRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PlainMockServiceTest {

    @Test
    public void testPlainService() {
        MockAccessRequestService service = new MockAccessRequestService();
        AccessRequest req = new AccessRequest("p1", "u1", "u1", "USER", "c", "s", "t", "TABLE", List.of("SELECT"), "PENDING", 0L, null, "j", null, null, null, null);
        service.saveRequests(List.of(req), "u1", List.of("users"), false);
        
        assertEquals(1, service.getAllRequests("u1", List.of("users"), false).size());
    }
}
