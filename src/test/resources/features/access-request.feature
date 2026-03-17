Feature: Access Request Management
  As an authorized user
  I want to manage access requests
  So that data access can be controlled and audited

  Scenario Outline: Submit and verify access requests
    Given I am authenticated as "<user>" with groups "<groups>"
    When I submit a request for catalog "<catalog>", schema "<schema>", table "<table>" with privileges "<privileges>"
    Then the request should be saved with status "PENDING"
    And the response should contain HATEOAS links for approval and rejection

    Examples:
      | user  | groups    | catalog | schema  | table | privileges     |
      | alice | consumers | main    | default | users | SELECT         |
      | bob   | users     | prod    | data    | sales | READ           |
      | carol | guest     | public  | info    | help  | USAGE          |
      | daryl | polaris   | polaris | cat     | tab   | SELECT         |
      | edgar | datahub   | datahub | db      | tbl   | READ           |
      | frank | gravitino | grav    | ns      | obj   | USAGE          |
      | grace | atlan     | atlan   | assets  | item  | SELECT         |
      | henry | hive      | hive    | default | tests | READ           |
      | ian   | volumes   | main    | upload  | vol1  | READ_VOLUME    |
      | jack  | ml        | main    | model   | mod1  | EXECUTE_MODEL  |

  Scenario Outline: Approvers visibility and retrieval
    Given I am authenticated as "<user>" with groups "<groups>"
    And there is a pending request for table "<table>"
    When I <action>
    Then I should <result> "<table>"
    And the response status should be <status>

    Examples:
      | user  | groups | table      | action                                            | result                    | status |
      | admin | admins | users      | list all requests                                 | see the request for table | 200    |
      | user1 | users  | test_table | retrieve the request for table "test_table" by id | contain                   | 200    |
      | admin | admins | secure_log | list all requests                                 | see the request for table | 200    |

  Scenario Outline: Access request edge cases and errors
    Given I am authenticated as "admin" with groups "admins"
    When I <action>
    Then the response status should be <status>

    Examples:
      | action                                           | status |
      | try to approve a non-existent request "invalid"  | 404    |
      | submit an empty request list                     | 400    |
      | call the stream endpoint                         | 200    |
