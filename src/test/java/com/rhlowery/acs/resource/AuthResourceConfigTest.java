package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.restassured.http.ContentType;

@QuarkusTest
public class AuthResourceConfigTest {

    @Test
    public void testGetConfig() {
        given()
            .accept(ContentType.JSON)
            .get("/api/auth/config")
            .then()
            .statusCode(200)
            .body("authServerUrl", notNullValue())
            .body("clientId", notNullValue())
            .body("discoveryEnabled", is(true));
    }
}
