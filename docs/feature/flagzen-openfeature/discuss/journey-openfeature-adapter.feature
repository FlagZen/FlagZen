Feature: OpenFeature SDK Adapter for FlagZen

  As a Java developer using OpenFeature with an existing provider (Flagd, CloudBees, Split),
  I want FlagZen to resolve flags through the OpenFeature SDK,
  so I can use polymorphic dispatch without replacing my flag infrastructure.

  Background:
    Given an OpenFeature provider is registered with the global API
    And a @Feature interface "checkout-flow" is defined with variants "CLASSIC" and "EXPRESS"

  # --- Step 2: Configure Provider ---

  Scenario: ServiceLoader discovers OpenFeatureFlagProvider automatically
    Given the flagzen-openfeature JAR is on the classpath
    And no FlagProvider is configured programmatically
    When FlagZen discovers FlagProvider implementations via ServiceLoader
    Then OpenFeatureFlagProvider is loaded
    And it uses the global OpenFeature client

  Scenario: Explicit construction with a named client
    Given Ricardo creates an OpenFeature client named "payments"
    When he constructs OpenFeatureFlagProvider with that client
    And creates a FeatureDispatcher with that provider
    Then the dispatcher resolves flags through the "payments" client

  # --- Step 3: Resolve String Flags ---

  Scenario: String flag resolved through OpenFeature
    Given the OpenFeature provider returns "EXPRESS" for flag key "checkout-flow"
    When Ricardo resolves the CheckoutFlow feature
    Then the ExpressCheckout variant is dispatched

  Scenario: Flag not found in OpenFeature returns empty
    Given the OpenFeature provider has no value for flag key "checkout-flow"
    When Ricardo calls getString("checkout-flow") on the adapter
    Then the result is Optional.empty()

  Scenario: OpenFeature evaluation error returns empty
    Given the OpenFeature provider returns an error for flag key "checkout-flow"
    When Ricardo calls getString("checkout-flow") on the adapter
    Then the result is Optional.empty()
    And FlagZen's fallback strategy activates

  # --- Step 3b: Typed Flag Resolution ---

  Scenario: Boolean flag resolved through OpenFeature native boolean
    Given the OpenFeature provider returns true for boolean flag "dark-mode"
    When Ricardo calls getBoolean("dark-mode") on the adapter
    Then the result is Optional.of(true)
    And the adapter used client.getBooleanDetails() (not string parsing)

  Scenario: Integer flag resolved through OpenFeature native integer
    Given the OpenFeature provider returns 42 for integer flag "max-retries"
    When Ricardo calls getInt("max-retries") on the adapter
    Then the result is OptionalInt.of(42)

  Scenario: Double flag resolved through OpenFeature native double
    Given the OpenFeature provider returns 0.75 for double flag "rollout-percentage"
    When Ricardo calls getDouble("rollout-percentage") on the adapter
    Then the result is OptionalDouble.of(0.75)

  # --- Step 4: Resolve with Evaluation Context ---

  Scenario: EvaluationContext mapped from FlagZen to OpenFeature
    Given the OpenFeature provider returns "EXPRESS" for "checkout-flow" when targeting key is "user-7291"
    And Ricardo builds a FlagZen EvaluationContext with targetingKey "user-7291" and attribute "plan" = "enterprise"
    When Ricardo calls getString("checkout-flow", context) on the adapter
    Then the adapter maps the FlagZen context to an OpenFeature EvaluationContext
    And the OpenFeature context has targetingKey "user-7291"
    And the OpenFeature context has attribute "plan" with String value "enterprise"
    And the result is Optional.of("EXPRESS")

  Scenario: Null targeting key omitted from OpenFeature context
    Given Ricardo builds a FlagZen EvaluationContext with no targeting key and attribute "region" = "EU"
    When the adapter maps this to an OpenFeature EvaluationContext
    Then the OpenFeature context has no targeting key set
    And the OpenFeature context has attribute "region" with String value "EU"

  # --- Error Paths ---

  Scenario: Unsupported attribute type logged and skipped
    Given Ricardo builds a FlagZen EvaluationContext with attribute "custom" = a java.time.Instant value
    When the adapter maps this to an OpenFeature EvaluationContext
    Then the "custom" attribute is omitted from the OpenFeature context
    And a warning is logged about the unsupported attribute type

  Scenario: OpenFeature client not initialized
    Given no OpenFeature provider has been registered
    When Ricardo constructs OpenFeatureFlagProvider with no-arg constructor
    And calls getString("checkout-flow")
    Then the result is Optional.empty()
