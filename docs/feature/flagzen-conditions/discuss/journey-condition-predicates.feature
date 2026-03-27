Feature: Condition Predicate-Based Variant Selection
  As a Java developer using FlagZen for polymorphic dispatch,
  I want to select variants based on runtime context predicates
  so that I can implement declarative in-code Strategy pattern selection
  without an external flag provider.

  Background:
    Given FlagZen core with EvaluationContext support (M1)

  # --- Step 1: Define FeaturePredicate ---

  Scenario: Define a feature predicate with no-arg constructor
    Given Kenji Tanaka creates a class IsEnterprise implementing FeaturePredicate
    And the test method returns true when ctx.attribute("plan") equals "enterprise"
    When he compiles the predicate class
    Then it compiles successfully
    And it has an accessible no-arg constructor

  Scenario: Predicate receives EvaluationContext
    Given a predicate IsEnterprise implementing FeaturePredicate
    And an EvaluationContext with attribute "plan" = "enterprise"
    When IsEnterprise.test(ctx) is called
    Then it returns true

  Scenario: Predicate returns false for non-matching context
    Given a predicate IsEnterprise implementing FeaturePredicate
    And an EvaluationContext with attribute "plan" = "startup"
    When IsEnterprise.test(ctx) is called
    Then it returns false

  # --- Step 2: Annotate Variant with @Condition ---

  Scenario: Annotate variant with condition predicate
    Given a @Feature interface PricingStrategy with flag key "pricing-tier"
    When Kenji annotates EnterprisePricing with @Variant(when = @Condition(on = IsEnterprise.class, order = 1))
    Then EnterprisePricing is a condition-based variant of PricingStrategy

  Scenario: Multiple condition-based variants with distinct orders
    Given a @Feature interface PricingStrategy
    When Kenji annotates EnterprisePricing with order 1
    And annotates StartupPricing with order 2
    And marks StandardPricing as @DefaultVariant
    Then PricingStrategy has two condition-based variants and one default

  # --- Step 3: Compile-Time Validation ---

  Scenario: Compilation fails when predicate does not implement FeaturePredicate
    Given a @Variant with @Condition(on = NotAPredicate.class, order = 1)
    And NotAPredicate does not implement FeaturePredicate
    When the annotation processor runs
    Then compilation fails
    And the error message is "NotAPredicate does not implement FeaturePredicate"

  Scenario: Compilation fails on duplicate order values within same feature
    Given @Feature PricingStrategy with two variants both having order = 1
    When the annotation processor runs
    Then compilation fails
    And the error message contains "Duplicate @Condition order"

  Scenario: Compilation fails when mixing value-based and condition-based variants
    Given @Feature PricingStrategy
    And @Variant("PREMIUM") on PremiumPricing (value-based)
    And @Variant(when = @Condition(on = IsEnterprise.class, order = 1)) on EnterprisePricing
    When the annotation processor runs
    Then compilation fails
    And the error message contains "mixes value-based and condition-based variants"

  Scenario: Compilation fails when predicate lacks no-arg constructor
    Given a predicate class with only a parameterized constructor
    And @Variant(when = @Condition(on = NoDefaultConstructor.class, order = 1))
    When the annotation processor runs
    Then compilation fails
    And the error message contains "must have an accessible no-arg constructor"

  Scenario: Successful compilation generates proxy with predicate dispatch
    Given a valid @Feature PricingStrategy with condition-based variants
    When the annotation processor runs
    Then PricingStrategy_FlagZenProxy is generated
    And the generated proxy contains predicate evaluation logic
    And the generated proxy has zero java.lang.reflect imports

  # --- Step 4: Runtime Dispatch ---

  Scenario: First matching predicate wins
    Given @Feature PricingStrategy with:
      | Variant            | Predicate    | Order |
      | EnterprisePricing  | IsEnterprise | 1     |
      | StartupPricing     | IsStartup   | 2     |
    And @DefaultVariant StandardPricing
    And EvaluationContext with attribute "plan" = "enterprise"
    When Kenji Tanaka resolves PricingStrategy via dispatcher
    Then EnterprisePricing is the active variant
    And IsStartup.test() was not called

  Scenario: Second predicate matches when first does not
    Given @Feature PricingStrategy with IsEnterprise (order 1) and IsStartup (order 2)
    And EvaluationContext with attribute "plan" = "startup"
    When Kenji resolves PricingStrategy
    Then IsEnterprise.test() returns false
    And IsStartup.test() returns true
    And StartupPricing is the active variant

  Scenario: No predicate matches, @DefaultVariant selected
    Given @Feature PricingStrategy with condition-based variants
    And @DefaultVariant StandardPricing
    And EvaluationContext with attribute "plan" = "free"
    When Kenji resolves PricingStrategy
    Then no predicate matches
    And StandardPricing is the active variant

  Scenario: No predicate matches, no default, EXCEPTION strategy
    Given @Feature PricingStrategy with fallback = FallbackStrategy.EXCEPTION
    And no @DefaultVariant
    And EvaluationContext with attribute "plan" = "free"
    When Kenji resolves PricingStrategy
    Then UnmatchedVariantException is thrown
    And the exception message lists the available conditions

  Scenario: No predicate matches, no default, NOOP strategy
    Given @Feature PricingStrategy with fallback = FallbackStrategy.NOOP
    And no @DefaultVariant
    And EvaluationContext with attribute "plan" = "free"
    When Kenji resolves PricingStrategy
    Then a NOOP proxy is returned
    And calling calculate() returns a safe default value

  Scenario: Predicate evaluated with EvaluationContext from block scope
    Given @Feature PricingStrategy with condition-based variants
    And Kenji wraps the resolve call in FlagContext.run(ctx, ...)
    When the proxy evaluates predicates
    Then predicates receive the block-scoped EvaluationContext

  Scenario: Predicate evaluated with explicit EvaluationContext
    Given @Feature PricingStrategy with condition-based variants
    When Kenji calls dispatcher.resolve(PricingStrategy.class, explicitContext)
    Then predicates receive the explicit EvaluationContext

  Scenario: No EvaluationContext available for condition-based feature
    Given @Feature PricingStrategy with condition-based variants and @DefaultVariant
    And no EvaluationContext is available (no explicit, no accessor, no scope, no default)
    When Kenji resolves PricingStrategy
    Then predicates receive a null or empty context
    And @DefaultVariant is selected (no predicate can match without context)

  Scenario: Predicate dispatch re-evaluates on each method call
    Given a resolved PricingStrategy proxy
    And EvaluationContext changes between calls (e.g., via FlagContext.run)
    When calculate() is called in a new scope with different context
    Then predicates are re-evaluated against the new context
    And a different variant may be selected

  Scenario: Predicate exception propagates to caller
    Given @Feature PricingStrategy with predicate IsEnterprise that throws RuntimeException
    And EvaluationContext with attribute "plan" = "enterprise"
    When the proxy evaluates IsEnterprise.test(ctx)
    Then the RuntimeException propagates to the caller
    And FlagZen does not swallow or wrap the exception

  @property
  Scenario: Predicate evaluation is deterministic
    Given the same EvaluationContext and the same set of condition-based variants
    When predicates are evaluated multiple times
    Then the same variant is always selected
