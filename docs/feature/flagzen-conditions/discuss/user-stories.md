<!-- markdownlint-disable MD024 -->

# User Stories: Condition Predicates (flagzen-conditions)

## US-CP-01: JDK Predicate Contract for Flag Values

### Problem

Kenji Tanaka is a senior Java developer who uses FlagZen for polymorphic dispatch. He wants to select variants based on flag values like pricing tier or retry count, but there is no contract for expressing these conditions. He currently has to write manual if/else chains outside FlagZen, defeating the purpose of declarative dispatch.

### Who

- Java developer | Uses FlagZen polymorphic dispatch | Wants declarative condition-based variant selection

### Solution

Use standard JDK predicate interfaces (`Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`) as the contract for all condition predicates. Predicates test the flag value, not EvaluationContext.

### Domain Examples

#### 1: String-based predicate -- Kenji writes Enterprise for enterprise customers

Kenji creates `Enterprise implements Predicate<String>`. The `test` method checks `"enterprise".equals(value)`. He instantiates it with a no-arg constructor and calls `test` with flag value "enterprise". It returns true.

#### 2: Int-based predicate -- Kenji writes HighRetryRange for retry thresholds

Kenji creates `HighRetryRange implements IntPredicate`. The `test` method checks `value >= 7`. For flag value 10, it returns true. For flag value 3, false.

#### 3: Double-based predicate -- Kenji writes HighThresholdRange for confidence thresholds

Kenji creates `HighThresholdRange implements DoublePredicate`. The `test` method checks `value > 0.75`. For flag value 0.9, it returns true. For 0.5, false.

### UAT Scenarios (BDD)

#### Scenario: Predicate<String> returns true for matching flag value

Given Kenji creates Enterprise implementing Predicate<String>
And a flag value "enterprise"
When he calls enterprise.test(value)
Then it returns true

#### Scenario: Predicate<String> returns false for non-matching flag value

Given Kenji creates Enterprise implementing Predicate<String>
And a flag value "startup"
When he calls enterprise.test(value)
Then it returns false

#### Scenario: IntPredicate returns true for matching flag value

Given Kenji creates HighRetryRange implementing IntPredicate
And a flag value 10
When he calls highRetryRange.test(value)
Then it returns true

#### Scenario: Predicate handles null value gracefully

Given Kenji creates Enterprise implementing Predicate<String>
And a null flag value
When he calls enterprise.test(null)
Then it returns false without throwing an exception

### Acceptance Criteria

- [ ] Supported predicate interfaces: `Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`
- [ ] Predicates test the flag value, not EvaluationContext
- [ ] Can be implemented as a lambda, method reference, or named class
- [ ] No restrictions on predicate logic -- pure user-defined function

### Outcome KPIs

- **Who**: Java developers writing condition predicates
- **Does what**: Implement a predicate in one class with one method
- **By how much**: Uses familiar JDK interfaces -- zero new types to learn
- **Measured by**: API surface -- standard JDK interfaces, zero FlagZen-specific predicate types
- **Baseline**: No predicate contract exists

### Technical Notes

- Uses standard JDK functional interfaces, not a custom FeaturePredicate type
- Predicates test the flag value (String, int, long, double), not EvaluationContext
- Zero runtime dependencies beyond flagzen-core
- Best practice: predicates should be fast (sub-millisecond). FlagZen evaluates predicates on every method call. Slow predicates degrade application performance -- this is the developer's responsibility, not FlagZen's concern.

---

## US-CP-02: @Condition Annotation Definition

### Problem

Kenji Tanaka has a JDK predicate implementation but no way to declaratively bind it to a @Variant. He needs an annotation that references the predicate class, so the proxy knows which predicates to check. Evaluation order is controlled by `order` on @Variant.

### Who

- Java developer | Has JDK predicate implementations | Wants declarative binding to @Variant

### Solution

A `@Condition` annotation with `matches` (predicate class reference) or `notMatches` (negated predicate class reference), used inside `@Variant(when = @Condition(...))`. `matches` and `notMatches` are mutually exclusive. The `order` attribute lives on `@Variant`, not on `@Condition`.

### Domain Examples

#### 1: Single condition -- Kenji binds Enterprise to EnterprisePricing

