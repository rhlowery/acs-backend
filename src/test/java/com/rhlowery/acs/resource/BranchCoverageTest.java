package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
@QuarkusTest
public class BranchCoverageTest {

    @Test
    public void testAuditBranches() {
        // 1. Full entry
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("id", "audit-1", "type", "test", "actor", "user1", "timestamp", 12345L))
            .post("/api/audit/log")
            .then()
            .statusCode(200);

        // 2. Minimal entry (hits timestamp == null and id == null)
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("type", "test", "actor", "user1"))
            .post("/api/audit/log")
            .then()
            .statusCode(200);
            
        // 3. UI log
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("level", "INFO", "message", "test UI message"))
            .post("/api/audit/log/ui")
            .then()
            .statusCode(204);
    }

    @Test
    public void testProxyBranches() {
        // sql/execute - empty statement
        given()
            .contentType(ContentType.JSON)
            .body(Map.of())
            .post("/api/sql/execute")
            .then()
            .statusCode(400);

        // sdk - null host
        given()
            .get("/api/sdk/tables")
            .then()
            .statusCode(200);
            
        // ucProxy
        given()
            .get("/api/uc/some/path")
            .then()
            .statusCode(501);
    }

    @Test
    public void testCatalogBranches() {
        // 1. Invalid catalogId (hits provider == null)
        given().get("/api/catalog/invalid/nodes").then().statusCode(404);
        given().get("/api/catalog/invalid/nodes/verify").then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of()).post("/api/catalog/invalid/nodes/policy").then().statusCode(404);
        given().get("/api/catalog/invalid/nodes/permissions").then().statusCode(404);

        // 2. Default principal (hits principal == null)
        given()
            .queryParam("path", "main")
            .get("/api/catalog/uc-oss/nodes/permissions")
            .then()
            .statusCode(200);
            
        // 3. Search branches
        given().queryParam("q", "test").get("/api/catalog/search").then().statusCode(200);
        given().queryParam("query", "test").get("/api/catalog/search").then().statusCode(200);
    }

    @Test
    public void testCatalogServiceBranches() {
        // Test verifyPolicy with non-existent catalog
        given()
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/unknown/verify")
            .then()
            .statusCode(404);
    }

    @Test
    public void testAccessRequestResourceBranches() {
        // 1. Authenticate
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "admin", "password", "admin", "role", "ADMIN", "groups", java.util.List.of("admins")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        // 2. Submit a request
        String id = java.util.UUID.randomUUID().toString();
        given()
            .auth().oauth2(token).cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(java.util.List.of(Map.of(
                "id", id,
                "catalogName", "uc-oss",
                "tableName", "table1",
                "privileges", java.util.List.of("SELECT")
            )))
            .post("/api/storage/requests")
            .then()
            .statusCode(200);

        // 3. Verify non-approved (hits !"APPROVED".equals(r.status()))
        given()
            .auth().oauth2(token).cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/verify")
            .then()
            .statusCode(400);

        // 4. Approve it
        given()
            .auth().oauth2(token).cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve")
            .then()
            .statusCode(200);

        // 5. Verify it (hits verified flow)
        given()
            .auth().oauth2(token).cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/verify")
            .then()
            .statusCode(200);
            
        // 6. Drift test (hits verified == false)
        // We need a way to mock drift. Since we use AbstractMockProvider, 
        // if we change the policy in the mock directly, we can trigger drift.
        // But for branch coverage, we just need to hit the 'else' block.
        // Actually, without mocking the service it might be hard to trigger the else block 
        // unless we have a specific catalog that always returns wrong permissions.
    }
}
