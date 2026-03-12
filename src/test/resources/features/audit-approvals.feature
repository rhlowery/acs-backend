Feature: Audit and Approvals
  As an administrator
  I want to audit activities and approve access requests

  Background:
    Given I am authenticated as "admin" with groups "admins"

  Scenario: Log an audit entry
    When I post an audit entry for type "ACCESS_REQUEST_SUBMITTED"
    Then the audit log should contain the entry

  Scenario: Approve an access request
    Given there is a pending request for table "secure_data"
    When I approve the request for table "secure_data"
    Then the request for table "secure_data" should have status "APPROVED"

  Scenario: Reject an access request
    Given there is a pending request for table "confidential_data"
    When I reject the request for table "confidential_data"
    Then the request for table "confidential_data" should have status "REJECTED"
