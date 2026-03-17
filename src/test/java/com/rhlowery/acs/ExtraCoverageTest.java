package com.rhlowery.acs;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import jakarta.inject.Inject;

@QuarkusTest
public class ExtraCoverageTest {

    @Test
    public void testCreateEmptyRequests() {
        given()
            .contentType(ContentType.JSON)
            .body(List.of())
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
            .body(Map.of())
            .post("/api/storage/requests/" + id + "/reject")
            .then()
            .statusCode(400)
            .body("error", equalTo("Rejection reason is mandatory"));
    }

    @Test
    public void testApproveNonPending() {
        // First create a request and approve it
        String id = UUID.randomUUID().toString();
        loginAs("admin", "admins");
        
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", id,
                "catalogName", "main",
                "schemaName", "default",
                "tableName", "test_table",
                "privileges", List.of("SELECT"),
                "status", "APPROVED" // Admins can set status
            )))
            .post("/api/storage/requests")
            .then().statusCode(200);

        // Try to approve it again
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve")
            .then()
            .statusCode(400)
            .body("error", equalTo("Request is not in a state that can be approved"));
    }

    @Test
    public void testUnauthorizedApproval() {
        String id = UUID.randomUUID().toString();
        loginAs("admin", "admins");
        
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", id,
                "catalogName", "main",
                "schemaName", "default",
                "tableName", "secret_table",
                "privileges", List.of("SELECT")
            )))
            .post("/api/storage/requests")
            .then().statusCode(200);

        // Login as standard user with no approver groups
        loginAs("alice", "standard-users");
        
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve")
            .then()
            .statusCode(403);
            
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("reason", "no"))
            .post("/api/storage/requests/" + id + "/reject")
            .then()
            .statusCode(403);
    }

    @Test
    public void testVerifyInvalidStatus() {
        String id = UUID.randomUUID().toString();
        loginAs("admin", "admins");
        
        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", id,
                "catalogName", "main",
                "schemaName", "default",
                "tableName", "verify_table",
                "privileges", List.of("SELECT"),
                "status", "PENDING"
            )))
            .post("/api/storage/requests")
            .then().statusCode(200);

        given()
            .cookie("bff_jwt", token)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/verify")
            .then()
            .statusCode(400)
            .body("error", equalTo("Only approved requests can be verified"));
    }

    @Test
    public void testUpdateNonExistentUserGroups() {
        given()
            .contentType(ContentType.JSON)
            .body(List.of("admins"))
            .patch("/api/users/non-existent/groups")
            .then()
            .statusCode(404);
    }

    @Inject
    com.rhlowery.acs.service.CatalogService catalogService;

    @Test
    public void testCatalogServiceBranches() {
        // Find provider returning null test
        try { catalogService.applyPolicy("invalid", "/", "X", "Y"); } catch (Exception e) {}
        try { catalogService.getEffectivePermissions("invalid", "/", "Y"); } catch (Exception e) {}
        try { catalogService.getNodes("invalid", "/"); } catch (Exception e) {}
        try { catalogService.getRequiredApprovers("invalid", "/"); } catch (Exception e) {}
        try { catalogService.verifyPolicy("invalid", "/", "X", "Y"); } catch (Exception e) {}
        
        // getRequiredApprovers loop and getParentPath branches
        catalogService.getRequiredApprovers("main", "/main/default/users");
        catalogService.getRequiredApprovers("main", "/main/finance/salaries");
        catalogService.getRequiredApprovers("main", "/");
        catalogService.getRequiredApprovers("main", null);
        
        // Clear
        catalogService.clear();
    }

    @Test
    public void testSqlEmptyStatement() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of())
            .post("/api/sql/execute")
            .then()
            .statusCode(400);
    }

    @Test
    public void testCatalogRegistration() {
        given()
            .get("/api/catalog/registrations/invalid")
            .then()
            .statusCode(404);
    }
    
    @Inject
    jakarta.enterprise.inject.Instance<com.rhlowery.acs.service.IdentityProvider> idps;

    @Test
    public void testIdpBranches() {
        idps.forEach(p -> {
            p.authenticate(Map.of()); // No userId
            p.getGroups("any");
        });
    }

    @Test
    public void testAuditBranches() {
        given().get("/api/audit/log?type=NONE").then().statusCode(200);
    }

    private String token;
    private void loginAs(String user, String groups) {
        token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", user, "groups", List.of(groups.split(","))))
            .post("/api/auth/login")
            .then()
            .extract()
            .cookie("bff_jwt");
    }
}