Kenji writes `@Variant(when = @Condition(matches = Enterprise.class), order = 1)` on `EnterprisePricing`. The annotation declares that this variant activates when `Enterprise.test(value)` returns true, and it is evaluated first.

#### 2: Multiple conditions with ordering -- Kenji sets up a priority chain

Kenji writes three variants: `@Condition(matches = Enterprise.class)` with order 1, `@Condition(matches = Startup.class)` with order 2, `@Condition(matches = InternalUser.class)` with order 3. Enterprise customers are checked first, then startups, then internal users.

#### 3: Negated condition -- Kenji uses notMatches

Kenji writes `@Variant(when = @Condition(notMatches = Enterprise.class), order = 2)` on `NonEnterprisePricing`. This variant activates when `Enterprise.test(value)` returns false.

### UAT Scenarios (BDD)

#### Scenario: @Condition annotation compiles with matches attribute

Given Kenji writes @Variant(when = @Condition(matches = Enterprise.class), order = 1)
When the source file is compiled
Then compilation succeeds
And @Condition is retained through annotation processing

#### Scenario: @Condition with notMatches compiles

Given Kenji writes @Variant(when = @Condition(notMatches = Enterprise.class), order = 1)
When the source file is compiled
Then compilation succeeds

#### Scenario: @Variant without when attribute works unchanged

Given an existing @Variant("PREMIUM") without when attribute
When compiled with the new @Condition support
Then it behaves identically to M0 (value-based dispatch)

### Acceptance Criteria

- [ ] @Condition annotation with `matches` (Class<? extends Predicate/IntPredicate/etc.>) attribute
- [ ] @Condition annotation with `notMatches` as negation (mutually exclusive with `matches`)
- [ ] @Condition is used inside @Variant's `when` attribute
- [ ] `order` attribute on @Variant, not on @Condition
- [ ] @Variant.when defaults to "no condition" for backward compatibility
- [ ] Retention: CLASS (needed by annotation processor, not at runtime)
- [ ] Target: implicitly scoped via @Variant (applies to TYPE)

### Outcome KPIs

- **Who**: Java developers declaring condition-based variants
- **Does what**: Bind a predicate to a variant in one annotation
- **By how much**: Single line of annotation vs manual dispatch code
- **Measured by**: Annotation expressiveness -- reads as English: "variant when condition matches Enterprise"
- **Baseline**: No declarative condition binding exists

### Technical Notes

- @Condition is a nested annotation type, not standalone -- used exclusively within @Variant(when = ...)
- The `when` attribute on @Variant should have a default value representing "no condition" (e.g., @Condition with a sentinel class)
- `order` on @Variant is int, not annotation ordering -- explicit control over evaluation sequence
- `order` is optional when unambiguous
- Depends on: US-CP-01 (JDK predicate type reference)

---

## US-CP-03: @Condition Annotation Model in Processor

### Problem

Kenji Tanaka has declared `@Condition` on his variants, but the annotation processor does not yet understand the `when` attribute. The processor needs an internal model to represent condition metadata alongside variant metadata for validation and code generation.

### Who

- Java developer | Compiles @Feature with @Condition variants | Expects the processor to understand condition metadata

### Solution

Extend the annotation processor's internal model (VariantModel) to capture condition metadata: predicate class reference (from `matches` or `notMatches`) and order value (from @Variant).

### Domain Examples

#### 1: Processor reads condition from EnterprisePricing variant

The processor encounters `@Variant(when = @Condition(matches = Enterprise.class), order = 1)` on `EnterprisePricing`. It creates a VariantModel with predicateClass = "com.example.Enterprise", negated = false, and order = 1.

#### 2: Processor reads value-based variant unchanged

The processor encounters `@Variant("PREMIUM")` on `PremiumCheckout` (no `when` attribute). The VariantModel has predicateClass = null and order = 0 (no condition).

#### 3: Processor collects all variants for a feature with unified dispatch

The processor collects VariantModels for PricingStrategy: EnterprisePricing (exact match "enterprise"), HighRetryPricing (condition, order 1), StandardPricing (default). Exact matches and conditions coexist on the same feature.

### UAT Scenarios (BDD)

#### Scenario: Processor extracts predicate class from @Condition matches

