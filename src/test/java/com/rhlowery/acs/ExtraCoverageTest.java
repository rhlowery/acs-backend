package com.rhlowery.acs;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.service.AuditService;
import com.rhlowery.acs.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ExtraCoverageTest {

    @Inject
    AccessRequestService accessRequestService;

    @Inject
    AuditService auditService;

    @Inject
    UserService userService;

    @Test
    public void testCreateEmptyRequests() {
        given()
            .contentType(ContentType.JSON)
            .body(Collections.emptyList())
            .post("/api/storage/requests")
            .then()
            .statusCode(400)
            .body("error", equalTo("Request list cannot be empty"));
    }

    @Test
    public void testRejectWithoutReason() {
        String id = UUID.randomUUID().toString();
        given()
            .contentType(ContentType.JSON)
            .body(Collections.emptyMap())
            .post("/api/storage/requests/" + id + "/reject")
            .then()
            .statusCode(400)
            .body("error", equalTo("Rejection reason is mandatory"));
    }

    @Test
    public void testUpdateNonExistentUserGroups() {
        given()
            .contentType(ContentType.JSON)
            .body(List.of("group1"))
            .patch("/api/users/non-existent/groups")
            .then()
            .statusCode(404);
    }

    @Test
    public void testAuthBranches() {
        // Invalid provider
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "user", "providerId", "unknown"))
            .post("/api/auth/login")
            .then().statusCode(400);

        // Missing userId
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("providerId", "oidc"))
            .post("/api/auth/login")
            .then().statusCode(400);

        // Logout
        given()
            .cookie("bff_jwt", "dummy")
            .post("/api/auth/logout")
            .then().statusCode(200); // FIXED EXPECTATION
            
        // Me endpoint without auth
        given()
            .get("/api/auth/me")
            .then().statusCode(401);
    }

    @Test
    public void testAccessRequestBranches() {
        String id1 = UUID.randomUUID().toString();
        AccessRequest req1 = new AccessRequest(id1, "alice", "alice", "USER", "cat", "sch", "tbl", "TABLE", 
            List.of("READ"), "PENDING", 0L, 0L, "J", null, List.of("group1"), Collections.emptyMap(), null);
        
        // Use the service directly to save initially (avoids API overriding approverGroups)
        accessRequestService.saveRequests(List.of(req1), "alice", List.of(), false);

        // Filter: requester match (via service)
        assertTrue(accessRequestService.getAllRequests("alice", List.of(), false).size() > 0);
        // Filter: group match (via service)
        assertTrue(accessRequestService.getAllRequests("bob", List.of("group1"), false).size() > 0);


        // Update permissions check: Use the Service Directly for the "ADMIN" branch
        // We want to force it to APPROVED
        AccessRequest update = new AccessRequest(id1, "alice", "alice", "USER", "cat", "sch", "tbl", "TABLE", 
            List.of("READ"), "APPROVED", 0L, 0L, "J", null, List.of("group1"), Collections.emptyMap(), null);
        
        accessRequestService.saveRequests(List.of(update), "admin", List.of("admins"), true);
        
        AccessRequest retrieved = accessRequestService.getRequestById(id1).orElseThrow();
        assertEquals("APPROVED", retrieved.status(), "Status should have been updated to APPROVED by admin");
    }

    @Test
    public void testUserServiceBranches() {
        userService.saveGroup(new com.rhlowery.acs.domain.Group("tg1", "N", "D", "P"));
        userService.updateGroupPersona("tg1", "NEW_P");
        assertEquals("NEW_P", userService.getGroup("tg1").get().persona());
        
        userService.saveUser(new com.rhlowery.acs.domain.User("tu1", "N", "E", "R", List.of("tg1"), "P"));
        userService.updateUserGroups("tu1", List.of("tg2"));
        userService.updateUserPersona("tu1", "NEW_P");
    }
}
