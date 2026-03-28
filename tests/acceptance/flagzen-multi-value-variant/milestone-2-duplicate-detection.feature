Feature: Compile-time duplicate detection across multi-value arrays
  As a Java developer using FlagZen,
  I want the compiler to catch duplicate variant values across multi-value arrays,
  so that I discover mapping conflicts at compile time rather than at runtime.

  # --- String duplicate detection ---

  @US-MV-03
  Scenario: Duplicate string value across different classes is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a variant "RetroCheckout" implementing "CheckoutFlow" for string values "RETRO" and "LEGACY"
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "LEGACY" as the conflicting value
    And the error names both "ClassicCheckout" and "RetroCheckout"

  @US-MV-03
  Scenario: Duplicate string value within the same array is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "CLASSIC"
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "CLASSIC" as the conflicting value

  @US-MV-03 @US-MV-05
  Scenario: Duplicate between array and repeated annotation is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a repeated variant annotation on "ClassicCheckout" for "CheckoutFlow" with value "LEGACY"
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "LEGACY" as the conflicting value

  @US-MV-03
  Scenario: Different string values across classes compile successfully
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a variant "ModernCheckout" implementing "CheckoutFlow" for string values "MODERN" and "CURRENT"
    When the project compiles
    Then compilation succeeds

  # --- Int duplicate detection ---

  @US-MV-03
  Scenario: Duplicate int value across different classes is rejected
    Given a feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a variant "BulkPricing" implementing "PricingTier" for int values 3 and 5
    And a variant "SpecialPricing" implementing "PricingTier" for int values 5 and 7
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "5" as the conflicting value

  # --- Long duplicate detection ---

  @US-MV-03
  Scenario: Duplicate long value across different classes is rejected
    Given a feature interface "RateLimit" with flag key "rate-limit" and type LONG
    And a variant "ThrottledRate" implementing "RateLimit" for long values 1000 and 2000
    And a variant "LimitedRate" implementing "RateLimit" for long values 2000 and 3000
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "2000" as the conflicting value

  @US-MV-03
  Scenario: Duplicate int value within the same array is rejected
    Given a feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a variant "BulkPricing" implementing "PricingTier" for int values 5 and 5
    When the project compiles
    Then compilation fails with error containing "Duplicate"
    And the error identifies "5" as the conflicting value
