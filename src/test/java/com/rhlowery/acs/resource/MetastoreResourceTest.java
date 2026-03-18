package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class MetastoreResourceTest {

    @Test
    public void testGetChildrenRoot() {
        given()
            .get("/api/metastores/uc-oss/children")
            .then()
            .statusCode(200)
            .body("nodes", hasSize(greaterThan(0)))
            .body("nodes.path", hasItems("/main", "/default"));
    }

    @Test
    public void testGetChildrenRecursive() {
        given()
            .queryParam("path", "/")
            .queryParam("depth", 2)
            .get("/api/metastores/uc-oss/children")
            .then()
            .statusCode(200)
            .body("nodes.path", hasItems("/main", "/default", "/main/default"));
    }

    @Test
    public void testGetChildrenPagination() {
        // Mock subList usage in MetastoreResource
        given()
            .queryParam("page_token", "1")
            .get("/api/metastores/uc-oss/children")
            .then()
            .statusCode(200)
            .body("nodes", hasSize(greaterThan(0)));
    }

    @Test
    public void testGetChildrenInvalidPageToken() {
        given()
            .queryParam("page_token", "not-a-number")
            .get("/api/metastores/uc-oss/children")
            .then()
            .statusCode(400)
            .body("error", containsString("Invalid page token"));
    }

    @Test
    public void testGetChildrenNotFound() {
        given()
            .get("/api/metastores/non-existent/children")
            .then()
            .statusCode(404)
            .body("error", containsString("Catalog not found"));
    }

    @Test
    public void testGetChildrenDepth0() {
        given()
            .queryParam("depth", 0)
            .get("/api/metastores/uc-oss/children")
            .then()
            .statusCode(200)
            .body("nodes", hasSize(0));
    }
}
