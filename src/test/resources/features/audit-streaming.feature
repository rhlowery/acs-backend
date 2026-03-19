@AuditStreaming
Feature: Audit Log Streaming
  As an Auditor or Reviewer
  I want to receive real-time streams of audit events
  So that I can monitor system activity as it happens.

  Background:
    Given the ACS Backend is initialized with mock data
    And I am authenticated as "auditor-user" with persona "AUDITOR"

  Scenario: Connect to audit stream and receive events
    When I connect to the audit SSE stream at "/api/audit/log/stream"
    And another user "admin" logs an audit event:
      | type   | actor | details |
      | LOGIN  | admin | { "ip": "127.0.0.1" } |
    Then I should receive an SSE event with type "audit-log" containing:
      | type   | actor | details |
      | LOGIN  | admin | { "ip": "127.0.0.1" } |

  Scenario: Access restricted for unauthorized personas
    Given I am authenticated as "standard-user" with persona "REQUESTER"
    When I attempt to connect to the audit SSE stream at "/api/audit/log/stream"
    Then the response status should be 403

  Scenario: Multiple events are streamed in order
    When I connect to the audit SSE stream at "/api/audit/log/stream"
    And another user "governor" logs multiple audit events:
      | type    | actor    | details |
      | CREATE  | governor | { "object": "table1" } |
      | UPDATE  | governor | { "object": "table1" } |
    Then I should receive 2 SSE events in order:
      | type    | actor    | details |
      | CREATE  | governor | { "object": "table1" } |
      | UPDATE  | governor | { "object": "table1" } |
