package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class CatalogRegistrationResourceTest {

    @Test
    public void testRegistrationLifecycle() {
        Map<String, Object> reg = Map.of(
            "id", "new-cat",
            "name", "New Catalog",
            "settings", Map.of("host", "localhost")
        );

        // 1. Create
        given()
            .contentType(ContentType.JSON)
            .body(reg)
            .post("/api/catalog/registrations")
            .then()
            .statusCode(201)
            .body("id", is("new-cat"));

        // 2. List
        given()
            .get("/api/catalog/registrations")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1));

        // 3. Get
        given()
            .get("/api/catalog/registrations/new-cat")
            .then()
            .statusCode(200)
            .body("name", is("New Catalog"));

        // 4. Update
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Updated Name", "settings", Map.of("port", 8080)))
            .patch("/api/catalog/registrations/new-cat")
            .then()
            .statusCode(200)
            .body("name", is("Updated Name"))
            .body("settings.port", is(8080))
            .body("settings.host", is("localhost"));

        // 5. Delete
        given()
            .delete("/api/catalog/registrations/new-cat")
            .then()
            .statusCode(204);

        // 6. Get not found
        given()
            .get("/api/catalog/registrations/new-cat")
            .then()
            .statusCode(404);
    }

    @Test
    public void testRegistrationErrors() {
        // Missing ID
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Broken"))
            .post("/api/catalog/registrations")
            .then()
            .statusCode(400);

        // Patch non-existent
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "No"))
            .patch("/api/catalog/registrations/no-id")
            .then()
            .statusCode(404);

        // Delete non-existent
        given()
            .delete("/api/catalog/registrations/no-id")
            .then()
            .statusCode(404);
            
        // Patch with non-map settings
        // First create one
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("id", "bad-cat", "settings", "NOT_A_MAP"))
            .post("/api/catalog/registrations")
            .then()
            .statusCode(201);
            
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("settings", Map.of("foo", "bar")))
            .patch("/api/catalog/registrations/bad-cat")
            .then()
            .statusCode(200);
            
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("settings", "NOT_A_MAP_STILL"))
            .patch("/api/catalog/registrations/bad-cat")
            .then()
            .statusCode(200);
    }
}
