Feature: Proxy and Search
  As a developer
  I want to proxy requests to Unity Catalog and search for tables

  Background:
    Given I am authenticated as "admin" with groups "admins"

  Scenario Outline: Execute operations via Unity Catalog proxy
    When I <operation>
    Then the response <assertion> "<expected>"
    And the response status should be <status>

    Examples:
      | operation                                            | assertion        | expected        | status |
      | search the catalog for "sensitive"                   | should contain   | sensitive_table | 200    |
      | execute a SQL statement "SELECT * FROM data"         | should contain   | success         | 200    |
      | fetch "catalogs" from the SDK                        | should contain   | sensitive_table | 200    |
      | call the search api with query "test"                | status should be | 200             | 200    |
      | call the search api with query ""                    | status should be | 200             | 200    |
      | call the generic UC proxy at "tables/mytable"        | status should be | 501             | 501    |
      | execute SQL with empty statement                     | status should be | 400             | 400    |
