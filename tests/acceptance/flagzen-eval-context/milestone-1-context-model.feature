Feature: Evaluation context model and explicit context resolution
  As a Java developer using FlagZen,
  I want to build evaluation contexts and pass them to flag resolution
  so that flags resolve based on user identity and attributes.

  # --- US-EC-01: EvaluationContext Builder ---

  @US-EC-01
  Scenario: Build evaluation context with targeting key and attributes
    Given a developer needs to target flags for user "user-7291"
    When the developer builds an evaluation context with targeting key "user-7291" and attributes:
      | attribute | value      |
      | plan      | enterprise |
      | region    | eu-west    |
    Then the context targeting key is "user-7291"
    And the context attribute "plan" is "enterprise"
    And the context attribute "region" is "eu-west"

  @US-EC-01
  Scenario: Build evaluation context without targeting key
    When the developer builds an evaluation context with only attribute "locale" = "de-DE"
    Then the context targeting key is absent
    And the context attribute "locale" is "de-DE"

  @US-EC-01
  Scenario: Build evaluation context with targeting key only
    When the developer builds an evaluation context with targeting key "session-abc" and no attributes
    Then the context targeting key is "session-abc"
    And the context has no attributes

  @US-EC-01 @error
  Scenario: Context attributes collection is never null
    When the developer builds an evaluation context with targeting key "user-1" and no attributes
    Then the context has an empty attributes collection, not null

  # --- US-EC-02: Explicit Context on FeatureDispatcher.resolve() ---

  @US-EC-02 @US-EC-03 @pending
  Scenario: Explicit context is forwarded to flag provider
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a flag provider that returns "PREMIUM" when context attribute "plan" is "enterprise"
    And an evaluation context with targeting key "user-vip-42" and attribute "plan" = "enterprise"
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the resolved proxy dispatches to the "PREMIUM" variant

  @US-EC-02 @pending
  Scenario: Resolve without context remains backward compatible
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    When the developer resolves "CheckoutFlow" without evaluation context
    Then the resolved proxy dispatches to the "CLASSIC" variant
    And behavior is identical to pre-context FlagZen

  @US-EC-02 @error @pending
  Scenario: Null context is treated as no context
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    When the developer resolves "CheckoutFlow" with null evaluation context
    Then the resolved proxy dispatches to the "CLASSIC" variant
    And the flag provider receives a contextless flag lookup

  # --- US-EC-03: FlagProvider Context-Aware Overload ---

  @US-EC-03 @pending
  Scenario: Existing flag provider ignores context via default method
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    And an evaluation context with targeting key "user-7291"
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the in-memory flag provider returns "CLASSIC" regardless of context
    And the resolved proxy dispatches to the "CLASSIC" variant

  @US-EC-03 @pending
  Scenario: Context-aware flag provider uses context for resolution
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a context-aware flag provider that returns "PREMIUM" for plan "enterprise"
    And an evaluation context with attribute "plan" = "enterprise"
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the resolved proxy dispatches to the "PREMIUM" variant

  @US-EC-03 @pending
  Scenario: Context-aware flag provider falls back when attribute missing
    Given a feature "CheckoutFlow" with variants "CLASSIC" and "PREMIUM"
    And a context-aware flag provider that returns "PREMIUM" for plan "enterprise"
    And an evaluation context with attribute "region" = "eu-west" but no "plan" attribute
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the resolved proxy dispatches to the default variant

  # --- US-EC-04: Generated Proxy Passes Context ---

  @US-EC-04 @pending
  Scenario: Generated proxy forwards context to flag provider
    Given a feature "CheckoutFlow" with flag key "checkout-flow"
    And a context-aware flag provider
    And an evaluation context with targeting key "user-7291"
    When the developer resolves "CheckoutFlow" with that evaluation context
    Then the flag provider receives both the flag key "checkout-flow" and the evaluation context

  @US-EC-04 @pending
  Scenario: Generated proxy works without context
    Given a feature "CheckoutFlow" with flag key "checkout-flow"
    And an in-memory flag provider with "checkout-flow" set to "CLASSIC"
    When the developer resolves "CheckoutFlow" without evaluation context
    Then the flag provider receives only the flag key "checkout-flow" without context
