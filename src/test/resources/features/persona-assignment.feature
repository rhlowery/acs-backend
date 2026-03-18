Feature: Persona and Role Assignment

  As an administrator,
  I want to manage user personas explicitly,
  so that I can define user capabilities independent of their provider-assigned roles.

  Background:
    Given the ACS Backend is initialized with mock data
    And an admin user "bob" is logged in

  Scenario: List all available personas
    When I request the list of available personas via "GET /api/auth/personas"
    Then the response status should be 200
    And the response should contain the following personas:
      | id         | name       | description                                 |
      | ADMIN      | Admin      | Full system access and configuration        |
      | APPROVER   | Approver   | Can review and approve/reject requests      |
      | REQUESTER  | Requester  | Can submit and view own access requests     |

  Scenario: Assign persona to a user
    Given a user "alice" exists in the system
    When I assign the persona "APPROVER" to user "alice" via "PUT /api/auth/users/alice/persona"
    Then the response status should be 200
    And the user "alice" should have the persona "APPROVER" in their profile

  Scenario: Persona overrules IDP roles for request approval
    Given user "alice" with IDP role "STANDARD_USER" is assigned the persona "APPROVER"
    And "alice" is logged in
    And a pending access request "req-1" exists for "charlie"
    When "alice" approves the access request "req-1"
    Then the response status should be 200
    And the access request "req-1" should have status "APPROVED"

  Scenario: Requester persona cannot approve requests even if in approver group
    Given user "alice" is in the group "governance-team"
    And user "alice" is assigned the persona "REQUESTER"
    And "alice" is logged in
    And a pending access request "req-2" exists for "charlie"
    When "alice" attempts to approve the access request "req-2"
    Then the response status should be 403
    And the access request "req-2" should have status "PENDING"

  Scenario: Assign persona to a group and verify user inheritance
    Given a group "data-governors" exists in the system
    When I assign the persona "GOVERNANCE_ADMIN" to group "data-governors" via "PUT /api/auth/groups/data-governors/persona"
    Then the response status should be 200
    And user "alice" in group "data-governors" should have the persona "GOVERNANCE_ADMIN" after login

  Scenario: Governance admin can reject requests
    Given user "alice" is assigned the persona "GOVERNANCE_ADMIN"
    And "alice" is logged in
    And a pending access request "req-3" exists for "charlie"
    When "alice" rejects the access request "req-3" with reason "Insufficient justification"
    Then the response status should be 200
    And the access request "req-3" should have status "REJECTED"
