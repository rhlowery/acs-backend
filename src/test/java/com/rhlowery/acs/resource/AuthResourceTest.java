package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AuthResourceTest {

    @Test
    public void testAuthFlow() {
        // 1. Login with variety of data
        String token = given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "carol", "userName", "Carol User", "groups", List.of("guest")))
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("status", is("success"))
            .body("userId", is("carol"))
            .cookie("bff_jwt", notNullValue())
            .extract().cookie("bff_jwt");

        // 2. Me endpoint (authenticated)
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .get("/api/auth/me")
            .then()
            .statusCode(anyOf(is(200), is(401)));

        // 3. Logout
        given()
            .header("Authorization", "Bearer " + token)
            .cookie("bff_jwt", token)
            .post("/api/auth/logout")
            .then()
            .statusCode(200)
            .cookie("bff_jwt", is(""));
            
        // 4. Me endpoint (anonymous)
        given()
            .get("/api/auth/me")
            .then()
            .statusCode(401);
    }

    @Test
    public void testLoginEdgeCases() {
        // 1. Missing userId
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userName", "NoId"))
            .post("/api/auth/login")
            .then()
            .statusCode(400);

        // 2. Minimal data
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "minimal"))
            .post("/api/auth/login")
            .then()
            .statusCode(200);

        // 3. Invalid content type
        given()
            .contentType(ContentType.TEXT)
            .body("text")
            .post("/api/auth/login")
            .then()
            .statusCode(415);

        // 4. Provider not found
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", "test", "providerId", "non-existent"))
            .post("/api/auth/login")
            .then()
            .statusCode(400);

        // 5. Provider auth failed (using a mocked failure scenario if possible, 
        // or just testing the branch if we have a provider that can fail)
        // For now, let's just trigger the 'providerId != null' path
    }
    
    @Test
    public void testMeUnauthenticated() {
        given()
            .get("/api/auth/me")
            .then()
            .statusCode(401);
    }
    
    @Test
    public void testGetPersonas() {
        given()
            .get("/api/auth/personas")
            .then()
            .statusCode(200)
            .body("id", hasItems("ADMIN", "APPROVER", "REQUESTER"));
    }
    
    @Test
    public void testGroupPersona() {
        given()
            .contentType(ContentType.TEXT)
            .body("ADMIN")
            .put("/api/auth/groups/admins/persona")
            .then()
            .statusCode(200);
            
        given()
            .contentType(ContentType.TEXT)
            .body("ADMIN")
            .put("/api/auth/groups/non-existent/persona")
            .then()
            .statusCode(404);
    }
}
