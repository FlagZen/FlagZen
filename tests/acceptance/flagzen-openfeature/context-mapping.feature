Feature: Evaluation context mapping for targeted flag resolution
  As a Java developer needing per-user flag resolution through OpenFeature,
  I want my FlagZen evaluation context passed through to the OpenFeature provider,
  so targeting rules resolve the correct variant per user or segment.

  # --- US-OF-03: Context mapping happy paths ---

  @US-OF-03 @pending
  Scenario: Flag resolved with targeting key and string attribute
    Given the flag management service returns "EXPRESS" for "checkout-flow" when the targeting key is "user-7291"
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an evaluation context with targeting key "user-7291" and attribute "plan" set to "enterprise"
    When the developer resolves string flag "checkout-flow" with that evaluation context
    Then the adapter returns "EXPRESS"

  @US-OF-03 @pending
  Scenario: Flag resolved with numeric and boolean attributes
    Given the flag management service returns "PREMIUM" for "pricing-tier" when attribute "age" is 34
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an evaluation context with attribute "age" set to 34 and attribute "premium" set to true
    When the developer resolves string flag "pricing-tier" with that evaluation context
    Then the adapter returns "PREMIUM"

  @US-OF-03 @pending
  Scenario: Flag resolved without a targeting key
    Given the flag management service returns "EU_COMPLIANT" for "data-policy" when attribute "region" is "EU"
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an evaluation context with no targeting key and attribute "region" set to "EU"
    When the developer resolves string flag "data-policy" with that evaluation context
    Then the adapter returns "EU_COMPLIANT"

  # --- US-OF-03: Error and edge paths ---

  @US-OF-03 @pending
  Scenario: Unsupported attribute type is skipped during context mapping
    Given the flag management service has flag "checkout-flow" set to "EXPRESS"
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an evaluation context with an unsupported attribute type
    When the developer resolves string flag "checkout-flow" with that evaluation context
    Then the adapter returns "EXPRESS"
    And a warning is logged about the unsupported attribute type

  @US-OF-03 @pending
  Scenario: Context-aware typed resolution passes context through
    Given the flag management service returns true for boolean flag "beta-features" when the targeting key is "user-7291"
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an evaluation context with targeting key "user-7291"
    When the developer resolves boolean flag "beta-features" with that evaluation context
    Then the adapter returns boolean true

  @US-OF-03 @pending
  Scenario: Empty evaluation context does not disrupt flag resolution
    Given the flag management service has flag "checkout-flow" set to "EXPRESS"
    And the developer creates an OpenFeature adapter with that service
    And the developer builds an empty evaluation context
    When the developer resolves string flag "checkout-flow" with that evaluation context
    Then the adapter returns "EXPRESS"
