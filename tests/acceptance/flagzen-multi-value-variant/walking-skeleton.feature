@walking-skeleton
Feature: Multi-value variant mapping for feature flags
  As a Java developer using FlagZen,
  I want to map multiple flag values to the same variant implementation using array syntax,
  so that I can consolidate variant annotations and reduce boilerplate.

  # Walking Skeleton 1: Compile-time -- string array annotation compiles and registers all values
  # Covers: US-MV-01 (thinnest slice through annotation schema change + processor array expansion + proxy generation)
  @US-MV-01
  Scenario: Developer maps multiple string values to one variant implementation
    Given a feature interface "CheckoutFlow" with flag key "checkout-flow"
    And a variant "ClassicCheckout" implementing "CheckoutFlow" for string values "CLASSIC" and "LEGACY"
    And a variant "ModernCheckout" implementing "CheckoutFlow" for value "MODERN"
    When the project compiles
    Then compilation succeeds
    And the generated proxy maps "CLASSIC" to "ClassicCheckout"
    And the generated proxy maps "LEGACY" to "ClassicCheckout"
    And the generated proxy maps "MODERN" to "ModernCheckout"

  # Walking Skeleton 2: Runtime dispatch -- flag value matching any array value dispatches correctly
  # Covers: US-MV-01 (runtime dispatch through FeatureDispatcher for multi-value string)
  @US-MV-01
  Scenario: Developer resolves a multi-value string feature to the matching variant at runtime
    Given a compiled multi-value feature "CheckoutFlow" with flag key "checkout-flow"
    And "ClassicCheckout" mapped to string values "CLASSIC" and "LEGACY"
    And "ModernCheckout" mapped to string value "MODERN"
    And a flag provider returning "LEGACY" for "checkout-flow"
    When the developer resolves "CheckoutFlow" through the multi-value dispatcher
    Then the "ClassicCheckout" variant handles the call
