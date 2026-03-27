@walking-skeleton
Feature: Targeted flag resolution with evaluation context
  As a Java developer using FlagZen for polymorphic dispatch,
  I want to pass evaluation context to flag resolution
  so that flags resolve differently per user for A/B testing and segmentation.

  # Walking Skeleton 1: Build context, pass to resolve, provider receives it
  # Covers: US-EC-01, US-EC-02, US-EC-03 (thinnest slice through context-aware dispatch)
  @US-EC-01 @US-EC-02 @US-EC-03 @pending
  Scenario: Developer resolves a feature with per-user evaluation context
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that returns "PREMIUM" when targeting key is "user-vip-42"
    And an evaluation context with targeting key "user-vip-42" and attribute "plan" = "enterprise"
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the resolved proxy dispatches to the "PREMIUM" variant
    And the flag provider received the evaluation context with targeting key "user-vip-42"

  # Walking Skeleton 2: Block-scoped context applies to multiple resolve calls
  # Covers: US-EC-05, US-EC-02, US-EC-03 (thinnest slice through scoped context)
  @US-EC-05 @US-EC-02 @pending
  Scenario: Developer scopes evaluation context to a block of code
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a feature "PaymentMethod" with variants "CARD" and "INVOICE"
    And a flag provider that uses targeting key for resolution
    And an evaluation context with targeting key "maria-santos-1042" and attribute "plan" = "enterprise"
    When the developer wraps two resolve calls inside a scoped context block
    Then both "CheckoutFlow" and "PaymentMethod" are resolved using targeting key "maria-santos-1042"

  # Walking Skeleton 3: Context resolution order with all sources
  # Covers: US-EC-07, US-EC-06 (thinnest slice through resolution chain)
  @US-EC-07 @US-EC-06 @pending
  Scenario: Explicit context takes precedence over all other context sources
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that uses targeting key for resolution
    And a context accessor returning targeting key "accessor-user"
    And a scoped context with targeting key "scoped-user"
    And an explicit evaluation context with targeting key "explicit-user"
    When the developer resolves "CheckoutFlow" with the explicit context
    Then the flag provider received targeting key "explicit-user"
    And the context accessor was not consulted
