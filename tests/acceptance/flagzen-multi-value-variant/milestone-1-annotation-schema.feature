Feature: Multi-value annotation schema for variant mapping
  As a Java developer using FlagZen,
  I want the @Variant annotation to accept arrays for string, int, and long values,
  so that I can map multiple flag values to one implementation without repeating annotations.

  # --- String array: happy path and backward compatibility ---

  @US-MV-01
  Scenario: Single string value still compiles without changes
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ModernCheckout" implementing "CheckoutFlow" for value "MODERN"
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "MODERN" to "ModernCheckout"

  @US-MV-01
  Scenario: Empty string in array is rejected at compile time
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and ""
    When the project compiles
    Then compilation fails with error containing "empty variant values are not permitted"

  # --- Int array: happy path and backward compatibility ---

  @US-MV-02
  Scenario: Multiple int values map to one implementation
    Given a feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a variant "BulkPricing" implementing "PricingTier" for int values 3 and 5
    And a variant "StandardPricing" implementing "PricingTier" for int value 1
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps int 3 to "BulkPricing"
    And the generated proxy maps int 5 to "BulkPricing"
    And the generated proxy maps int 1 to "StandardPricing"

  @US-MV-02
  Scenario: Single int value still compiles without changes
    Given a feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a variant "StandardPricing" implementing "PricingTier" for int value 42
    When the project compiles
    Then compilation succeeds

  @US-MV-02
  Scenario: Integer.MIN_VALUE is a valid int array value
    Given a feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a variant "EdgePricing" implementing "PricingTier" for int values 3 and -2147483648
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps int -2147483648 to "EdgePricing"

  # --- Long array: happy path and backward compatibility ---

  @US-MV-04
  Scenario: Multiple long values map to one implementation
    Given a feature interface "RateLimit" with flag key "rate-limit" and type LONG
    And a variant "ThrottledRate" implementing "RateLimit" for long values 1000 and 2000
    And a variant "UnlimitedRate" implementing "RateLimit" for long value 999999
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps long 1000 to "ThrottledRate"
    And the generated proxy maps long 2000 to "ThrottledRate"

  @US-MV-04
  Scenario: Single long value still compiles without changes
    Given a feature interface "RateLimit" with flag key "rate-limit" and type LONG
    And a variant "UnlimitedRate" implementing "RateLimit" for long value 999999
    When the project compiles
    Then compilation succeeds

  # --- Double multi-value (already array via @CloseTo[]) ---

  @US-MV-01
  Scenario: Multiple CloseTo values map to one implementation
    Given a feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a variant "SmallDiscount" implementing "DiscountRate" for double values 0.05 and 0.10
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps double approximately 0.05 to "SmallDiscount"
    And the generated proxy maps double approximately 0.10 to "SmallDiscount"

  # --- Composability: array values + repeated annotations ---

  @US-MV-05
  Scenario: Array values compose with a repeated single-value annotation
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a repeated variant annotation on "ClassicCheckout" for "CheckoutFlow" with value "RETRO"
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "CLASSIC" to "ClassicCheckout"
    And the generated proxy maps "LEGACY" to "ClassicCheckout"
    And the generated proxy maps "RETRO" to "ClassicCheckout"

  @US-MV-05
  Scenario: Two arrays on the same class compose all values
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a repeated variant annotation on "ClassicCheckout" for "CheckoutFlow" with values "RETRO" and "VINTAGE"
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "RETRO" to "ClassicCheckout"
    And the generated proxy maps "VINTAGE" to "ClassicCheckout"

  # --- Enum validation with multi-value arrays ---

  @US-MV-06
  Scenario: Multi-value satisfies REQUIRED fallback coverage
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and fallback REQUIRED
    And an inner Variant enum on "CheckoutFlow" with values CLASSIC, LEGACY, MODERN
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a variant "ModernCheckout" implementing "CheckoutFlow" for value "MODERN"
    When the project compiles
    Then compilation succeeds

  @US-MV-06
  Scenario: Invalid enum value in array is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And an inner Variant enum on "CheckoutFlow" with values CLASSIC, MODERN
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "INVALID"
    When the project compiles
    Then compilation fails with error containing "does not match any value in CheckoutFlow.Variant"

  @US-MV-06
  Scenario: Incomplete REQUIRED coverage despite multi-value reports missing variant
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and fallback REQUIRED
    And an inner Variant enum on "CheckoutFlow" with values CLASSIC, LEGACY, MODERN
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    When the project compiles
    Then compilation fails with error containing "MODERN"
    And the error mentions missing implementation
