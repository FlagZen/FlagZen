Feature: Runtime dispatch for multi-value variant mappings
  As a Java developer using FlagZen,
  I want the FeatureDispatcher to correctly dispatch to the matching variant
  when the flag value matches any value in a multi-value array,
  so that consolidated annotations work seamlessly at runtime.

  # --- String multi-value dispatch ---

  @US-MV-01
  Scenario: Flag value matching first array element dispatches correctly
    Given a compiled multi-value feature "CheckoutFlow" with flag key "checkout-flow"
    And "ClassicCheckout" mapped to string values "CLASSIC" and "LEGACY"
    And "ModernCheckout" mapped to string value "MODERN"
    And a flag provider returning "CLASSIC" for "checkout-flow"
    When the developer resolves "CheckoutFlow" through the multi-value dispatcher
    Then the "ClassicCheckout" variant handles the call

  @US-MV-01
  Scenario: Flag value matching second array element dispatches correctly
    Given a compiled multi-value feature "CheckoutFlow" with flag key "checkout-flow"
    And "ClassicCheckout" mapped to string values "CLASSIC" and "LEGACY"
    And "ModernCheckout" mapped to string value "MODERN"
    And a flag provider returning "LEGACY" for "checkout-flow"
    When the developer resolves "CheckoutFlow" through the multi-value dispatcher
    Then the "ClassicCheckout" variant handles the call

  @US-MV-01
  Scenario: Flag value matching single-value variant dispatches correctly
    Given a compiled multi-value feature "CheckoutFlow" with flag key "checkout-flow"
    And "ClassicCheckout" mapped to string values "CLASSIC" and "LEGACY"
    And "ModernCheckout" mapped to string value "MODERN"
    And a flag provider returning "MODERN" for "checkout-flow"
    When the developer resolves "CheckoutFlow" through the multi-value dispatcher
    Then the "ModernCheckout" variant handles the call

  # --- Int multi-value dispatch ---

  @US-MV-02
  Scenario: Int flag value matching any array element dispatches correctly
    Given a compiled multi-value feature "PricingTier" with flag key "pricing-tier" and type INT
    And "BulkPricing" mapped to int values 3 and 5
    And "StandardPricing" mapped to int value 1
    And a flag provider returning "5" for "pricing-tier"
    When the developer resolves "PricingTier" through the multi-value dispatcher
    Then the "BulkPricing" variant handles the call

  # --- Long multi-value dispatch ---

  @US-MV-04
  Scenario: Long flag value matching any array element dispatches correctly
    Given a compiled multi-value feature "RateLimit" with flag key "rate-limit" and type LONG
    And "ThrottledRate" mapped to long values 1000 and 2000
    And "UnlimitedRate" mapped to long value 999999
    And a flag provider returning "2000" for "rate-limit"
    When the developer resolves "RateLimit" through the multi-value dispatcher
    Then the "ThrottledRate" variant handles the call

  # --- Unmatched value with multi-value variants ---

  @US-MV-01
  Scenario: Unmatched flag value with multi-value variants triggers fallback
    Given a compiled multi-value feature "CheckoutFlow" with flag key "checkout-flow"
    And "ClassicCheckout" mapped to string values "CLASSIC" and "LEGACY"
    And "ModernCheckout" mapped to string value "MODERN"
    And the feature uses fallback strategy EXCEPTION
    And a flag provider returning "UNKNOWN" for "checkout-flow"
    When the developer resolves "CheckoutFlow" through the multi-value dispatcher expecting fallback
    Then an unmatched variant error is raised for the multi-value feature
