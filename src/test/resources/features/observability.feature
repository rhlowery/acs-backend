Feature: Observability
  As a DevOps engineer
  I want to monitor the health and metrics of the service
  So that I can ensure its availability and performance

  Scenario Outline: Monitor service health and metadata
    When I call the <endpoint> endpoint
    Then the response should contain "<expected_snippet>"
    And the response status should be 200

    Examples:
      | endpoint | expected_snippet      |
      | health   | UP                    |
      | metrics  | jvm_memory_used_bytes |
      | openapi  | ACS Backend API       |
