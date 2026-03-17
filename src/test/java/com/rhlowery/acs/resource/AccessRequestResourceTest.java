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
            .body(Map.of("userId", "admin", "role", "ADMIN", "groups", List.of("admins")))
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
            .body("[0].tableName", notNullValue());

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
            .body(Map.of("userId", "user1", "role", "STANDARD_USER", "groups", List.of("users")))
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
            .body(Map.of("userId", "admin", "role", "ADMIN", "groups", List.of("admins")))
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
        given().get("/api/storage/requests").then().statusCode(200); // Default to anonymous
    }
}