Given @Variant(when = @Condition(matches = Enterprise.class), order = 1) on EnterprisePricing
When the annotation processor processes EnterprisePricing
Then the VariantModel contains predicate class reference "com.example.Enterprise"
And negated = false
And order value 1

#### Scenario: Processor extracts predicate class from @Condition notMatches

Given @Variant(when = @Condition(notMatches = Enterprise.class), order = 2) on NonEnterprisePricing
When the annotation processor processes NonEnterprisePricing
Then the VariantModel contains predicate class reference "com.example.Enterprise"
And negated = true
And order value 2

#### Scenario: Processor handles unified dispatch with exact matches and conditions

Given @Feature PricingStrategy has exact-match variants and condition-based variants
When the processor processes PricingStrategy
Then exact-match variants have value metadata
And condition variants have predicate metadata
And both coexist in the same FeatureModel

#### Scenario: Processor orders condition variants by order attribute

Given three condition variants with orders 3, 1, 2
When the processor collects them
Then they are sorted as order 1, 2, 3 for code generation

### Acceptance Criteria

- [ ] VariantModel extended with optional predicate class reference, negated flag, and order
- [ ] Processor correctly reads @Condition's matches/notMatches attributes from annotation mirror
- [ ] Order read from @Variant, not from @Condition
- [ ] Condition variants sorted by order for code generation
- [ ] Value-based variants unchanged -- no condition metadata
- [ ] Exact matches and conditions coexist in the same FeatureModel

### Outcome KPIs

- **Who**: Java developers compiling projects with condition-based variants
- **Does what**: Annotation processor correctly models condition metadata
- **By how much**: Zero processor errors on valid @Condition usage
- **Measured by**: Processor unit tests with condition, value-based, and unified variant combinations
- **Baseline**: Processor has no condition awareness

### Technical Notes

- Uses javax.lang.model.element API to read annotation values
- Predicate class stored as TypeMirror (compile-time), not Class (runtime)
- Depends on: US-CP-02 (@Condition annotation exists to read)

---

## US-CP-04: Compile-Time Predicate Type Validation

### Problem

Kenji Tanaka references `Enterprise.class` in `@Condition(matches = ...)`, but if `Enterprise` does not implement a JDK predicate interface, the code would fail at runtime with a confusing ClassCastException. He expects the annotation processor to catch this at compile time.

### Who

- Java developer | References predicate class in @Condition | Expects compile-time safety

### Solution

The annotation processor validates that the class referenced by `@Condition(matches = ...)` or `@Condition(notMatches = ...)` implements one of the supported JDK predicate interfaces (`Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`) and has an accessible no-arg constructor.

### Domain Examples

#### 1: Valid predicate -- Enterprise implements Predicate<String>

Kenji's `Enterprise implements Predicate<String>` is referenced in `@Condition(matches = Enterprise.class)`. The processor verifies the implements relationship. Compilation succeeds.

#### 2: Invalid predicate -- NotAPredicate does not implement a JDK predicate interface

Kenji accidentally references `NotAPredicate.class` which is a plain POJO. The processor emits: `error: NotAPredicate does not implement a supported predicate interface`. Compilation fails.

#### 3: Missing no-arg constructor -- Predicate requires DI

Kenji's `DatabasePredicate` has only `DatabasePredicate(DataSource ds)`. Without Spring module, the processor emits: `error: DatabasePredicate must have an accessible no-arg constructor (or use flagzen-spring for DI)`. Compilation fails.

### UAT Scenarios (BDD)

#### Scenario: Valid predicate compiles successfully

Given @Condition(matches = Enterprise.class) where Enterprise implements Predicate<String>
When the annotation processor runs
Then no errors are emitted for Enterprise

#### Scenario: Non-predicate class fails compilation

Given @Condition(matches = NotAPredicate.class) where NotAPredicate does not implement a JDK predicate interface
When the annotation processor runs
Then compilation fails with "NotAPredicate does not implement a supported predicate interface"

#### Scenario: Predicate without no-arg constructor fails compilation

Given @Condition(matches = DatabasePredicate.class) with no no-arg constructor
When the annotation processor runs
Then compilation fails with message containing "accessible no-arg constructor"

