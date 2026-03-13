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
import jakarta.inject.Inject;
import com.rhlowery.acs.service.AccessRequestService;

public class StepDefinitions {

    private String currentToken;
    private Response lastResponse;

    @Inject
    AccessRequestService accessRequestService;

    @Given("I am authenticated as {string} with groups {string}")
    public void i_am_authenticated_as_with_groups(String user, String groups) {
        Response response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "userId", user,
                "role", user.contains("admin") ? "ADMIN" : "STANDARD_USER",
                "groups", List.of(groups.split(","))
            ))
            .post("/api/auth/login");
        
        response.then().statusCode(200);
        currentToken = response.getCookie("bff_jwt");
        assertNotNull(currentToken, "JWT cookie should not be null");
        System.out.println("DEBUG: Generated token: " + currentToken);
    }

    private io.restassured.specification.RequestSpecification givenAuth() {
        System.out.println("DEBUG: givenAuth() currentToken: " + (currentToken != null ? "present" : "NULL"));
        return RestAssured.given()
            .cookie("bff_jwt", currentToken != null ? currentToken : "")
            .header("Authorization", "Bearer " + currentToken);
    }

    @When("I submit a request for catalog {string}, schema {string}, table {string} with privileges {string}")
    public void i_submit_a_request_for_catalog_schema_table_with_privileges(String catalog, String schema, String table, String privileges) {
        String requestId = UUID.randomUUID().toString();
        lastResponse = givenAuth()
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

    @Given("there is a pending request for table {string}")
    public void there_is_a_pending_request_for_table(String table) {
        String requestId = UUID.randomUUID().toString();
        givenAuth()
            .contentType(ContentType.JSON)
            .body(List.of(Map.of(
                "id", requestId,
                "catalogName", "main",
                "schemaName", "default",
                "tableName", table,
                "privileges", List.of("SELECT"),
                "justification", "Admin test"
            )))
            .post("/api/storage/requests")
            .then().statusCode(200);
    }

    @Then("the request should be saved with status {string}")
    public void the_request_should_be_saved_with_status(String status) {
        lastResponse.then().statusCode(200).body("status", equalTo("success"));
        
        givenAuth()
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("[0].status", equalTo(status));
    }

    @Given("I submit a request for catalog {string}, table {string}")
    public void i_submit_a_request_simple(String catalog, String table) {
        i_submit_a_request_for_catalog_schema_table_with_privileges(catalog, "default", table, "SELECT");
    }

    @Then("the response should contain HATEOAS links for approval and rejection")
    public void the_response_should_contain_hateoas_links_for_approval_and_rejection() {
        givenAuth()
            .get("/api/storage/requests")
            .then()
            .statusCode(200)
            .body("[0]._links.approve.href", notNullValue())
            .body("[0]._links.reject.href", notNullValue());
    }

    @When("I list all requests")
    public void i_list_all_requests() {
        lastResponse = givenAuth()
            .get("/api/storage/requests");
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
        lastResponse.then().body(containsString(content));
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
        Response response = givenAuth()
            .get("/api/storage/requests");
        
        response.then().statusCode(200);
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        assertNotNull(id, "Request ID for table " + table + " should not be null");
        
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve");
        lastResponse.then().statusCode(200);
    }

    @When("I reject the request for table {string}")
    public void i_reject_the_request_for_table(String table) {
        Response response = givenAuth()
            .get("/api/storage/requests");
        
        response.then().statusCode(200);
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        assertNotNull(id, "Request ID for table " + table + " should not be null");
        
        String url = "/api/storage/requests/" + id + "/reject";
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .post(url);
        lastResponse.then().statusCode(200);
    }

    @Then("the request for table {string} should have status {string}")
    public void the_request_for_table_should_have_status(String table, String status) {
        lastResponse = givenAuth()
            .get("/api/storage/requests");
        
        lastResponse.then()
            .statusCode(200)
            .body("find { it.tableName == '" + table + "' }.status", equalTo(status));
    }

    @When("I search the catalog for {string}")
    public void i_search_the_catalog_for(String query) {
        lastResponse = givenAuth()
            .get("/api/catalog/search?query=" + query);
    }

    @Then("^I should (.*) \"([^\"]*)\"$")
    public void i_should_generic_action(String action, String value) {
        if (action.contains("see the request for table")) {
            lastResponse.then().statusCode(200).body("tableName", hasItem(value));
        } else if (action.contains("contain")) {
            the_response_should_contain(value);
        }
    }

    @When("I execute a SQL statement {string}")
    public void i_execute_a_sql_statement(String sql) {
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .body(Map.of("statement", sql, "warehouse_id", "test"))
            .post("/api/sql/execute");
        lastResponse.then().statusCode(200);
    }

    @When("I try to approve the request for table {string}")
    public void i_try_to_approve_the_request_for_table(String table) {
        Response response = givenAuth()
            .get("/api/storage/requests");
        
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .post("/api/storage/requests/" + id + "/approve");
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(int code) {
        lastResponse.then().statusCode(code);
    }

    @Then("the response status should be {string}")
    public void the_response_status_should_be_str(String code) {
        lastResponse.then().statusCode(Integer.parseInt(code));
    }

    @When("I logout")
    public void i_logout() {
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .post("/api/auth/logout");
        currentToken = null; // Clear local token
    }

    @When("I call the me endpoint")
    public void i_call_the_me_endpoint() {
        lastResponse = givenAuth()
            .get("/api/auth/me");
    }

    @When("I try to approve a non-existent request {string}")
    public void i_try_to_approve_a_non_existent_request(String id) {
        lastResponse = givenAuth()
            .post("/api/storage/requests/" + id + "/approve");
    }

    @When("I submit an empty request list")
    public void i_submit_an_empty_request_list() {
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .body(List.of())
            .post("/api/storage/requests");
    }

    @When("I call the stream endpoint")
    public void i_call_the_stream_endpoint() {
        lastResponse = givenAuth()
            .config(io.restassured.config.RestAssuredConfig.config()
                .httpClient(io.restassured.config.HttpClientConfig.httpClientConfig()
                    .setParam("http.socket.timeout", 2000)
                    .setParam("http.connection.timeout", 2000)))
            .get("/api/storage/requests/stream");
    }

    @When("I fetch {string} from the SDK")
    public void i_fetch_from_the_sdk(String target) {
        lastResponse = givenAuth()
            .header("x-workspace-host", "test-host")
            .get("/api/sdk/" + target);
    }

    @When("I call the generic UC proxy at {string}")
    public void i_call_the_generic_uc_proxy_at(String path) {
        lastResponse = givenAuth()
            .get("/api/uc/" + path);
    }

    @When("I call the search api with query {string}")
    public void i_call_the_search_api_with_query(String query) {
        lastResponse = givenAuth()
            .queryParam("q", query)
            .get("/api/catalog/search");
    }

    @When("I execute SQL {string}")
    public void i_execute_sql(String sql) {
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .body(Map.of("statement", sql))
            .post("/api/sql/execute");
    }

    @When("I execute SQL with empty statement")
    public void i_execute_sql_with_empty_statement() {
        lastResponse = givenAuth()
            .contentType(ContentType.JSON)
            .body(Map.of("parameters", Map.of())) // Missing statement
            .post("/api/sql/execute");
    }

    @Then("^I (.*) see HATEOAS links to approve it$")
    public void i_see_hateoas_links_to_approve_it(String visibility) {
        if (visibility.contains("should not")) {
            lastResponse.then().body("any { it.tableName == 'sensitive_data' && it.links.any { it.rel == 'approve' } }", is(false));
        } else {
            lastResponse.then().body("[0]._links.approve.href", notNullValue());
        }
    }

    @When("I retrieve the request for table {string} by id")
    public void i_retrieve_the_request_for_table_by_id(String table) {
        Response response = givenAuth()
            .get("/api/storage/requests");
        
        String id = response.then().extract().path("find { it.tableName == '" + table + "' }.id");
        
        lastResponse = givenAuth()
            .get("/api/storage/requests/" + id);
    }

    @When("I try to login with no userId")
    public void i_try_to_login_with_no_user_id() {
        lastResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(Map.of("invalid", "data"))
            .post("/api/auth/login");
    }
}
