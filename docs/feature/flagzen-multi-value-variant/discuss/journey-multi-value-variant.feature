Feature: Multi-Value Variant Mapping
  As a Java developer using FlagZen
  I want to map multiple flag values to the same variant implementation
  So that I can reduce annotation boilerplate when consolidating flag values

  # --- String Multi-Value ---

  Scenario: String array maps multiple values to one implementation
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ClassicCheckout" annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
    And a class "ModernCheckout" annotated with @Variant(value = "MODERN", of = CheckoutFlow.class)
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "CLASSIC" to ClassicCheckout
    And the generated proxy maps "LEGACY" to ClassicCheckout
    And the generated proxy maps "MODERN" to ModernCheckout

  Scenario: Single string value still works (backward compatibility)
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ModernCheckout" annotated with @Variant(value = "MODERN", of = CheckoutFlow.class)
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "MODERN" to ModernCheckout

  Scenario: Duplicate string value across classes produces compile error
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ClassicCheckout" annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
    And a class "RetroCheckout" annotated with @Variant(value = {"RETRO", "LEGACY"}, of = CheckoutFlow.class)
    When the project compiles
    Then compilation fails with error containing "Duplicate @Variant(\"LEGACY\") for feature \"checkout-flow\""

  Scenario: Duplicate string value within same array produces compile error
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ClassicCheckout" annotated with @Variant(value = {"CLASSIC", "CLASSIC"}, of = CheckoutFlow.class)
    When the project compiles
    Then compilation fails with error containing "Duplicate @Variant(\"CLASSIC\") for feature \"checkout-flow\""

  # --- Int Multi-Value ---

  Scenario: Int array maps multiple values to one implementation
    Given a @Feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a class "BulkPricing" annotated with @Variant(intValue = {3, 5}, of = PricingTier.class)
    And a class "StandardPricing" annotated with @Variant(intValue = 1, of = PricingTier.class)
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps int 3 to BulkPricing
    And the generated proxy maps int 5 to BulkPricing
    And the generated proxy maps int 1 to StandardPricing

  Scenario: Duplicate int value across classes produces compile error
    Given a @Feature interface "PricingTier" with flag key "pricing-tier" and type INT
    And a class "BulkPricing" annotated with @Variant(intValue = {3, 5}, of = PricingTier.class)
    And a class "SpecialPricing" annotated with @Variant(intValue = {5, 7}, of = PricingTier.class)
    When the project compiles
    Then compilation fails with error containing "Duplicate @Variant(\"5\") for feature \"pricing-tier\""

  # --- Long Multi-Value ---

  Scenario: Long array maps multiple values to one implementation
    Given a @Feature interface "RateLimit" with flag key "rate-limit" and type LONG
    And a class "ThrottledRate" annotated with @Variant(longValue = {1000L, 2000L}, of = RateLimit.class)
    And a class "UnlimitedRate" annotated with @Variant(longValue = 999999L, of = RateLimit.class)
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps long 1000 to ThrottledRate
    And the generated proxy maps long 2000 to ThrottledRate

  Scenario: Duplicate long value across classes produces compile error
    Given a @Feature interface "RateLimit" with flag key "rate-limit" and type LONG
    And a class "ThrottledRate" annotated with @Variant(longValue = {1000L, 2000L}, of = RateLimit.class)
    And a class "LimitedRate" annotated with @Variant(longValue = {2000L, 3000L}, of = RateLimit.class)
    When the project compiles
    Then compilation fails with error containing "Duplicate @Variant(\"2000\") for feature \"rate-limit\""

  # --- Composability: Array + Repeated Annotations ---

  Scenario: Array values compose with repeated annotations
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ClassicCheckout" annotated with:
      | @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class) |
      | @Variant(value = "RETRO", of = CheckoutFlow.class)              |
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "CLASSIC" to ClassicCheckout
    And the generated proxy maps "LEGACY" to ClassicCheckout
    And the generated proxy maps "RETRO" to ClassicCheckout

  Scenario: Duplicate across array and repeated annotation produces compile error
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "ClassicCheckout" annotated with:
      | @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class) |
      | @Variant(value = "LEGACY", of = CheckoutFlow.class)              |
    When the project compiles
    Then compilation fails with error containing "Duplicate @Variant(\"LEGACY\") for feature \"checkout-flow\""

  # --- Variant Enum Validation with Multi-Value ---

  Scenario: All array values validated against inner Variant enum
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow" and inner enum Variant(CLASSIC, MODERN)
    And a class "ClassicCheckout" annotated with @Variant(value = {"CLASSIC", "INVALID"}, of = CheckoutFlow.class)
    When the project compiles
    Then compilation fails with error containing "does not match any value in CheckoutFlow.Variant"

  # --- REQUIRED Fallback with Multi-Value ---

  Scenario: REQUIRED fallback satisfied by multi-value covering all enum values
    Given a @Feature interface "CheckoutFlow" with flag key "checkout-flow", fallback REQUIRED, and inner enum Variant(CLASSIC, LEGACY, MODERN)
    And a class "ClassicCheckout" annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
    And a class "ModernCheckout" annotated with @Variant(value = "MODERN", of = CheckoutFlow.class)
    When the project compiles
    Then compilation succeeds

  # --- Double Multi-Value (already array via CloseTo[]) ---

  Scenario: Multiple CloseTo values map to one implementation
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = {@CloseTo(0.05), @CloseTo(0.10)}, of = DiscountRate.class)
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps double ~0.05 to SmallDiscount
    And the generated proxy maps double ~0.10 to SmallDiscount

  # --- Runtime Dispatch ---

  Scenario: Runtime dispatch works for all mapped string values
    Given a compiled feature "CheckoutFlow" with flag key "checkout-flow"
    And ClassicCheckout mapped to values "CLASSIC" and "LEGACY"
    And ModernCheckout mapped to value "MODERN"
    When the flag provider returns "LEGACY" for key "checkout-flow"
    Then the FeatureDispatcher returns the ClassicCheckout implementation

  Scenario: Runtime dispatch works for all mapped int values
    Given a compiled feature "PricingTier" with flag key "pricing-tier" and type INT
    And BulkPricing mapped to int values 3 and 5
    And StandardPricing mapped to int value 1
    When the flag provider returns 5 for key "pricing-tier"
    Then the FeatureDispatcher returns the BulkPricing implementation

  # --- @CloseTo Overlapping Range Detection ---

  Scenario: Overlapping @CloseTo ranges across variants produces compile error
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.05), of = DiscountRate.class)
    And a class "MediumDiscount" annotated with @Variant(doubleValue = @CloseTo(value = 0.12, delta = 0.05), of = DiscountRate.class)
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges for feature \"discount-rate\""
    And the error message names "SmallDiscount" and "MediumDiscount"
    And the error message shows the overlapping ranges

  Scenario: Non-overlapping @CloseTo ranges across variants accepted
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.01), of = DiscountRate.class)
    And a class "LargeDiscount" annotated with @Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01), of = DiscountRate.class)
    When the project compiles
    Then compilation succeeds

  Scenario: Overlapping @CloseTo ranges within same variant array produces compile error
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = {@CloseTo(value = 0.1, delta = 0.05), @CloseTo(value = 0.12, delta = 0.05)}, of = DiscountRate.class)
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges within variant SmallDiscount"

  Scenario: Non-overlapping @CloseTo ranges within same variant array accepted
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = {@CloseTo(0.1), @CloseTo(0.5)}, of = DiscountRate.class)
    When the project compiles
    Then compilation succeeds

  Scenario: Overlapping @CloseTo ranges with default delta detected
    Given a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
    And a class "SmallDiscount" annotated with @Variant(doubleValue = @CloseTo(0.1), of = DiscountRate.class)
    And a class "MediumDiscount" annotated with @Variant(doubleValue = @CloseTo(0.100001), of = DiscountRate.class)
    When the project compiles
    Then compilation fails with error containing "Overlapping @CloseTo ranges"