#### Scenario: Abstract predicate class fails compilation

Given @Condition(matches = AbstractPredicate.class) where AbstractPredicate is abstract
When the annotation processor runs
Then compilation fails with message indicating the class cannot be instantiated

#### Scenario: matches and notMatches both specified fails compilation

Given @Condition(matches = Enterprise.class, notMatches = Startup.class)
When the annotation processor runs
Then compilation fails with "matches and notMatches are mutually exclusive"

### Acceptance Criteria

- [ ] Processor validates @Condition(matches/notMatches) class implements a JDK predicate interface
- [ ] Processor validates @Condition(matches/notMatches) class has accessible no-arg constructor
- [ ] Processor validates @Condition(matches/notMatches) class is not abstract
- [ ] Processor validates matches and notMatches are not both specified
- [ ] Error messages are specific and actionable
- [ ] Validation does not interfere with value-based @Variant processing

### Outcome KPIs

- **Who**: Java developers referencing predicate classes in @Condition
- **Does what**: Get compile-time error for invalid predicate references
- **By how much**: 100% of invalid predicates caught at compile time (vs. runtime ClassCastException)
- **Measured by**: Processor error tests covering all invalid predicate scenarios
- **Baseline**: No compile-time validation for predicates

### Technical Notes

- Use javax.lang.model.util.Types.isAssignable() for implements check
- Constructor check via ElementFilter.constructorsIn() on the type element
- Depends on: US-CP-01 (JDK predicate interfaces), US-CP-03 (processor reads @Condition)

---

## US-CP-05: Order Uniqueness Validation

### Problem

Kenji Tanaka might accidentally assign the same order value to two variants, making evaluation order ambiguous. This must be caught at compile time.

### Who

- Java developer | Declares multiple condition-based variants | Expects compile-time protection against ambiguous configurations

### Solution

The annotation processor validates that no duplicate order values exist within the same @Feature.

### Domain Examples

#### 1: Duplicate order values -- Kenji assigns order 1 twice

Kenji writes `@Variant(when = @Condition(matches = Enterprise.class), order = 1)` on EnterprisePricing and `@Variant(when = @Condition(matches = Startup.class), order = 1)` on StartupPricing. Processor emits: `error: Duplicate @Variant order 1 on PricingStrategy`. Compilation fails.

#### 2: Valid unified dispatch -- Exact matches and conditions coexist

Kenji writes `@Variant("enterprise")` on EnterprisePricing (exact match) and `@Variant(when = @Condition(matches = HighRetryRange.class), order = 1)` on HighRetryPricing, both implementing PricingStrategy. Both compile successfully because exact matches and conditions coexist via unified dispatch.

#### 3: Valid separate features -- Different features with different configurations

Kenji has `@Feature("checkout-flow")` with value-based variants and `@Feature("pricing-tier")` with condition-based variants. Both compile successfully.

### UAT Scenarios (BDD)

#### Scenario: Duplicate order values rejected

Given @Feature PricingStrategy with two variants both having order = 1
When the annotation processor runs
Then compilation fails with "Duplicate @Variant order 1 on PricingStrategy"

#### Scenario: Exact matches and conditions coexist on same feature

Given @Feature PricingStrategy with @Variant("enterprise") and @Variant(when = @Condition(matches = HighRetryRange.class), order = 1)
When the annotation processor runs
Then compilation succeeds

#### Scenario: Separate features with different configurations compile

Given @Feature CheckoutFlow with value-based variants
And @Feature PricingStrategy with condition-based variants
When the annotation processor runs
Then both features compile successfully

#### Scenario: Non-sequential order values are valid

Given @Feature PricingStrategy with condition orders 10, 20, 30
When the annotation processor runs
Then compilation succeeds (order values need not be sequential)

### Acceptance Criteria

- [ ] Duplicate order values within same @Feature produce compile error
- [ ] Exact matches and conditions can coexist on the same @Feature (unified dispatch)
- [ ] Different @Feature interfaces can use different configurations
- [ ] Error messages identify the @Feature and conflicting elements
- [ ] Non-sequential order values (e.g., 10, 20, 30) are valid

### Outcome KPIs

