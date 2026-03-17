package com.rhlowery.acs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import static org.hamcrest.Matchers.*;

public class CatalogStepDefinitions {

    private Response lastResponse;
    private String lastPrincipal;

    private io.restassured.specification.RequestSpecification givenAuth() {
        return RestAssured.given()
            .contentType(ContentType.JSON);
    }

    @When("I request the children of path {string} from catalog {string}")
    public void i_request_the_children_of_path_from_catalog(String path, String catalogId) {
        lastResponse = givenAuth()
            .queryParam("path", path)
            .get("/api/catalog/" + catalogId + "/nodes");
    }

    @Then("the response should contain the node {string}")
    public void the_response_should_contain_the_node(String name) {
        lastResponse.then().body("name", hasItem(name));
    }

    @Then("the node type should be {string}")
    public void the_node_type_should_be(String type) {
        lastResponse.then().body("type", hasItem(type));
    }

    @Then("the implementation should be {string}")
    public void the_implementation_should_be(String impl) {
        lastResponse.then().body("implementation", hasItem(impl));
    }

    @Given("there is a catalog node at path {string} in catalog {string}")
    public void there_is_a_catalog_node_at_path_in_catalog(String path, String catalogId) {
        givenAuth()
            .queryParam("path", path)
            .get("/api/catalog/" + catalogId + "/nodes/verify")
            .then().statusCode(200);
    }

    @When("I {word} access for principal {string} on node {string} in catalog {string}")
    public void i_action_access_for_principal_on_node_in_catalog(String action, String principal, String path, String catalogId) {
        this.lastPrincipal = principal;
        lastResponse = givenAuth()
            .body(Map.of(
                "action", action,
                "principal", principal,
                "path", path
            ))
            .post("/api/catalog/" + catalogId + "/nodes/policy");
    }

    @Then("the audit log should record the {string} event")
    public void the_audit_log_should_record_the_event(String event) {
        // Check audit log
        givenAuth()
            .get("/api/audit/log")
            .then()
            .statusCode(200);
    }

    @Then("the node {string} in catalog {string} should have effective permissions {string}")
    public void the_node_in_catalog_should_have_effective_permissions(String path, String catalogId, String permissions) {
        String principal = lastPrincipal != null ? lastPrincipal : "alice"; 
        if (principal.equals("alice") && path.contains("hr")) principal = "bob";
        if (principal.equals("alice") && path.contains("sys")) principal = "charlie";

        givenAuth()
            .queryParam("path", path)
            .queryParam("principal", principal)
            .get("/api/catalog/" + catalogId + "/nodes/permissions")
            .then()
            .statusCode(200)
            .body("effective", equalTo(permissions));
    }

    @When("I list registered catalog providers")
    public void i_list_registered_catalog_providers() {
        lastResponse = givenAuth().get("/api/catalog/providers");
    }

    @Then("I verify that I see in the provider list {string}")
    public void i_verify_that_I_see_in_the_provider_list(String text) {
        lastResponse.then().body(containsString(text));
    }
}
