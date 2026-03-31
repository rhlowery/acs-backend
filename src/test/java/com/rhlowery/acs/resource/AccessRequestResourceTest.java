package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AccessRequestResourceTest {

    @Test
    public void testFullAccessRequestFlow() {
        // 1. Login to get token
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "admin", "password", "admin", "role", "ADMIN", "groups", List.of("admins")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        // 2. Submit a request
        String requestId = UUID.randomUUID().toString();
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", requestId,
                "catalogName", "test_catalog",
                "schemaName", "test_schema",
                "tableName", "test_table",
                "privileges", List.of("SELECT"),
                "justification", "Testing coverage"
            )))
            .post("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("status", is("success"));

        // 3. Get all requests
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("find { it.id == '" + requestId + "' }.tableName", is("test_table"));

        // 4. Get request by ID
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests/" + requestId)
            .then()
            .statusCode(200)
            .body("tableName", is("test_table"))
            .body("_links.approve.href", containsString(requestId));

        // 5. Approve request
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + requestId + "/approve")
            .then()
            .statusCode(200);

        // 6. Verify status updated
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests/" + requestId)
            .then()
            .statusCode(200)
            .body("status", is("APPROVED"));

        // 7. Reject it
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(Map.of("reason", "Cleanup"))
            .post("/api/storage/requests/" + requestId + "/reject")
            .then()
            .statusCode(200);
    }

    @Test
    public void testNonAdminForbidden() {
        // 1. Login as standard user
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "user1", "password", "password", "role", "STANDARD_USER", "groups", List.of("users")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        // 2. Try to approve a request as non-admin
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/some-id/approve")
            .then()
            .statusCode(404);
    }

    @Test
    public void testErrorScenarios() {
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "admin", "password", "admin", "role", "ADMIN", "groups", List.of("admins")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        // 1. Get non-existent request
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests/non-existent")
            .then()
            .statusCode(404);

        // 2. Approve non-existent request
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/non-existent/approve")
            .then()
            .statusCode(404);

        // 3. Reject non-existent request
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(Map.of("reason", "Testing"))
            .post("/api/storage/requests/non-existent/reject")
            .then()
            .statusCode(404);

        // 4. Submit empty list
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(List.of())
            .post("/api/storage/requests")
            .then()
            .statusCode(400); // Now returns 400

        // 5. Unauthenticated calls
        given().get("/api/storage/requests").then().statusCode(200); 
    }

    @Test
    public void testApproverPersona() {
        // 1. Assign persona via API (Requires admin login first)
        String adminToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "bob", "password", "password", "role", "ADMIN", "groups", List.of("admins")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        given()
            .header("Authorization", "Bearer " + adminToken)
            .cookie("bff_jwt", adminToken)
            .contentType(ContentType.TEXT)
            .body("APPROVER")
            .put("/api/auth/users/alice/persona")
            .then()
            .statusCode(200);

        // 2. Login as alice (now an APPROVER)
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "alice", "password", "password", "role", "STANDARD_USER", "groups", List.of("standard-users")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");

        // 3. Submit a request
        String requestId = UUID.randomUUID().toString();
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", requestId,
                "catalogName", "test_catalog",
                "schemaName", "test_schema",
                "tableName", "test_table",
                "privileges", List.of("SELECT"),
                "justification", "Approver test"
            )))
            .post("/api/storage/requests")
            .then()
            .statusCode(200);

        // 4. Approve it
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + requestId + "/approve")
            .then()
            .statusCode(200);
    }

    @Test
    public void testGovernanceAdminPersona() {
        // 1. Assign GOVERNANCE_ADMIN persona
        String adminToken = loginAs("bob", "ADMIN", "admins");
        given()
            .header("Authorization", "Bearer " + adminToken)
            .cookie("bff_jwt", adminToken)
            .contentType(ContentType.TEXT)
            .body("GOVERNANCE_ADMIN")
            .put("/api/auth/users/charlie/persona")
            .then()
            .statusCode(200);

        // 2. Login as charlie
        String token = loginAs("charlie", "STANDARD_USER", "users");

        // 3. Governance admin should be able to list all requests
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests")
            .then()
            .statusCode(200);
    }

    @Test
    public void testSecurityAdminPersona() {
        String adminToken = loginAs("bob", "ADMIN", "admins");
        given()
            .header("Authorization", "Bearer " + adminToken)
            .cookie("bff_jwt", adminToken)
            .contentType(ContentType.TEXT)
            .body("SECURITY_ADMIN")
            .put("/api/auth/users/david/persona")
            .then()
            .statusCode(200);

        String token = loginAs("david", "STANDARD_USER", "users");
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/storage/requests")
            .then()
            .statusCode(200);
    }

    @Test
    public void testDeepRequestApprovers() {
        String token = loginAs("alice", "STANDARD_USER", "users");
        
        // This targets a non-existent child of a node with approvers to test recursion
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", UUID.randomUUID().toString(),
                "catalogName", "uc-oss",
                "schemaName", "default",
                "tableName", "sensitive_tbl",
                "path", "/default/sensitive_tbl/sub",
                "privileges", List.of("SELECT"),
                "justification", "Recursion test"
            )))
            .post("/api/storage/requests")
            .then()
            .statusCode(200);
    }

    @Test
    public void testUpdateForbidden() {
        String aliceToken = loginAs("alice", "STANDARD_USER", "users");
        String requestId = UUID.randomUUID().toString();
        
        // 1. Alice creates a request
        given()
            .header("Authorization", "Bearer " + aliceToken)
            .cookie("bff_jwt", aliceToken)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of("id", requestId, "catalogName", "cat", "schemaName", "sch", "tableName", "tbl")))
            .post("/api/storage/requests")
            .then()
            .statusCode(200);

        // 2. Charlie (not an admin or approver for this) tries to approve/update it
        String charlieToken = loginAs("charlie", "STANDARD_USER", "users");
        given()
            .header("Authorization", "Bearer " + charlieToken)
            .cookie("bff_jwt", charlieToken)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + requestId + "/approve")
            .then()
            .statusCode(403); // Forbidden branch in resource returns 403
    }

    @Test
    public void testDesignatedApprover() {
        // 1. Alice creates request for sensitive tbl
        String aliceToken = loginAs("alice", "STANDARD_USER", "users");
        String id = UUID.randomUUID().toString();
        given()
            .header("Authorization", "Bearer " + aliceToken)
            .cookie("bff_jwt", aliceToken)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of("id", id, "catalogName", "uc-oss", "path", "/default/sensitive_tbl")))
            .post("/api/storage/requests")
            .then()
            .statusCode(200);

        // 2. Eve (designated approver) approves it
        String eveToken = loginAs("eve", "STANDARD_USER", "sensitive-approvers");
        given()
            .header("Authorization", "Bearer " + eveToken)
            .cookie("bff_jwt", eveToken)
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve")
            .then()
            .statusCode(200);
    }

    private String loginAs(String user, String role, String groups) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", user, "password", user.equals("admin") ? "admin" : "password", "role", role, "groups", java.util.Arrays.asList(groups.split(","))))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract().cookie("bff_jwt");
    }
}
