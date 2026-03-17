package com.rhlowery.acs.infrastructure;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class LineageServiceTest {

    @Inject
    LineageService lineageService;

    @Test
    public void testLineage() {
        com.rhlowery.acs.domain.AccessRequest req = new com.rhlowery.acs.domain.AccessRequest(
            "id", "u1", "u1", "USER", "c", "s", "t", "TABLE", java.util.List.of("SELECT"), "PENDING", 0L, null, "j", null, null, null, null);
        lineageService.emitAccessRequestEvent(req, "SUBMITTED");
        lineageService.emitAccessRequestEvent(req, "APPROVED");
        lineageService.emitAccessRequestEvent(req, "REJECTED");
        lineageService.emitAccessRequestEvent(req, "OTHER");
    }
}
