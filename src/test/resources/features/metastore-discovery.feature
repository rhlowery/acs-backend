Feature: Metastore Metadata Discovery
  As a Catalog Tree UI user
  I want to fetch children of a metastore node with optional recursion,
  So that I can explore the catalog structure efficiently with pagination support.

  Background:
    Given the ACS Backend is initialized with mock data
    And I am authenticated as "admin" with groups "admins"

  Scenario: List catalogs at the metastore root level
    When I request children for metastore "uc-oss" via "GET /api/metastores/uc-oss/children"
    Then the response status should be 200
    And the response should contain a flat list of catalogs:
      | path      | type    |
      | /main     | CATALOG |
      | /default  | DATABASE|

  Scenario: Request children for a specific path with default depth
    When I request children for metastore "uc-oss" at path "/main"
    Then the response status should be 200
    And the response should contain only immediate children (Schemas):
      | path           | type   |
      | /main/default  | DATABASE |

  Scenario: Request children with depth 2 for recursive discovery
    When I request children for metastore "uc-oss" at path "/" with depth 2
    Then the response status should be 200
    And the response should contain catalogs and their children:
      | path           | type     |
      | /main          | CATALOG  |
      | /main/default  | DATABASE |

  Scenario: Pagination support for large metastore nodes
    When I request children for metastore "uc-oss" at path "/" with depth 2
    Then the response status should be 200
    And a "next_page_token" should be present in the response

  Scenario: Metadata included in flat path list
    When I request children for metastore "uc-oss" at path "/main/default"
    Then each node in the response should have metadata:
      | field       |
      | owner       |
      | comment     |
      | permissions |

  Scenario Outline: Discover children for different metadata providers
    When I request children for metastore "<source>" via "GET /api/metastores/<source>/children"
    Then the response status should be 200
    And the response should contain a flat list of catalogs:
      | path      | type    |
      | <path>    | <type>  |

    Examples:
      | source     | path          | type     |
      | polaris    | /polaris_cat  | CATALOG  |
      | datahub    | /default      | DATABASE |
      | gravitino  | /metalake     | NAMESPACE|
      | atlan      | /asset        | TABLE    |
      | hive       | /hms_cat      | CATALOG  |
