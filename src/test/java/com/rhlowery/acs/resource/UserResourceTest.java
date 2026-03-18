package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class UserResourceTest {

    @Test
    public void testUserManagement() {
        // 1. List users
        given()
            .get("/api/users")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1));

        // 2. List groups
        given()
            .get("/api/groups")
            .then()
            .statusCode(200);

        // 3. Get user
        given()
            .get("/api/users/alice")
            .then()
            .statusCode(200)
            .body("id", is("alice"));

        // 4. Update groups
        given()
            .contentType(ContentType.JSON)
            .body(List.of("new-group"))
            .patch("/api/users/alice/groups")
            .then()
            .statusCode(200)
            .body("groups", hasItem("new-group"));
    }

    @Test
    public void testUserNotFound() {
        given()
            .get("/api/users/non-existent")
            .then()
            .statusCode(404);

        given()
            .contentType(ContentType.JSON)
            .body(List.of("group"))
            .patch("/api/users/non-existent/groups")
            .then()
            .statusCode(404);
    }
}
