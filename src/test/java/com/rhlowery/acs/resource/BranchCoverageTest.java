package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class BranchCoverageTest {

    @Test
    public void testAuditBranches() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("type", "test", "actor", "user1"))
            .post("/api/audit/log")
            .then()
            .statusCode(200);
    }

    @Test
    public void testProxyBranches() {
        // sql/execute
        given()
            .contentType(ContentType.JSON)
            .body(Map.of())
            .post("/api/sql/execute")
            .then()
            .statusCode(400);

        // sdk
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
}
