package com.rhlowery.acs.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import com.rhlowery.acs.infrastructure.DatabricksClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ProxyResourceTest {

    @InjectMock
    @RestClient
    DatabricksClient databricksClient;

    @Test
    public void testSdkFetchSuccess() {
        when(databricksClient.fetchFromUC(any(), any(), any(), any(), any(), any()))
            .thenReturn(java.util.Map.of("data", "test"));
            
        given()
            .header("x-workspace-host", "test-host")
            .get("/api/sdk/catalogs")
            .then()
            .statusCode(200);
    }

    @Test
    public void testSqlExecuteSuccess() {
        when(databricksClient.executeSql(any(), any()))
            .thenReturn(java.util.Map.of("data", "test"));
            
        given()
            .contentType("application/json")
            .body(java.util.Map.of("statement", "SELECT 1"))
            .post("/api/sql/execute")
            .then()
            .statusCode(200);
    }
    
    @Test
    public void testGenericProxy() {
        // Generic proxy is not fully implemented yet in the resource
        given()
            .get("/api/uc/some/path")
            .then()
            .statusCode(501);
    }

    @Test
    public void testSdkFetchError() {
        when(databricksClient.fetchFromUC(any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("SDK Error"));
            
        given()
            .header("x-workspace-host", "test-host")
            .get("/api/sdk/catalogs")
            .then()
            .statusCode(500);
    }

    @Test
    public void testSqlExecuteError() {
        when(databricksClient.executeSql(any(), any()))
            .thenThrow(new RuntimeException("SQL Error"));
            
        given()
            .contentType("application/json")
            .body(java.util.Map.of("statement", "SELECT 1"))
            .post("/api/sql/execute")
            .then()
            .statusCode(500);
    }
}
