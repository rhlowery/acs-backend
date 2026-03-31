package com.rhlowery.acs;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.service.AuditService;
import com.rhlowery.acs.service.UserService;
import com.rhlowery.acs.resource.AuthResource;
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

    @Inject
    AuthResource authResource;

    @Inject
    jakarta.enterprise.inject.Instance<com.rhlowery.acs.service.IdentityProvider> providersInstance;

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
            .then().statusCode(401);

        // Logout
        given()
            .cookie("bff_jwt", "dummy")
            .post("/api/auth/logout")
            .then().statusCode(200);
            
        // Me endpoint without auth
        given()
            .get("/api/auth/me")
            .then().statusCode(401);
            
        // Config endpoint
        given()
            .get("/api/auth/config")
            .then().statusCode(200);
            
        // Personas endpoint with dummy auth
        given()
            .cookie("bff_jwt", "dummy")
            .get("/api/auth/personas")
            .then().statusCode(anyOf(is(200), is(401)));
    }

    @Test
    public void testAuditBranches() {
        // Log an entry
        com.rhlowery.acs.domain.AuditEntry entry = new com.rhlowery.acs.domain.AuditEntry(null, "TEST", "A", null, 0L, 0L, Map.of(), null, null);
        given()
            .contentType(ContentType.JSON)
            .body(entry)
            .post("/api/audit/log")
            .then().statusCode(200);

        // Get logs
        given()
            .get("/api/audit/log")
            .then().statusCode(200);

        // UI logs
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("level", "INFO", "message", "test"))
            .post("/api/audit/log/ui")
            .then().statusCode(204);

        // Get requests (Requires ADMIN/AUDITOR persona)
        given()
            .get("/api/storage/requests")
            .then().statusCode(200);
    }

    @Test
    public void testCatalogBranches() {
        given()
            .get("/api/catalog/providers")
            .then().statusCode(200);

        given()
            .queryParam("q", "test")
            .get("/api/catalog/search")
            .then().statusCode(200);

        given()
            .get("/api/catalog/databricks/nodes")
            .then().statusCode(200);

        given()
            .get("/api/catalog/invalid/nodes")
            .then().statusCode(404);

        given()
            .queryParam("path", "/")
            .get("/api/catalog/databricks/nodes/verify")
            .then().statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("path", "/a/b", "action", "READ", "principal", "p1"))
            .post("/api/catalog/databricks/nodes/policy")
            .then().statusCode(202);

        given()
            .queryParam("path", "/a/b")
            .get("/api/catalog/databricks/nodes/permissions")
            .then().statusCode(200);
    }

    @Test
    public void testMetastoreBranches() {
        given()
            .get("/api/metastores/databricks/children")
            .then().statusCode(200);
    }

    @Test
    public void testProxyBranches() {
        given()
            .get("/api/uc/some/path")
            .then().statusCode(501);
    }

    @Test
    public void testEntityDirectHit() {
        com.rhlowery.acs.infrastructure.entity.AuditEntryEntity e1 = new com.rhlowery.acs.infrastructure.entity.AuditEntryEntity();
        e1.id = "id"; e1.type = "T"; e1.actor = "A"; e1.userId = "U"; e1.timestamp = 0L; e1.serverTimestamp = 0L; e1.details = "{}"; e1.signature = "S"; e1.signer = "I";
        
        com.rhlowery.acs.infrastructure.entity.UserEntity u1 = new com.rhlowery.acs.infrastructure.entity.UserEntity();
        u1.id = "id"; u1.name = "N"; u1.email = "E"; u1.role = "R"; u1.groups = List.of(); u1.persona = "P";

        com.rhlowery.acs.infrastructure.entity.GroupEntity g1 = new com.rhlowery.acs.infrastructure.entity.GroupEntity();
        g1.id = "id"; g1.name = "N"; g1.description = "D"; g1.persona = "P";

        com.rhlowery.acs.infrastructure.entity.AccessRequestEntity ar = new com.rhlowery.acs.infrastructure.entity.AccessRequestEntity();
        ar.id = "id"; ar.requesterId = "r"; ar.userId = "u"; ar.principalType = "T"; ar.catalogName = "c"; ar.schemaName = "s"; ar.tableName = "t"; ar.resourceType = "R"; ar.privileges = List.of(); ar.status = "S"; ar.createdAt = 0L; ar.updatedAt = 0L; ar.justification = "j"; ar.rejectionReason = "r"; ar.approverGroups = List.of(); ar.expirationTime = 0L;
        
        new com.rhlowery.acs.domain.User("u", "n", "e", "r", List.of(), "p");
        new com.rhlowery.acs.domain.Group("g", "n", "d", "p");
        new com.rhlowery.acs.domain.CatalogNode("n", com.rhlowery.acs.domain.NodeType.TABLE, "p", "c", List.of(), "a", "d", "p");
        new com.rhlowery.acs.domain.AuditEntry("i", "t", "a", "u", 0L, 0L, Map.of(), "s", "i");
        new com.rhlowery.acs.domain.AccessRequest("i", "r", "u", "t", "c", "s", "t", "r", List.of(), "s", 0L, 0L, "j", "r", List.of(), Map.of(), 0L);
    }

    @Test
    public void testAccessRequestBranches() {
        String id1 = UUID.randomUUID().toString();
        AccessRequest req1 = new AccessRequest(id1, "alice", "alice", "USER", "cat", "sch", "tbl", "TABLE", 
            List.of("READ"), "PENDING", 0L, 0L, "J", null, List.of("group1"), Collections.emptyMap(), null);
        
        accessRequestService.saveRequests(List.of(req1), "alice", List.of(), false);
        assertTrue(accessRequestService.getAllRequests("alice", List.of(), false).size() > 0);
        assertTrue(accessRequestService.getAllRequests("bob", List.of("group1"), false).size() > 0);

        AccessRequest update = new AccessRequest(id1, "alice", "alice", "USER", "cat", "sch", "tbl", "TABLE", 
            List.of("READ"), "APPROVED", 0L, 0L, "J", null, List.of("group1"), Collections.emptyMap(), null);
        
        accessRequestService.saveRequests(List.of(update), "admin", List.of("admins"), true);
        
        AccessRequest retrieved = accessRequestService.getRequestById(id1).orElseThrow();
        assertEquals("APPROVED", retrieved.status());
    }

    @Test
    public void testUserServiceBranches() {
        userService.saveGroup(new com.rhlowery.acs.domain.Group("tg1", "N", "D", "P"));
        userService.saveGroup(new com.rhlowery.acs.domain.Group("tg1", "N2", "D2", "P2")); // Hit update branch
        userService.updateGroupPersona("tg1", "NEW_P");
        assertEquals("NEW_P", userService.getGroup("tg1").get().persona());
        
        userService.saveUser(new com.rhlowery.acs.domain.User("tu1", "N", "E", "R", List.of("tg1"), "P"));
        userService.saveUser(new com.rhlowery.acs.domain.User("tu1", "N2", "E2", "R2", List.of("tg1"), "P2")); // Hit update branch
        userService.updateUserGroups("tu1", List.of("tg2"));
        userService.updateUserPersona("tu1", "NEW_P");
        
        assertTrue(userService.listUsers().size() > 0);
        assertTrue(userService.listGroups().size() > 0);
        
        userService.clear();
        assertEquals(0, userService.listUsers().size());
    }

    @Test
    public void testAccessRequestForbidden() {
        String id = UUID.randomUUID().toString();
        AccessRequest req = new AccessRequest(id, "owner", "owner", "USER", "c", "s", "t", "T", List.of("R"), "PENDING", 0L, 0L, "J", null, List.of("g"), null, null);
        accessRequestService.saveRequests(List.of(req), "owner", List.of(), false);
        
        // Try update by other (non-admin, non-approver)
        assertThrows(RuntimeException.class, () -> accessRequestService.saveRequests(List.of(req), "stranger", List.of(), false));
        
        accessRequestService.clear();
    }

    @Test
    public void testMockIdentityProviderBranches() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "admin", "password", "wrong", "providerId", "mock"))
            .post("/api/auth/login")
            .then().statusCode(401);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "admin", "password", "admin", "providerId", "mock", "groups", List.of("new-group"), "persona", "NEW_MOD"))
            .post("/api/auth/login")
            .then().statusCode(200);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "new-unknown", "providerId", "mock"))
            .post("/api/auth/login")
            .then().statusCode(401);

        com.rhlowery.acs.service.impl.MockIdentityProvider idp = (com.rhlowery.acs.service.impl.MockIdentityProvider) providersInstance.stream()
            .filter(p -> "mock".equals(p.getId()))
            .findFirst()
            .get();
        idp.register("reg1", "p", "n", "P", List.of("g1"));
        assertTrue(idp.hasUser("reg1"));
        assertFalse(idp.hasUser("non-existent"));
        assertEquals(List.of("g1"), idp.getGroups("reg1"));
        assertEquals(List.of(), idp.getGroups("non-existent"));
    }

    @Test
    public void testCatalogPathBranches() {
        String[] cats = {"databricks", "polaris", "datahub", "gravitino", "atlan", "hive"};
        String[] paths = {"/", "/main", "/default", "/finance", "/sensitive", "/salaries", "/staged", "/model", "/compute"};
        
        for (String c : cats) {
            for (String p : paths) {
                given().queryParam("path", p).get("/api/catalog/" + c + "/nodes").then().statusCode(200);
            }
        }
    }

    @Inject
    com.rhlowery.acs.service.CatalogService catalogService;

    @Test
    public void testCatalogServiceExtraBranches() {
        // Drift detection
        catalogService.applyPolicy("databricks", "/drift", "READ", "alice");
        assertFalse(catalogService.verifyPolicy("databricks", "/drift", "WRITE", "alice"));
        
        // Non-existent catalog
        assertThrows(RuntimeException.class, () -> catalogService.getNodes("invalid", "/"));
        assertThrows(RuntimeException.class, () -> catalogService.getEffectivePermissions("invalid", "/", "p"));
        assertThrows(RuntimeException.class, () -> catalogService.applyPolicy("invalid", "/", "A", "P"));
        
        // Approver resolution (parent path search)
        // Set an approver at /finance
        catalogService.applyPolicy("databricks", "/finance", "APPROVE", "finance-admin"); 
        // Note: AbstractMockProvider.getNode has hardcoded approvers for some paths
        // /finance has finance-approvers
        List<String> approvers = catalogService.getRequiredApprovers("databricks", "/finance/sub/path");
        assertNotNull(approvers);
    }
}
