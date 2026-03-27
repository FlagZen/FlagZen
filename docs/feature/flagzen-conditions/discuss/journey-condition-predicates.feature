Feature: Condition Predicate-Based Variant Selection
  As a Java developer using FlagZen for polymorphic dispatch,
  I want to select variants based on flag value predicates
  so that I can implement declarative in-code Strategy pattern selection
  without an external flag provider.

  Background:
    Given FlagZen core with flag value support

  # --- Step 1: Define Predicate ---

  Scenario: Define a predicate using JDK Predicate<String>
    Given Kenji Tanaka creates a class Enterprise implementing Predicate<String>
    And the test method returns true when value equals "enterprise"
    When he compiles the predicate class
    Then it compiles successfully
    And it has an accessible no-arg constructor

  Scenario: Predicate receives flag value
    Given a predicate Enterprise implementing Predicate<String>
    And a flag value "enterprise"
    When Enterprise.test(value) is called
    Then it returns true

  Scenario: Predicate returns false for non-matching value
    Given a predicate Enterprise implementing Predicate<String>
    And a flag value "startup"
    When Enterprise.test(value) is called
    Then it returns false

  Scenario: Define a predicate using JDK IntPredicate
    Given Kenji Tanaka creates a class HighRetryRange implementing IntPredicate
    And the test method returns true when value >= 7
    When he compiles the predicate class
    Then it compiles successfully

  Scenario: Define a predicate using JDK DoublePredicate
    Given Kenji Tanaka creates a class HighThresholdRange implementing DoublePredicate
    And the test method returns true when value > 0.75
    When he compiles the predicate class
    Then it compiles successfully

  # --- Step 2: Annotate Variant with @Condition ---

  Scenario: Annotate variant with condition predicate
    Given a @Feature interface PricingStrategy with flag key "pricing-tier"
    When Kenji annotates EnterprisePricing with @Variant(when = @Condition(matches = Enterprise.class), order = 1)
    Then EnterprisePricing is a condition-based variant of PricingStrategy

  Scenario: Annotate variant with notMatches negation
    Given a @Feature interface PricingStrategy with flag key "pricing-tier"
    When Kenji annotates NonEnterprisePricing with @Variant(when = @Condition(notMatches = Enterprise.class), order = 2)
    Then NonEnterprisePricing activates when Enterprise.test(value) returns false

  Scenario: Multiple condition-based variants with distinct orders
    Given a @Feature interface PricingStrategy
    When Kenji annotates EnterprisePricing with @Variant(when = @Condition(matches = Enterprise.class), order = 1)
    And annotates StartupPricing with @Variant(when = @Condition(matches = Startup.class), order = 2)
    And marks StandardPricing as @DefaultVariant
    Then PricingStrategy has two condition-based variants and one default

  # --- Step 3: Compile-Time Validation ---

  Scenario: Compilation fails when predicate does not implement a JDK predicate interface
    Given a @Variant with @Condition(matches = NotAPredicate.class)
    And NotAPredicate does not implement Predicate<String>, IntPredicate, LongPredicate, or DoublePredicate
    When the annotation processor runs
    Then compilation fails
    And the error message is "NotAPredicate does not implement a supported predicate interface"

  Scenario: Compilation fails on duplicate order values within same feature
    Given @Feature PricingStrategy with two variants both having order = 1
    When the annotation processor runs
    Then compilation fails
    And the error message contains "Duplicate @Variant order"

  Scenario: Compilation fails when predicate lacks no-arg constructor
    Given a predicate class with only a parameterized constructor
    And @Variant(when = @Condition(matches = NoDefaultConstructor.class), order = 1)
    When the annotation processor runs
    Then compilation fails
    And the error message contains "must have an accessible no-arg constructor"

  Scenario: Compilation fails when matches and notMatches are both specified
    Given @Variant(when = @Condition(matches = Enterprise.class, notMatches = Startup.class), order = 1)
    When the annotation processor runs
    Then compilation fails
    And the error message contains "matches and notMatches are mutually exclusive"

  Scenario: Successful compilation generates proxy with predicate dispatch
    Given a valid @Feature PricingStrategy with condition-based variants
    When the annotation processor runs
    Then PricingStrategy_FlagZenProxy is generated
    And the generated proxy contains predicate evaluation logic
    And the generated proxy has zero java.lang.reflect imports

  Scenario: Exact matches and conditions coexist on the same @Feature
    Given @Feature PricingStrategy with:
      | Variant            | Type      | Value/Predicate | Order |
      | EnterprisePricing  | exact     | "enterprise"    |       |
      | HighRetryPricing   | condition | HighRetryRange  | 1     |
    And @DefaultVariant StandardPricing
    When the annotation processor runs
    Then compilation succeeds
    And PricingStrategy_FlagZenProxy supports both exact and condition dispatch

  # --- Step 4: Runtime Dispatch ---

  Scenario: First matching predicate wins
    Given @Feature PricingStrategy with:
      | Variant            | Predicate  | Order |
      | EnterprisePricing  | Enterprise | 1     |
      | StartupPricing     | Startup    | 2     |
    And @DefaultVariant StandardPricing
    And flag value "enterprise"
    When Kenji Tanaka resolves PricingStrategy via dispatcher
    Then EnterprisePricing is the active variant
    And Startup.test() was not called

  Scenario: Second predicate matches when first does not
    Given @Feature PricingStrategy with Enterprise (order 1) and Startup (order 2)
    And flag value "startup"
    When Kenji resolves PricingStrategy
    Then Enterprise.test() returns false
    And Startup.test() returns true
    And StartupPricing is the active variant

  Scenario: No predicate matches, @DefaultVariant selected
    Given @Feature PricingStrategy with condition-based variants
    And @DefaultVariant StandardPricing
    And flag value "free"
    When Kenji resolves PricingStrategy
    Then no predicate matches
    And StandardPricing is the active variant

  Scenario: No predicate matches, no default, EXCEPTION strategy
    Given @Feature PricingStrategy with fallback = FallbackStrategy.EXCEPTION
    And no @DefaultVariant
    And flag value "free"
    When Kenji resolves PricingStrategy
    Then UnmatchedVariantException is thrown
    And the exception message lists the available conditions

  Scenario: No predicate matches, no default, NOOP strategy
    Given @Feature PricingStrategy with fallback = FallbackStrategy.NOOP
    And no @DefaultVariant
    And flag value "free"
    When Kenji resolves PricingStrategy
    Then a NOOP proxy is returned
    And calling calculate() returns a safe default value

  Scenario: Unified dispatch: exact match checked before conditions
    Given @Feature PricingStrategy with:
      | Variant            | Type      | Value/Predicate | Order |
      | EnterprisePricing  | exact     | "enterprise"    |       |
      | HighRetryPricing   | condition | HighRetryRange  | 1     |
    And @DefaultVariant StandardPricing
    And flag value "enterprise"
    When Kenji resolves PricingStrategy
    Then EnterprisePricing is selected via exact match
    And HighRetryRange.test() is not called

  Scenario: Predicate dispatch re-evaluates on each method call
    Given a resolved PricingStrategy proxy
    And flag value changes between calls
    When calculate() is called with a new flag value
    Then predicates are re-evaluated against the new value
    And a different variant may be selected

  Scenario: Predicate exception propagates to caller
    Given @Feature PricingStrategy with predicate Enterprise that throws RuntimeException
    And flag value "enterprise"
    When the proxy evaluates Enterprise.test(value)
    Then the RuntimeException propagates to the caller
    And FlagZen does not swallow or wrap the exception

  @property
  Scenario: Predicate evaluation is deterministic
    Given the same flag value and the same set of condition-based variants
    When predicates are evaluated multiple times
    Then the same variant is always selected
