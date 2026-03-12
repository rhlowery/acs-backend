Feature: Access Request Management
  As an authorized user
  I want to manage access requests
  So that data access can be controlled and audited

  Scenario: Submit a new access request
    Given I am authenticated as "alice" with groups "consumers"
    When I submit a request for catalog "main", schema "default", table "users" with privileges "SELECT"
    Then the request should be saved with status "PENDING"
    And the response should contain HATEOAS links for approval and rejection

  Scenario: Approvers can see pending requests
    Given I am authenticated as "admin" with groups "admins"
    And there is a pending request for table "users"
    When I list all requests
    Then I should see the request for table "users"
    And I should see HATEOAS links to approve it
