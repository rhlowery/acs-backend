Feature: Audit and Approvals
  As an administrator
  I want to audit activities and approve access requests

  Background:
    Given I am authenticated as "admin" with groups "admins"

  Scenario Outline: Manage access request lifecycle
    Given there is a <initial_status> request for table "<table>"
    When I <action> the request for table "<table>"
    Then the request for table "<table>" should have status "<status>"
    And I <link_visibility> see HATEOAS links to approve it

    Examples:
      | table          | initial_status | action  | status   | link_visibility |
      | secure_data    | pending        | approve | APPROVED | should not      |
      | secure_data    | approved       | verify  | VERIFIED | should not      |
      | sensitive_data | pending        | reject  | REJECTED | should not      |

  Scenario Outline: Log and verify audit entries
    When I post an audit entry for type "<type>"
    Then the audit log should contain the entry

    Examples:
      | type                      |
      | ACCESS_REQUEST_SUBMITTED  |
      | ACCESS_REQUEST_APPROVED   |
      | REJECTION_REASON_LOGGED    |