- **Who**: Java developers declaring multiple condition-based variants
- **Does what**: Get compile-time error for ambiguous configurations
- **By how much**: 100% of order conflicts caught at compile time
- **Measured by**: Processor error tests for each invalid combination
- **Baseline**: No order validation exists

### Technical Notes

- Order uniqueness: collect all order values per FeatureModel, check for duplicates
- Exact matches and conditions coexist -- no mixing detection needed
- @DefaultVariant is compatible with both modes (not counted as either)
- Depends on: US-CP-03 (processor models condition metadata)

---

## US-CP-06: Proxy Generation for Predicate Dispatch

### Problem

Kenji Tanaka has valid condition-based variants that pass compile-time validation, but the proxy generator only knows how to emit value-based dispatch (map lookup from flag provider string). He needs the generator to produce a dispatch path that evaluates predicates against the flag value.

### Who

- Java developer | Has compiled condition-based @Feature | Expects generated proxy to dispatch via predicates

### Solution

Extend ProxyGenerator to emit predicate-based dispatch logic: instantiate predicates, evaluate in order against the flag value, delegate to first matching variant. Exact matches and conditions coexist in the same proxy via unified dispatch.

### Domain Examples

#### 1: Enterprise pricing selected -- Kenji resolves with enterprise flag value

Kenji has `@Feature("pricing-tier")` with Enterprise (order 1) and Startup (order 2). The flag provider returns "enterprise". The generated proxy calls `Enterprise.test("enterprise")` which returns true. EnterprisePricing is selected. Startup is never evaluated.

#### 2: Startup pricing selected -- Kenji resolves with startup flag value

Same feature. Flag value is "startup". Proxy calls `Enterprise.test("startup")` -> false, then `Startup.test("startup")` -> true. StartupPricing is selected.

#### 3: No match, default selected -- Kenji resolves with free-tier flag value

Flag value is "free". Both predicates return false. Proxy falls through to `@DefaultVariant StandardPricing`.

### UAT Scenarios (BDD)

#### Scenario: Generated proxy evaluates predicates in order

Given @Feature PricingStrategy with Enterprise (order 1) and Startup (order 2)
And flag value "enterprise"
When Kenji resolves PricingStrategy
Then the proxy evaluates Enterprise.test("enterprise") first
And returns EnterprisePricing

#### Scenario: Proxy stops at first matching predicate

Given @Feature PricingStrategy with Enterprise (order 1) and Startup (order 2)
And flag value "enterprise"
When the proxy dispatches
Then Startup.test(value) is not called

#### Scenario: Proxy uses @DefaultVariant when no predicate matches

Given @Feature PricingStrategy with condition-based variants and @DefaultVariant StandardPricing
And flag value "free"
When the proxy dispatches
Then both predicates return false
And StandardPricing is selected

#### Scenario: Proxy re-evaluates predicates on each method call

Given Kenji has a resolved PricingStrategy proxy reference
When he calls calculate() with flag value "enterprise"
And then calls calculate() with flag value "startup"
Then the first call dispatches to EnterprisePricing
And the second call dispatches to StartupPricing

#### Scenario: Predicate exception propagates to caller

Given @Feature PricingStrategy with a predicate Enterprise that throws RuntimeException
And flag value "enterprise"
When the proxy evaluates Enterprise.test(value)
Then the RuntimeException propagates to the caller
And FlagZen does not swallow or wrap the exception

#### Scenario: Generated proxy has zero reflection imports

Given the generated PricingStrategy_FlagZenProxy source
When inspecting its imports
Then no java.lang.reflect imports are present

### Acceptance Criteria

- [ ] ProxyGenerator emits predicate dispatch path for condition-based @Feature
- [ ] Predicates evaluated in @Variant order attribute sequence
- [ ] First matching predicate selects the variant
- [ ] Subsequent predicates not evaluated after match (short-circuit)
- [ ] Predicates re-evaluated on each method call (dynamic dispatch)
- [ ] Predicate exceptions propagate to caller (FlagZen does not catch or wrap)
- [ ] Generated code has zero java.lang.reflect imports
- [ ] Exact matches and conditions coexist in the same proxy (unified dispatch)

### Outcome KPIs

