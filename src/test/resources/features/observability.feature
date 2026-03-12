Feature: Observability
  As a DevOps engineer
  I want to monitor the health and metrics of the service
  So that I can ensure its availability and performance

  Scenario: Health check endpoint returns UP
    When I call the health endpoint
    Then the status should be "UP"
    And it should contain liveness and readiness checks

  Scenario: Metrics endpoint returns Prometheus data
    When I call the metrics endpoint
    Then the response should contain "jvm_memory_used_bytes"

  Scenario: OpenAPI endpoint returns YAML or JSON
    When I call the openapi endpoint
    Then the response should contain "openapi"
    And the response should contain "ACS Backend API"
