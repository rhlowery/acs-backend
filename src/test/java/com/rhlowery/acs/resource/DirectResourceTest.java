package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DirectResourceTest {

    @Inject
    AccessRequestResource accessRequestResource;

    @Test
    public void testDirectCall() {
        assertNotNull(accessRequestResource);
        // This will at least cover the injection and class loading
    }
}
