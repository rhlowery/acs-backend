Feature: Catalog Registration Management
  As an administrator
  I want to manage catalog registrations via API
  So that I can dynamically add, update, and remove catalog connections

  Background:
    Given I am authenticated as "admin" with groups "admins"

  Scenario Outline: Manage catalog registrations through the lifecycle
    When I register a new catalog with id "<catalog_id>" and type "<type>" and settings:
      | host | <host> |
      | port | <port> |
    Then the response status should be 201
    And the catalog "<catalog_id>" should be present in the registration list

    When I update the catalog "<catalog_id>" settings with:
      | host | <new_host> |
    Then the response status should be 200
    And the catalog "<catalog_id>" host should be "<new_host>"

    When I remove the catalog registration for "<catalog_id>"
    Then the response status should be 204
    And the catalog "<catalog_id>" should not be present in the registration list

    Examples:
      | catalog_id | type | host             | port | new_host          |
      | hms-dev    | hive | hms.dev.local    | 9083 | hms-new.dev.local |
      | polaris-1  | uc   | polaris.io       | 443  | api.polaris.io    |

  Scenario: List all catalog registrations
    Given the following catalogs are registered:
      | id        | type |
      | cat-1     | glue |
      | cat-2     | uc   |
    When I request the list of all registered catalogs
    Then the response status should be 200
    And the following catalog IDs should be in the list:
      | cat-1 |
      | cat-2 |