- **Who**: Java developers with condition-based features
- **Does what**: Get automatic predicate dispatch without writing dispatch logic
- **By how much**: Zero lines of dispatch code written by the developer
- **Measured by**: Generated proxy source inspection
- **Baseline**: Proxy generator only supports value-based dispatch

### Technical Notes

- Predicates instantiated once at proxy construction (no-arg constructor), reused per dispatch
- Flag value obtained from FlagProvider per method call
- For value-based features: generated proxy is unchanged from M0
- Proxy supports unified dispatch: exact matches checked first, then conditions by order
- Depends on: US-CP-01, US-CP-02, US-CP-03, US-CP-04 (all validation must pass before generation)

---

## US-CP-07: Fallback Behavior for Condition Dispatch

### Problem

Kenji Tanaka has a condition-based @Feature where no predicate matches and no @DefaultVariant exists. The behavior must be consistent with existing FallbackStrategy semantics -- but the meaning of REQUIRED needs clarification for condition-based features (there is no enum coverage concept for predicates).

### Who

- Java developer | Has condition-based feature with unmatched flag value | Expects consistent fallback behavior

### Solution

FallbackStrategy for condition-based features: EXCEPTION throws UnmatchedVariantException, NOOP returns safe defaults, REQUIRED requires @DefaultVariant at compile time (since predicate completeness cannot be statically verified).

### Domain Examples

#### 1: EXCEPTION strategy, no match, no default -- Kenji gets clear error

Kenji's PricingStrategy has `fallback = FallbackStrategy.EXCEPTION` and no @DefaultVariant. Flag value is "free", no predicates match. `UnmatchedVariantException` is thrown with message listing the available predicates: "No condition matched for pricing-tier. Predicates: [Enterprise(1), Startup(2)]."

#### 2: NOOP strategy, no match, no default -- Kenji gets safe default

Kenji's PricingStrategy has `fallback = FallbackStrategy.NOOP`. No predicates match. The proxy returns safe defaults: `calculate()` returns `null` (or Money zero if return type allows).

#### 3: REQUIRED strategy enforces @DefaultVariant at compile time

Kenji's PricingStrategy has `fallback = FallbackStrategy.REQUIRED` (the default). The processor requires @DefaultVariant because predicate completeness cannot be proven at compile time. Without @DefaultVariant, compilation fails: "REQUIRED strategy on condition-based feature pricing-tier requires @DefaultVariant."

### UAT Scenarios (BDD)

#### Scenario: EXCEPTION strategy throws when no predicate matches

Given @Feature PricingStrategy with fallback = EXCEPTION and no @DefaultVariant
And flag value where no predicate matches
When Kenji resolves PricingStrategy
Then UnmatchedVariantException is thrown
And the exception message lists available predicates

#### Scenario: NOOP strategy returns safe defaults when no predicate matches

Given @Feature PricingStrategy with fallback = NOOP and no @DefaultVariant
And flag value where no predicate matches
When Kenji resolves PricingStrategy and calls calculate()
Then a safe default value is returned

#### Scenario: REQUIRED strategy demands @DefaultVariant at compile time

Given @Feature PricingStrategy with fallback = REQUIRED (default)
And condition-based variants but no @DefaultVariant
When the annotation processor runs
Then compilation fails with "REQUIRED strategy on condition-based feature requires @DefaultVariant"

#### Scenario: REQUIRED strategy with @DefaultVariant compiles

Given @Feature PricingStrategy with fallback = REQUIRED
And condition-based variants and @DefaultVariant StandardPricing
When the annotation processor runs
Then compilation succeeds

#### Scenario: No flag value falls through to fallback

Given a condition-based @Feature with @DefaultVariant
And no flag value available
When Kenji resolves the feature
Then @DefaultVariant is selected (predicates cannot match without a value)

### Acceptance Criteria

- [ ] EXCEPTION: throws UnmatchedVariantException with predicate list in message
- [ ] NOOP: returns safe default values (consistent with M0 NOOP behavior)
- [ ] REQUIRED: compilation fails without @DefaultVariant on condition-based features
- [ ] @DefaultVariant takes priority over FallbackStrategy (same as M0)
- [ ] Missing flag value falls through to @DefaultVariant or FallbackStrategy

### Outcome KPIs

