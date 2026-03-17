Feature: Enhanced Authorization with Time-Bound Access and Multi-Approvers

  Scenario: Request access with time-bound constraint
    Given "alice" is logged in
    When she requests "SELECT" on "/databricks/default/sensitive_tbl" with justification "Need for analysis" and expiration in "24" hours
    Then the request status should be "PENDING"
    And it should have an expiration time set
    And it should require approval from "sensitive-approvers"

  Scenario: Multi-signature approval from designated groups
    Given "alice" has a pending request for "/databricks/finance/quarters"
    And the request requires approval from "finance-approvers"
    When "bob" who is in "finance-approvers" approves the request
    Then the request status should be "APPROVED"
    And the policy should be applied in the catalog

  Scenario: Denial by any approver rejects the whole request
    Given "alice" has a pending request for "/databricks/default/sensitive_tbl"
    When "charlie" who is in "sensitive-approvers" denies the request
    Then the request status should be "REJECTED"
    And no policy should be applied

  Scenario: Administrator bypasses multi-signature
    Given "alice" has a pending request for "/databricks/finance/quarters"
    And "admin" is logged in
    When he approves the pending request from "alice" for "/databricks/finance/quarters"
    Then the request status should be "APPROVED"
    And the policy should be applied in the catalog
