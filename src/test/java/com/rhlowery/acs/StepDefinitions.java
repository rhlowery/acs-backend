package com.rhlowery.acs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class StepDefinitions {

    private String currentToken;
    private Response lastResponse;

    @Given("I am authenticated as {string} with groups {string}")
    public void i_am_authenticated_as_with_groups(String user, String groups) {
        Response response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("userId", user, "userName", user, "groups", List.of(groups.split(","))))
            .post("/api/auth/login");
        
        response.then().statusCode(200);
        currentToken = response.getCookie("bff_jwt");
        assertNotNull(currentToken, "JWT cookie should not be null");
    }

    @When("I submit a request for catalog {string}, schema {string}, table {string} with privileges {string}")
    public void i_submit_a_request_for_catalog_schema_table_with_privileges(String catalog, String schema, String table, String privileges) {
        String requestId = UUID.randomUUID().toString();
        lastResponse = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", requestId,
                "catalogName", catalog,
                "schemaName", schema,
                "tableName", table,
                "privileges", List.of(privileges),
                "justification", "Testing"
            )))
            .post("/api/storage/requests");
        lastResponse.then().statusCode(200);
    }

    @Then("the request should be saved with status {string}")
    public void the_request_should_be_saved_with_status(String status) {
        lastResponse.then().statusCode(200).body("status", equalTo("success"));
        
        RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("[0].status", equalTo(status));
    }

    @Then("the response should contain HATEOAS links for approval and rejection")
    public void the_response_should_contain_hateoas_links_for_approval_and_rejection() {
        RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("[0]._links.approve.href", notNullValue())
            .body("[0]._links.reject.href", notNullValue());
    }

    @Given("there is a pending request for table {string}")
    public void there_is_a_pending_request_for_table_p(String table) {
        i_submit_a_request_for_catalog_schema_table_with_privileges("main", "default", table, "SELECT");
    }

    @When("I list all requests")
    public void i_list_all_requests() {
        lastResponse = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests");
    }

    @Then("I should see the request for table {string}")
    public void i_should_see_the_request_for_table(String table) {
        lastResponse.then().statusCode(200).body("tableName", hasItem(table));
    }

    @Then("I should see HATEOAS links to approve it")
    public void i_should_see_hateoas_links_to_approve_it() {
        lastResponse.then().body("[0]._links.approve.href", notNullValue());
    }

    @When("I call the health endpoint")
    public void i_call_the_health_endpoint() {
        lastResponse = RestAssured.get("/health");
    }

    @Then("the status should be {string}")
    public void the_status_should_be(String status) {
        lastResponse.then().statusCode(200).body("status", equalTo(status));
    }

    @Then("it should contain liveness and readiness checks")
    public void it_should_contain_liveness_and_readiness_checks() {
        lastResponse.then()
            .body("checks.name", hasItems("ACS Backend is alive", "ACS Backend is ready to serve requests"));
    }

    @When("I call the metrics endpoint")
    public void i_call_the_metrics_endpoint() {
        lastResponse = RestAssured.get("/metrics");
    }

    @When("I call the openapi endpoint")
    public void i_call_the_openapi_endpoint() {
        lastResponse = RestAssured.get("/openapi");
    }

    @Then("the response should contain {string}")
    public void the_response_should_contain(String content) {
        lastResponse.then().statusCode(200).body(containsString(content));
    }

    @When("I post an audit entry for type {string}")
    public void i_post_an_audit_entry_for_type(String type) {
        lastResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("type", type, "actor", "alice", "details", Map.of("reason", "test")))
            .post("/api/audit/log");
        lastResponse.then().statusCode(200);
    }

    @Then("the audit log should contain the entry")
    public void the_audit_log_should_contain_the_entry() {
        RestAssured.get("/api/audit/log")
            .then()
            .statusCode(200)
            .body("[0].type", notNullValue());
    }

    @When("I approve the request for table {string}")
    public void i_approve_the_request_for_table(String table) {
        Response response = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests");
        
        response.then().statusCode(200);
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        assertNotNull(id, "Request ID for table " + table + " should not be null");
        
        lastResponse = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .post("/api/storage/requests/" + id + "/approve");
        lastResponse.then().statusCode(200);
    }

    @When("I reject the request for table {string}")
    public void i_reject_the_request_for_table(String table) {
        Response response = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests");
        
        response.then().statusCode(200);
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        assertNotNull(id, "Request ID for table " + table + " should not be null");
        
        String url = "/api/storage/requests/" + id + "/reject";
        lastResponse = RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .post(url);
        lastResponse.then().statusCode(200);
    }

    @Then("the request for table {string} should have status {string}")
    public void the_request_for_table_should_have_status(String table, String status) {
        RestAssured.given()
            .cookie("bff_jwt", currentToken)
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("find { it.tableName == '" + table + "' }.status", equalTo(status));
    }
}
