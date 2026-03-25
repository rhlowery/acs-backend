Feature: 3rd Party Identity Providers
  As a system administrator
  I want to support external identity providers
  So that users can authenticate using their existing corporate credentials

  Scenario: Get identity provider configuration
    When I request the identity provider configuration
    Then the response status should be 200
    And the response should contain "authServerUrl"
    And the response should contain "clientId"

  Scenario: List supported identity providers
    Given I am authenticated as "admin" with groups "admins"
    When I request the list of identity providers
    Then the response should contain "Standard OIDC Provider"
    And the response should contain "Standard SAML 2.0 Provider"

  Scenario Outline: Authenticate via external provider
    When I login via "<provider>" as "<user>"
    Then the response status should be 200
    And the response should contain "providerId" with value "<provider>"

    Examples:
      | provider | user |
      | oidc     | alice |
      | saml     | bob   |
