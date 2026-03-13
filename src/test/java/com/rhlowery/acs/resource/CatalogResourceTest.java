package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class CatalogResourceTest {

    @Test
    public void testCatalogEndpoints() {
        // 1. Search
        given()
            .queryParam("query", "test")
            .get("/api/catalog/search")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);

        // 2. Nodes (with 404 since no provider id matches)
        given()
            .queryParam("catalogId", "invalid")
            .get("/api/catalog/nodes")
            .then()
            .statusCode(404);

        // 3. Verify node
        given()
            .queryParam("path", "main.default.table1")
            .get("/api/catalog/nodes/verify")
            .then()
            .statusCode(200);

        // 4. Apply policy
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("path", "main.default.table1", "action", "ALLOW", "principal", "user1"))
            .post("/api/catalog/nodes/policy")
            .then()
            .statusCode(202);

        // 5. Get permissions
        given()
            .queryParam("path", "main.default.table1")
            .queryParam("principal", "user1")
            .get("/api/catalog/nodes/permissions")
            .then()
            .statusCode(200)
            .body("effective", notNullValue());
            
        // 6. List providers
        given()
            .get("/api/catalog/providers")
            .then()
            .statusCode(200)
            .body(containsString("UnityCatalogNodeProvider"));
    }
}