- **Who**: Java developers with condition-based features in edge cases
- **Does what**: Handle unmatched conditions consistently with existing FallbackStrategy
- **By how much**: Zero new concepts -- same three strategies, same behavior
- **Measured by**: Fallback tests mirror existing M0 fallback tests for value-based dispatch
- **Baseline**: FallbackStrategy exists for value-based dispatch only

### Technical Notes

- REQUIRED interpretation for conditions is the key design decision: since predicates are runtime-evaluated, "completeness" cannot be verified statically, so @DefaultVariant is mandatory
- This differs from value-based REQUIRED where enum coverage IS verifiable
- Exception messages should include predicate class names and order for debuggability
- Depends on: US-CP-06 (proxy dispatch generates fallback paths)

---

## US-CP-08: Spring DI for Predicate Instantiation

### Problem

Kenji Tanaka's predicate needs a database connection or a service client to evaluate. The no-arg constructor requirement means he cannot inject dependencies. When flagzen-spring is on the classpath, predicates should be instantiated via Spring's DI container instead.

### Who

- Java developer | Uses Spring Boot | Predicate needs injected dependencies

### Solution

When flagzen-spring is on the classpath, predicates annotated with `@Component` (or any Spring stereotype) are resolved from the ApplicationContext instead of via no-arg constructor.

### Domain Examples

#### 1: Predicate with injected service -- Kenji's PremiumCustomer checks the database

Kenji writes `@Component class PremiumCustomer implements Predicate<String>`. It has `PremiumCustomer(CustomerRepository repo)` constructor. Spring injects the repository. The predicate queries `repo.findById(targetingKey)` and checks the plan field.

#### 2: Non-Spring environment -- No-arg constructor still works

Tomas Bergstrom does not use Spring. His `Enterprise` has a no-arg constructor and checks flag values only. It works identically to US-CP-01 through US-CP-07.

#### 3: Mixed predicates -- Some Spring-managed, some plain

Kenji has `@Component class PremiumCustomer` (Spring-managed) and `class EuRegion` (no-arg constructor, no Spring annotation). Both work: PremiumCustomer resolved from ApplicationContext, EuRegion instantiated via constructor.

### UAT Scenarios (BDD)

#### Scenario: Spring-managed predicate resolved from ApplicationContext

Given flagzen-spring is on the classpath
And PremiumCustomer is annotated with @Component
And PremiumCustomer has a constructor with CustomerRepository parameter
When the proxy needs to evaluate PremiumCustomer
Then it is resolved from the Spring ApplicationContext
And the CustomerRepository is injected

#### Scenario: Non-Spring predicate uses no-arg constructor

Given flagzen-spring is not on the classpath
And Enterprise has a no-arg constructor
When the proxy needs to evaluate Enterprise
Then it is instantiated via no-arg constructor

#### Scenario: Mixed Spring and plain predicates coexist

Given flagzen-spring is on the classpath
And PremiumCustomer is @Component with injected dependencies
And EuRegion has a no-arg constructor and no @Component
When the proxy evaluates both predicates
Then PremiumCustomer is resolved from ApplicationContext
And EuRegion is instantiated via no-arg constructor

### Acceptance Criteria

- [ ] When flagzen-spring is present, @Component predicates resolved from ApplicationContext
- [ ] When flagzen-spring is absent, no-arg constructor instantiation (existing behavior)
- [ ] Mixed predicates (some Spring, some plain) work within the same @Feature
- [ ] Compile-time validation relaxes no-arg constructor check for @Component predicates

### Outcome KPIs

- **Who**: Spring Boot developers with predicates needing injected dependencies
- **Does what**: Use constructor injection in predicates without workarounds
- **By how much**: Zero workarounds (no service locator, no static fields)
- **Measured by**: Spring integration tests with injected predicates
- **Baseline**: All predicates require no-arg constructor

### Technical Notes

- This extends flagzen-spring module, not flagzen-core
- Proxy receives a predicate factory (either constructor-based or ApplicationContext-based)
- The compile-time no-arg constructor check must be skipped when the class has @Component (or @Service, @Repository, etc.)
- Depends on: US-CP-06 (proxy dispatch), M4 (flagzen-spring module)
- Could be deferred to a later milestone if M4 is not yet started
