@walking-skeleton
Feature: Type-safe polymorphic dispatch for feature flags
  As a Java developer managing feature flags,
  I want to define features as typed interfaces and resolve the active variant at runtime,
  so that my flag-dependent code uses polymorphic dispatch instead of if/else conditionals.

  # Walking Skeleton 1: Compile-time feature definition and proxy generation
  # Covers: US-01, US-02, US-04 (thinnest slice through annotation processing)
  @US-01 @US-02 @US-04
  Scenario: Developer defines a feature with variants and a dispatch proxy is generated
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a method "execute" declared on "CheckoutFlow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for value "CLASSIC"
    And a variant "StreamlinedCheckout" implementing "CheckoutFlow" for value "STREAMLINED"
    When the project compiles
    Then compilation succeeds
    And a dispatch proxy "CheckoutFlow_FlagZenProxy" is generated
    And the proxy implements the "CheckoutFlow" interface

  # Walking Skeleton 2: Runtime resolution through FeatureDispatcher
  # Covers: US-05, US-06 (thinnest slice through runtime dispatch)
  @US-05 @US-06
  Scenario: Developer resolves a feature to the active variant at runtime
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "STREAMLINED"
    And the dispatcher is configured with this provider
    When the developer resolves "CheckoutFlow" through the dispatcher
    And calls "execute" on the resolved proxy
    Then the call is handled by the "StreamlinedCheckout" variant

  # Walking Skeleton 3: Pin flag values in tests with one annotation
  # Covers: US-07 (thinnest slice through testing support)
  @US-07
  Scenario: Developer pins a flag value in a test with a single annotation
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a test method annotated to pin "checkout-flow" to "PREMIUM"
    When the test resolves "CheckoutFlow"
    Then the resolved proxy delegates to "PremiumCheckout"
    And no flag provider setup was needed in the test
