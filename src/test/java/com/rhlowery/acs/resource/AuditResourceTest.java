package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AuditResourceTest {

    @Test
    public void testAuditLogging() {
        // 1. Post audit log
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "type", "ACCESS_GRANTED",
                "actor", "admin",
                "details", Map.of("resource", "table1")
            ))
            .post("/api/audit/log")
            .then()
            .statusCode(200)
            .body("status", is("success"));

        // 2. Get audit logs
        given()
            .get("/api/audit/log")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("[0].type", notNullValue());
            
        // 3. Post audit UI log
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("level", "INFO", "message", "test-ui"))
            .post("/api/audit/log/ui")
            .then()
            .statusCode(204);
    }

    @Test
    public void testAuditLogCleanup() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("type", "TEST"))
            .post("/api/audit/log")
            .then()
            .statusCode(200);
    }
}
