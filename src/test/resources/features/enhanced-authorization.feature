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
    When "ben" who is in "finance-approvers" approves the request
    Then the request status should be "PARTIALLY_APPROVED"
    When "admin" who is in "governance-team" approves the request
    Then the request status should be "APPROVED"
    And the policy should be applied in the catalog

  Scenario: Denial by any approver rejects the whole request with reason
    Given "alice" has a pending request for "/databricks/default/sensitive_tbl"
    When "charlie" who is in "sensitive-approvers" denies the request with reason "Insufficient business justification"
    Then the request status should be "REJECTED"
    And the audit log should record the reason "Insufficient business justification"
    And no policy should be applied

  Scenario: Mandatory Governance approval for multi-owner request
    Given "alice" has a pending request for "/databricks/finance/salaries" (Owner: finance-leads)
    And the request requires approval from "governance-team"
    When "ben" who is in "finance-leads" approves the request
    Then the request status should be "PARTIALLY_APPROVED"
    And the request status should be "PARTIALLY_APPROVED"

  Scenario: Request access for a Service Principal
    Given "alice" is logged in
    When she requests "SELECT" on "/databricks/default/main_tbl" for Service Principal "sp-data-etl"
    Then the request status should be "PENDING"
    And the target principal should be "sp-data-etl" with type "SERVICE_PRINCIPAL"

  Scenario: Request access to a Unity Catalog Volume
    Given "alice" is logged in
    When she requests "READ_VOLUME" on "/databricks/staged/data_uploads" (type: VOLUME)
    Then the request status should be "PENDING"
    And it should require approval from "data-governors"
