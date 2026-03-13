package com.rhlowery.acs.infrastructure;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.eclipse.microprofile.health.HealthCheckResponse;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HealthCheckTest {

    @Inject
    @org.eclipse.microprofile.health.Liveness
    ServiceLivenessCheck liveness;

    @Inject
    @org.eclipse.microprofile.health.Readiness
    ServiceReadinessCheck readiness;

    @Test
    public void testHealth() {
        assertEquals(HealthCheckResponse.Status.UP, liveness.call().getStatus());
        assertEquals(HealthCheckResponse.Status.UP, readiness.call().getStatus());
    }
}
