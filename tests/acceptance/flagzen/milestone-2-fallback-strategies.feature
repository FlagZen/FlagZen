@milestone-2
Feature: Runtime dispatch and fallback strategies
  As a Java developer using feature flags at runtime,
  I want configurable behavior when a flag value has no matching variant,
  so that I can choose between loud failure, graceful degradation, or compile-time completeness.

  # --- US-05: Runtime resolution ---

  @US-05
  Scenario: Proxy follows runtime flag value changes
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And the flag provider returns "CLASSIC" for "checkout-flow"
    And the developer has resolved "CheckoutFlow" through the dispatcher
    When the flag provider value changes to "PREMIUM"
    And the developer calls "execute" on the same proxy
    Then the call is handled by the "PremiumCheckout" variant

  @US-05
  Scenario: Dispatcher returns the same proxy instance for repeated resolutions
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    When the developer resolves "CheckoutFlow" through the dispatcher twice
    Then both resolutions return the same proxy instance

  @US-05 @pending
  Scenario: Resolution fails clearly when no flag provider is configured
    Given no flag provider is configured
    When the developer resolves "CheckoutFlow" through the dispatcher
    Then a configuration error is raised
    And the message states no flag provider is configured
    And the message suggests how to add one

  # --- US-06: FlagProvider SPI ---

  @US-06 @pending
  Scenario: In-memory flag provider serves flag values for development
    Given an in-memory flag provider with "checkout-flow" set to "STREAMLINED"
    And the dispatcher is configured with this provider
    When the developer resolves "CheckoutFlow" through the dispatcher
    And calls "execute" on the resolved proxy
    Then the call is handled by the "StreamlinedCheckout" variant

  @US-06 @pending
  Scenario: Flag provider registered programmatically via configuration API
    Given a custom flag provider that returns "PREMIUM" for "checkout-flow"
    When the developer configures the dispatcher with this provider
    And resolves "CheckoutFlow" through the dispatcher
    Then the resolved proxy delegates to "PremiumCheckout"

  @US-06 @pending
  Scenario: Flag provider returns no value for an unknown flag key
    Given an in-memory flag provider with no flags configured
    And the feature "DarkMode" uses fallback strategy EXCEPTION
    When the developer resolves "DarkMode" and calls a method
    Then an unmatched variant error is raised
    And the message indicates no flag value was found for "dark-mode"

  # --- US-09: Fallback strategies ---

  @US-09 @pending
  Scenario: EXCEPTION strategy throws on unmatched variant value
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC", "STREAMLINED", "PREMIUM"
    And "CheckoutFlow" uses fallback strategy EXCEPTION
    And the flag provider returns "BETA" for "checkout-flow"
    When the developer calls "execute" on the resolved proxy
    Then an unmatched variant error is raised
    And the error message lists known variants: "CLASSIC", "STREAMLINED", "PREMIUM"

  @US-09 @pending
  Scenario: NOOP strategy returns safe defaults for unmatched variant
    Given a compiled feature "DarkMode" with a void method "apply" and a boolean method "isEnabled"
    And "DarkMode" uses fallback strategy NOOP
    And the flag provider returns "midnight" for "dark-mode" with no matching variant
    When the developer calls "apply" on the resolved proxy
    Then no exception is thrown and the method does nothing
    When the developer calls "isEnabled" on the resolved proxy
    Then the result is false

  @US-09 @pending
  Scenario: Default variant handles unmatched values before fallback strategy
    Given a compiled feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And "CheckoutFlow" uses fallback strategy EXCEPTION
    And a default variant "DefaultCheckout" is registered for "CheckoutFlow"
    And the flag provider returns "BETA" for "checkout-flow"
    When the developer calls "execute" on the resolved proxy
    Then the call is handled by "DefaultCheckout"
    And no exception is thrown

  @US-09 @property @pending
  Scenario: NOOP fallback never throws regardless of flag value
    Given any feature configured with fallback strategy NOOP
    And any flag value that does not match a known variant
    When any method is called on the resolved proxy
    Then no exception is thrown
    And return values are safe defaults for their types
