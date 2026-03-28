Feature: String flag resolution through OpenFeature
  As a Java developer using OpenFeature with an existing flag management service,
  I want string flags resolved through the OpenFeature SDK,
  so FlagZen's polymorphic dispatch works with my existing flag infrastructure.

  # --- US-OF-01: String resolution happy paths ---

  @US-OF-01
  Scenario: Resolved flag value is returned to the caller
    Given the flag management service has flag "checkout-flow" set to "EXPRESS"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves string flag "checkout-flow" through the adapter
    Then the adapter returns "EXPRESS"

  @US-OF-01
  Scenario: Adapter constructed with a specific named client resolves flags through that client
    Given the developer has a dedicated OpenFeature client for the "payments" domain
    And the flag management service has flag "payment-method" set to "STRIPE" for that client
    When the developer creates an OpenFeature adapter with that specific client
    And the developer resolves string flag "payment-method" through the adapter
    Then the adapter returns "STRIPE"

  # --- US-OF-01: Error and edge paths ---

  @US-OF-01
  Scenario: Flag not configured in the upstream service returns no value
    Given the flag management service has no flag named "new-dashboard"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves string flag "new-dashboard" through the adapter
    Then the adapter returns no string value

  @US-OF-01
  Scenario: Flag evaluation error in the upstream service returns no value
    Given the flag management service returns an error for flag "checkout-flow"
    And the developer creates an OpenFeature adapter with that service
    When the developer resolves string flag "checkout-flow" through the adapter
    Then the adapter returns no string value

  @US-OF-01
  Scenario: Adapter with no upstream service configured returns no value
    Given no flag management service has been registered
    And the developer creates an OpenFeature adapter with default configuration
    When the developer resolves string flag "checkout-flow" through the adapter
    Then the adapter returns no string value
