@milestone-1
Feature: Compile-time validation of features and variants
  As a Java developer defining feature flags,
  I want the annotation processor to catch configuration errors at compile time,
  so that typos, missing variants, and structural mistakes never reach runtime.

  # --- US-01: @Feature annotation ---

  @US-01
  Scenario: Feature defined without variant enum accepts free-form values
    Given a feature interface "DarkMode" with flag key "dark-mode"
    And no inner Variant enum is defined on "DarkMode"
    And a variant "DarkModeOn" implementing "DarkMode" for value "on"
    When the project compiles
    Then compilation succeeds
    And "on" is accepted as a valid variant value

  @US-01
  Scenario: Feature annotation rejected on a class
    Given a class "CheckoutService" annotated as a feature with key "checkout-flow"
    When the project compiles
    Then compilation fails
    And the error message states "@Feature can only be applied to interfaces"

  @US-01
  Scenario: Feature with fallback strategy records configuration
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and fallback strategy REQUIRED
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    And a variant "StreamlinedCheckout" implementing "CheckoutFlow" for value "STREAMLINED"
    And a variant "PremiumCheckout" implementing "CheckoutFlow" for value "PREMIUM"
    When the project compiles
    Then compilation succeeds
    And the fallback strategy REQUIRED is recorded for "checkout-flow"

  # --- US-02: @Variant annotation ---

  @US-02
  Scenario: Variant class that does not implement the feature interface is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "BrokenVariant" annotated as variant "CLASSIC" but not implementing "CheckoutFlow"
    When the project compiles
    Then compilation fails
    And the error states the variant class must implement the feature interface

  @US-02
  Scenario: Multi-feature variant registered for both features
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a feature interface "PaymentMethod" with flag key "payment-method"
    And a class "PremiumCreditCheckout" annotated for "CheckoutFlow" with value "PREMIUM" and for "PaymentMethod" with value "CREDIT_CARD"
    And "PremiumCreditCheckout" implements both "CheckoutFlow" and "PaymentMethod"
    When the project compiles
    Then compilation succeeds
    And "PremiumCreditCheckout" is registered for both "checkout-flow" and "payment-method"

  @US-02
  Scenario: Default variant registered as fallback
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a class "DefaultCheckout" annotated as the default variant implementing "CheckoutFlow"
    When the project compiles
    Then compilation succeeds
    And "DefaultCheckout" is registered as the fallback for "checkout-flow"

  # --- US-03: Compile-time variant value validation ---

  @US-03 @pending
  Scenario: Variant value not in enum is rejected at compile time
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And an inner Variant enum with values "CLASSIC", "STREAMLINED", "PREMIUM"
    And a variant "TurboCheckout" implementing "CheckoutFlow" for value "TURBO"
    When the project compiles
    Then compilation fails
    And the error states "TURBO" is not a valid value for "CheckoutFlow"
    And the error lists valid values: "CLASSIC", "STREAMLINED", "PREMIUM"

  @US-03 @pending
  Scenario: Duplicate variant value for the same feature is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    And a variant "LegacyCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    When the project compiles
    Then compilation fails
    And the error identifies both "ClassicCheckout" and "LegacyCheckout" as conflicting

  @US-03 @pending
  Scenario: REQUIRED strategy with incomplete variant coverage is rejected
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and fallback strategy REQUIRED
    And an inner Variant enum with values "CLASSIC", "STREAMLINED", "PREMIUM"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    And a variant "StreamlinedCheckout" implementing "CheckoutFlow" for value "STREAMLINED"
    When the project compiles
    Then compilation fails
    And the error lists "PREMIUM" as missing an implementation

  @US-03 @pending
  Scenario: REQUIRED strategy satisfied by a default variant
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow" and fallback strategy REQUIRED
    And an inner Variant enum with values "CLASSIC", "STREAMLINED", "PREMIUM"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    And a variant "StreamlinedCheckout" implementing "CheckoutFlow" for value "STREAMLINED"
    And a default variant "DefaultCheckout" implementing "CheckoutFlow"
    When the project compiles
    Then compilation succeeds

  # --- US-04: Proxy generation ---

  @US-04 @pending
  Scenario: Generated proxy provides a descriptive identity
    Given a compiled feature "CheckoutFlow" with flag key "checkout-flow"
    And the dispatch proxy "CheckoutFlow_FlagZenProxy" has been generated
    When the developer inspects the proxy's string representation
    Then it shows "FlagZenProxy[checkout-flow]"

  @US-04 @pending
  Scenario: Generated proxy contains no runtime reflection
    Given a compiled feature "CheckoutFlow" with flag key "checkout-flow"
    And the dispatch proxy "CheckoutFlow_FlagZenProxy" has been generated
    When the developer inspects the generated source code
    Then it contains no reflection imports
    And dispatch uses direct method calls or map lookups

  @US-04 @property @pending
  Scenario: Every feature interface produces exactly one proxy
    Given any valid feature interface with at least one variant
    When the project compiles
    Then exactly one proxy class is generated per feature interface
    And each proxy implements its corresponding feature interface
