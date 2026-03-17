Feature: User and Group Management
  As a system administrator
  I want to manage users and their group memberships
  So that I can assign roles and approve requests correctly

  Scenario: List all users
    Given I am authenticated as "bob" with groups "admins"
    When I request the list of all users
    Then the response should contain user "alice"
    And the response should contain user "bob"

  Scenario: List all groups
    Given I am authenticated as "bob" with groups "admins"
    When I request the list of all available groups
    Then the response should contain group "admins"
    And the response should contain group "data-governors"
    And the response should contain group "governance-team"

  Scenario: Update user group memberships
    Given I am authenticated as "bob" with groups "admins"
    And user "alice" has groups:
      | group |
      | standard-users |
    When I update groups for user "alice" to:
      | group |
      | standard-users |
      | data-governors |
    Then the user "alice" should have the following groups:
      | group |
      | standard-users |
      | data-governors |
