Feature: Common Catalog Interface
  As a system architect
  I want a generic interface to traverse and manage different data catalogs
  So that I can overlay security and audit policies consistently across various backends

  Background:
    Given I am authenticated as "admin" with groups "admins"

  Scenario Outline: Traverse data catalog tree across implementations
    When I request the children of path "<path>" from catalog "<catalog_id>"
    Then the response should contain the node "<expected_node>"
    And the node type should be "<expected_type>"
    And the implementation should be "<implementation_class>"

    Examples:
      | catalog_id | path              | expected_node | expected_type | implementation_class           |
      | uc-oss     | /                 | main          | CATALOG       | UnityCatalogNodeProvider       |
      | glue-prod  | /                 | default       | DATABASE      | GlueNodeProvider               |
      | databricks | /main/default     | sensitive_tbl | TABLE         | DatabricksNodeProvider         |
      | iceberg    | /finance          | quarters      | NAMESPACE     | IcebergNodeProvider            |
      | polaris    | /                 | polaris_cat   | CATALOG       | PolarisNodeProvider            |
      | datahub    | /                 | default       | DATABASE      | DataHubNodeProvider            |
      | gravitino  | /                 | metalake      | NAMESPACE     | GravitinoNodeProvider          |
      | atlan      | /                 | asset         | TABLE         | AtlanNodeProvider              |
      | hive       | /                 | hms_cat       | CATALOG       | HiveMetastoreNodeProvider      |

  Scenario Outline: Overlay and proxy authorization on catalog nodes
    Given there is a catalog node at path "<path>" in catalog "<catalog_id>"
    When I <action> access for principal "<principal>" on node "<path>" in catalog "<catalog_id>"
    Then the audit log should record the "<audit_type>" event
    And the node "<path>" in catalog "<catalog_id>" should have effective permissions "<permissions>"

    Examples:
      | catalog_id | path                     | action   | principal | audit_type     | permissions |
      | uc-oss     | /main/default/users      | overlay  | alice     | POLICY_APPLIED | SELECT      |
      | glue-prod  | /glue-prod/hr/salaries   | revoke   | bob       | ACCESS_REVOKED | NONE        |
      | databricks | /databricks/sys/logs     | approve  | charlie   | REQUEST_OK     | READ        |
      | polaris    | /polaris/root/secret     | overlay  | polaris   | POLICY_APPLIED | READ        |

  Scenario: Plugin registration via Service Loader
    When I list registered catalog providers
    Then I verify that I see in the provider list "com.rhlowery.acs.service.impl.GlueNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.UnityCatalogNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.PolarisNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.DataHubNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.GravitinoNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.AtlanNodeProvider"
    And I verify that I see in the provider list "com.rhlowery.acs.service.impl.HiveMetastoreNodeProvider"
