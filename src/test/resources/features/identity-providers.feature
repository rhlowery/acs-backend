Feature: 3rd Party Identity Providers
  As a system administrator
  I want to support external identity providers
  So that users can authenticate using their existing corporate credentials

  Scenario: List supported identity providers
    Given I am authenticated as "admin" with groups "admins"
    When I request the list of identity providers
    Then the response should contain "Okta"
    And the response should contain "Microsoft Entra ID"

  Scenario Outline: Authenticate via external provider
    When I login via "<provider>" as "<user>"
    Then the response status should be 200
    And the response should contain "providerId" with value "<provider>"
    And the user should have the following groups:
      | group |
      | external-users |
      | <provider>-users |

    Examples:
      | provider | user |
      | okta     | alice |
      | azure-ad | bob   |

  Scenario: Authenticate as admin via Okta
    When I login via "okta" as "admin"
    Then the response status should be 200
    And the response should contain "role" with value "ADMIN"
    And the user should have the following groups:
      | group |
      | admins |
      | okta-admins |
