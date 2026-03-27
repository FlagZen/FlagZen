<!-- markdownlint-disable MD024 -->

# User Stories: Condition Predicates (flagzen-conditions)

## US-CP-01: FeaturePredicate Functional Interface

### Problem

Kenji Tanaka is a senior Java developer who uses FlagZen for polymorphic dispatch. He wants to select variants based on runtime properties like user plan or region, but there is no contract for expressing these conditions. He currently has to write manual if/else chains outside FlagZen, defeating the purpose of declarative dispatch.

### Who

- Java developer | Uses FlagZen polymorphic dispatch | Wants declarative condition-based variant selection

### Solution

A `FeaturePredicate` functional interface with a single `boolean test(EvaluationContext ctx)` method, serving as the contract for all condition predicates.

### Domain Examples

#### 1: Plan-based predicate -- Kenji writes IsEnterprise for enterprise customers

Kenji creates `IsEnterprise implements FeaturePredicate`. The `test` method checks `"enterprise".equals(ctx.attribute("plan"))`. He instantiates it with a no-arg constructor and calls `test` with an EvaluationContext containing attribute "plan" = "enterprise". It returns true.

#### 2: Region-based predicate -- Kenji writes IsEuRegion for GDPR-affected users

Kenji creates `IsEuRegion implements FeaturePredicate`. The `test` method checks if `ctx.attribute("region")` is in the set `{"eu-west", "eu-central", "eu-north"}`. For an EvaluationContext with attribute "region" = "eu-west", it returns true. For "us-east", false.

#### 3: Multi-attribute predicate -- Kenji writes IsBetaTester combining plan and flag

Kenji creates `IsBetaTester implements FeaturePredicate`. The `test` method checks `"beta".equals(ctx.attribute("program"))` AND the targeting key is not null. For context with targeting key "user-7291" and attribute "program" = "beta", it returns true. For context with null targeting key, it returns false.

### UAT Scenarios (BDD)

#### Scenario: FeaturePredicate has single test method

Given the FeaturePredicate interface definition
When Kenji Tanaka inspects its API
Then it has exactly one abstract method: `boolean test(EvaluationContext ctx)`
And it is annotated with @FunctionalInterface

#### Scenario: Predicate returns true for matching context

Given Kenji creates IsEnterprise implementing FeaturePredicate
And an EvaluationContext with attribute "plan" = "enterprise"
When he calls isEnterprise.test(ctx)
Then it returns true

#### Scenario: Predicate returns false for non-matching context

Given Kenji creates IsEnterprise implementing FeaturePredicate
And an EvaluationContext with attribute "plan" = "startup"
When he calls isEnterprise.test(ctx)
Then it returns false

#### Scenario: Predicate handles null attribute gracefully

Given Kenji creates IsEnterprise implementing FeaturePredicate
And an EvaluationContext with no "plan" attribute
When he calls isEnterprise.test(ctx)
Then it returns false without throwing an exception

### Acceptance Criteria

- [ ] FeaturePredicate is a @FunctionalInterface with `boolean test(EvaluationContext ctx)`
- [ ] Interface is in com.flagzen package
- [ ] Can be implemented as a lambda, method reference, or named class
- [ ] No restrictions on predicate logic -- pure user-defined function

### Outcome KPIs

- **Who**: Java developers writing condition predicates
- **Does what**: Implement a predicate in one class with one method
- **By how much**: Single interface to learn -- same pattern as java.util.function.Predicate
- **Measured by**: API surface -- one interface, one method, zero configuration
- **Baseline**: No predicate contract exists

### Technical Notes

- Must depend on EvaluationContext from M1 (US-EC-01)
- @FunctionalInterface annotation for lambda support
- No generic type parameter -- always takes EvaluationContext (not Predicate<T>)
- Zero runtime dependencies beyond flagzen-core
- Best practice: predicates should be fast (sub-millisecond). FlagZen evaluates predicates on every method call. Slow predicates degrade application performance -- this is the developer's responsibility, not FlagZen's concern.

---

## US-CP-02: @Condition Annotation Definition

### Problem

Kenji Tanaka has a FeaturePredicate implementation but no way to declaratively bind it to a @Variant. He needs an annotation that references the predicate class and specifies evaluation order, so the proxy knows which predicates to check and in what sequence.

### Who

- Java developer | Has FeaturePredicate implementations | Wants declarative binding to @Variant

### Solution

A `@Condition` annotation with `on` (predicate class reference) and `order` (evaluation sequence) attributes, used inside `@Variant(when = @Condition(...))`.

### Domain Examples

#### 1: Single condition -- Kenji binds IsEnterprise to EnterprisePricing

Kenji writes `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))` on `EnterprisePricing`. The annotation declares that this variant activates when `IsEnterprise.test(ctx)` returns true, and it is evaluated first.

#### 2: Multiple conditions with ordering -- Kenji sets up a priority chain

Kenji writes three variants: `@Condition(on = IsEnterprise.class, order = 1)`, `@Condition(on = IsStartup.class, order = 2)`, `@Condition(on = IsInternalUser.class, order = 3)`. Enterprise customers are checked first, then startups, then internal users.

#### 3: Condition with @DefaultVariant fallback -- Kenji provides a catch-all

Kenji adds `@DefaultVariant` on `StandardPricing` alongside two condition-based variants. If neither predicate matches, `StandardPricing` is used.

### UAT Scenarios (BDD)

#### Scenario: @Condition annotation compiles with valid attributes

Given Kenji writes @Variant(when = @Condition(on = IsEnterprise.class, order = 1))
When the source file is compiled
Then compilation succeeds
And @Condition is retained through annotation processing

#### Scenario: @Condition on attribute references a Class

Given @Condition(on = IsEnterprise.class, order = 1)
When the annotation processor reads the "on" attribute
Then it resolves to the IsEnterprise class type

#### Scenario: @Variant without when attribute works unchanged

Given an existing @Variant("PREMIUM") without when attribute
When compiled with the new @Condition support
Then it behaves identically to M0 (value-based dispatch)

### Acceptance Criteria

- [ ] @Condition annotation with `on` (Class<? extends FeaturePredicate>) and `order` (int) attributes
- [ ] @Condition is used inside @Variant's `when` attribute
- [ ] @Variant.when defaults to "no condition" for backward compatibility
- [ ] Retention: CLASS (needed by annotation processor, not at runtime)
- [ ] Target: implicitly scoped via @Variant (applies to TYPE)

### Outcome KPIs

- **Who**: Java developers declaring condition-based variants
- **Does what**: Bind a predicate to a variant in one annotation
- **By how much**: Single line of annotation vs manual dispatch code
- **Measured by**: Annotation expressiveness -- reads as English: "variant when condition on IsEnterprise"
- **Baseline**: No declarative condition binding exists

### Technical Notes

- @Condition is a nested annotation type, not standalone -- used exclusively within @Variant(when = ...)
- The `when` attribute on @Variant should have a default value representing "no condition" (e.g., @Condition with a sentinel class)
- order attribute is int, not annotation ordering -- explicit control over evaluation sequence
- Depends on: US-CP-01 (FeaturePredicate type reference)

---

## US-CP-03: @Condition Annotation Model in Processor

### Problem

Kenji Tanaka has declared `@Condition` on his variants, but the annotation processor does not yet understand the `when` attribute. The processor needs an internal model to represent condition metadata alongside variant metadata for validation and code generation.

### Who

- Java developer | Compiles @Feature with @Condition variants | Expects the processor to understand condition metadata

### Solution

Extend the annotation processor's internal model (VariantModel) to capture condition metadata: predicate class reference and order value.

### Domain Examples

#### 1: Processor reads condition from EnterprisePricing variant

The processor encounters `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))` on `EnterprisePricing`. It creates a VariantModel with predicateClass = "com.example.IsEnterprise" and order = 1.

#### 2: Processor reads value-based variant unchanged

The processor encounters `@Variant("PREMIUM")` on `PremiumCheckout` (no `when` attribute). The VariantModel has predicateClass = null and order = 0 (no condition).

#### 3: Processor collects all condition variants for a feature

The processor collects VariantModels for PricingStrategy: EnterprisePricing (order 1), StartupPricing (order 2), StandardPricing (default). It sorts condition variants by order for code generation.

### UAT Scenarios (BDD)

#### Scenario: Processor extracts predicate class from @Condition

Given @Variant(when = @Condition(on = IsEnterprise.class, order = 1)) on EnterprisePricing
When the annotation processor processes EnterprisePricing
Then the VariantModel contains predicate class reference "com.example.IsEnterprise"
And order value 1

#### Scenario: Processor distinguishes condition-based from value-based variants

Given @Feature PricingStrategy has condition-based variants
And @Feature CheckoutFlow has value-based variants
When the processor processes both features
Then PricingStrategy variants have condition metadata
And CheckoutFlow variants have value metadata

#### Scenario: Processor orders condition variants by order attribute

Given three condition variants with orders 3, 1, 2
When the processor collects them
Then they are sorted as order 1, 2, 3 for code generation

### Acceptance Criteria

- [ ] VariantModel extended with optional predicate class reference and order
- [ ] Processor correctly reads @Condition's on and order attributes from annotation mirror
- [ ] Condition variants sorted by order for code generation
- [ ] Value-based variants unchanged -- no condition metadata

### Outcome KPIs

- **Who**: Java developers compiling projects with condition-based variants
- **Does what**: Annotation processor correctly models condition metadata
- **By how much**: Zero processor errors on valid @Condition usage
- **Measured by**: Processor unit tests with condition and value-based variant combinations
- **Baseline**: Processor has no condition awareness

### Technical Notes

- Uses javax.lang.model.element API to read annotation values
- Predicate class stored as TypeMirror (compile-time), not Class (runtime)
- Depends on: US-CP-02 (@Condition annotation exists to read)

---

## US-CP-04: Compile-Time Predicate Type Validation

### Problem

Kenji Tanaka references `IsEnterprise.class` in `@Condition(on = ...)`, but if `IsEnterprise` does not implement `FeaturePredicate`, the code would fail at runtime with a confusing ClassCastException. He expects the annotation processor to catch this at compile time.

### Who

- Java developer | References predicate class in @Condition | Expects compile-time safety

### Solution

The annotation processor validates that the class referenced by `@Condition(on = ...)` implements `FeaturePredicate` and has an accessible no-arg constructor.

### Domain Examples

#### 1: Valid predicate -- IsEnterprise implements FeaturePredicate

Kenji's `IsEnterprise implements FeaturePredicate` is referenced in `@Condition(on = IsEnterprise.class)`. The processor verifies the implements relationship. Compilation succeeds.

#### 2: Invalid predicate -- NotAPredicate does not implement FeaturePredicate

Kenji accidentally references `NotAPredicate.class` which is a plain POJO. The processor emits: `error: NotAPredicate does not implement FeaturePredicate`. Compilation fails.

#### 3: Missing no-arg constructor -- Predicate requires DI

Kenji's `DatabasePredicate` has only `DatabasePredicate(DataSource ds)`. Without Spring module, the processor emits: `error: DatabasePredicate must have an accessible no-arg constructor (or use flagzen-spring for DI)`. Compilation fails.

### UAT Scenarios (BDD)

#### Scenario: Valid predicate compiles successfully

Given @Condition(on = IsEnterprise.class) where IsEnterprise implements FeaturePredicate
When the annotation processor runs
Then no errors are emitted for IsEnterprise

#### Scenario: Non-predicate class fails compilation

Given @Condition(on = NotAPredicate.class) where NotAPredicate does not implement FeaturePredicate
When the annotation processor runs
Then compilation fails with "NotAPredicate does not implement FeaturePredicate"

#### Scenario: Predicate without no-arg constructor fails compilation

Given @Condition(on = DatabasePredicate.class) with no no-arg constructor
When the annotation processor runs
Then compilation fails with message containing "accessible no-arg constructor"

#### Scenario: Abstract predicate class fails compilation

Given @Condition(on = AbstractPredicate.class) where AbstractPredicate is abstract
When the annotation processor runs
Then compilation fails with message indicating the class cannot be instantiated

### Acceptance Criteria

- [ ] Processor validates @Condition(on) class implements FeaturePredicate
- [ ] Processor validates @Condition(on) class has accessible no-arg constructor
- [ ] Processor validates @Condition(on) class is not abstract
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
- Depends on: US-CP-01 (FeaturePredicate), US-CP-03 (processor reads @Condition)

---

## US-CP-05: Order Uniqueness and Mixing Validation

### Problem

Kenji Tanaka might accidentally assign the same order value to two condition-based variants, making evaluation order ambiguous. Or he might mix value-based and condition-based variants on the same feature, which has no coherent dispatch semantics. Both must be caught at compile time.

### Who

- Java developer | Declares multiple condition-based variants | Expects compile-time protection against ambiguous configurations

### Solution

The annotation processor validates: (1) no duplicate order values within the same @Feature, and (2) a feature cannot mix value-based and condition-based @Variant annotations.

### Domain Examples

#### 1: Duplicate order values -- Kenji assigns order 1 twice

Kenji writes `@Condition(on = IsEnterprise.class, order = 1)` on EnterprisePricing and `@Condition(on = IsStartup.class, order = 1)` on StartupPricing. Processor emits: `error: Duplicate @Condition order 1 on PricingStrategy`. Compilation fails.

#### 2: Mixed dispatch modes -- Kenji mixes value and condition

Kenji writes `@Variant("PREMIUM")` on PremiumPricing and `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))` on EnterprisePricing, both implementing PricingStrategy. Processor emits: `error: PricingStrategy mixes value-based and condition-based variants`. Compilation fails.

#### 3: Valid separate features -- One value-based, one condition-based

Kenji has `@Feature("checkout-flow")` with value-based variants and `@Feature("pricing-tier")` with condition-based variants. Both compile successfully because each feature uses a single dispatch mode.

### UAT Scenarios (BDD)

#### Scenario: Duplicate order values rejected

Given @Feature PricingStrategy with two variants both having @Condition order = 1
When the annotation processor runs
Then compilation fails with "Duplicate @Condition order 1 on PricingStrategy"

#### Scenario: Mixed value-based and condition-based variants rejected

Given @Feature PricingStrategy with @Variant("PREMIUM") and @Variant(when = @Condition(...))
When the annotation processor runs
Then compilation fails with message containing "mixes value-based and condition-based"

#### Scenario: Separate features with different dispatch modes compile

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
- [ ] Mixing value-based and condition-based @Variant on same @Feature produces compile error
- [ ] Different @Feature interfaces can use different dispatch modes
- [ ] Error messages identify the @Feature and conflicting elements
- [ ] Non-sequential order values (e.g., 10, 20, 30) are valid

### Outcome KPIs

- **Who**: Java developers declaring multiple condition-based variants
- **Does what**: Get compile-time error for ambiguous or conflicting configurations
- **By how much**: 100% of order conflicts and mixed modes caught at compile time
- **Measured by**: Processor error tests for each invalid combination
- **Baseline**: No order or mixing validation exists

### Technical Notes

- Order uniqueness: collect all order values per FeatureModel, check for duplicates
- Mixing detection: partition variants into value-based and condition-based per feature, reject if both non-empty
- @DefaultVariant is compatible with both modes (not counted as either)
- Depends on: US-CP-03 (processor models condition metadata)

---

## US-CP-06: Proxy Generation for Predicate Dispatch

### Problem

Kenji Tanaka has valid condition-based variants that pass compile-time validation, but the proxy generator only knows how to emit value-based dispatch (map lookup from flag provider string). He needs the generator to produce a second dispatch path that evaluates predicates in order.

### Who

- Java developer | Has compiled condition-based @Feature | Expects generated proxy to dispatch via predicates

### Solution

Extend ProxyGenerator to emit predicate-based dispatch logic: instantiate predicates, evaluate in order, delegate to first matching variant.

### Domain Examples

#### 1: Enterprise pricing selected -- Kenji resolves with enterprise context

Kenji has `@Feature("pricing-tier")` with IsEnterprise (order 1) and IsStartup (order 2). He creates an EvaluationContext with attribute "plan" = "enterprise" and resolves PricingStrategy. The generated proxy calls `IsEnterprise.test(ctx)` which returns true. EnterprisePricing is selected. IsStartup is never evaluated.

#### 2: Startup pricing selected -- Kenji resolves with startup context

Same feature. Context has attribute "plan" = "startup". Proxy calls `IsEnterprise.test(ctx)` -> false, then `IsStartup.test(ctx)` -> true. StartupPricing is selected.

#### 3: No match, default selected -- Kenji resolves with free-tier context

Context has attribute "plan" = "free". Both predicates return false. Proxy falls through to `@DefaultVariant StandardPricing`.

### UAT Scenarios (BDD)

#### Scenario: Generated proxy evaluates predicates in order

Given @Feature PricingStrategy with IsEnterprise (order 1) and IsStartup (order 2)
And EvaluationContext with attribute "plan" = "enterprise"
When Kenji resolves PricingStrategy
Then the proxy evaluates IsEnterprise.test(ctx) first
And returns EnterprisePricing

#### Scenario: Proxy stops at first matching predicate

Given @Feature PricingStrategy with IsEnterprise (order 1) and IsStartup (order 2)
And EvaluationContext with attribute "plan" = "enterprise"
When the proxy dispatches
Then IsStartup.test(ctx) is not called

#### Scenario: Proxy uses @DefaultVariant when no predicate matches

Given @Feature PricingStrategy with condition-based variants and @DefaultVariant StandardPricing
And EvaluationContext with attribute "plan" = "free"
When the proxy dispatches
Then both predicates return false
And StandardPricing is selected

#### Scenario: Proxy re-evaluates predicates on each method call

Given Kenji has a resolved PricingStrategy proxy reference
When he calls calculate() inside FlagContext.run() with enterprise context
And then calls calculate() inside FlagContext.run() with startup context
Then the first call dispatches to EnterprisePricing
And the second call dispatches to StartupPricing

#### Scenario: Predicate exception propagates to caller

Given @Feature PricingStrategy with a predicate IsEnterprise that throws RuntimeException
And EvaluationContext with attribute "plan" = "enterprise"
When the proxy evaluates IsEnterprise.test(ctx)
Then the RuntimeException propagates to the caller
And FlagZen does not swallow or wrap the exception

#### Scenario: Generated proxy has zero reflection imports

Given the generated PricingStrategy_FlagZenProxy source
When inspecting its imports
Then no java.lang.reflect imports are present

### Acceptance Criteria

- [ ] ProxyGenerator emits predicate dispatch path for condition-based @Feature
- [ ] Predicates evaluated in order attribute sequence
- [ ] First matching predicate selects the variant
- [ ] Subsequent predicates not evaluated after match (short-circuit)
- [ ] Predicates re-evaluated on each method call (dynamic dispatch)
- [ ] Predicate exceptions propagate to caller (FlagZen does not catch or wrap)
- [ ] Generated code has zero java.lang.reflect imports
- [ ] Proxy obtains EvaluationContext from the resolution chain (M1)

### Outcome KPIs

- **Who**: Java developers with condition-based features
- **Does what**: Get automatic predicate dispatch without writing dispatch logic
- **By how much**: Zero lines of dispatch code written by the developer
- **Measured by**: Generated proxy source inspection
- **Baseline**: Proxy generator only supports value-based dispatch

### Technical Notes

- Predicates instantiated once at proxy construction (no-arg constructor), reused per dispatch
- EvaluationContext obtained per method call from resolution chain (explicit > accessor > scoped > default)
- For value-based features: generated proxy is unchanged from M0
- Proxy determines dispatch mode from FeatureMetadata at construction
- Depends on: US-CP-01, US-CP-02, US-CP-03, US-CP-04 (all validation must pass before generation)

---

## US-CP-07: Fallback Behavior for Condition Dispatch

### Problem

Kenji Tanaka has a condition-based @Feature where no predicate matches and no @DefaultVariant exists. The behavior must be consistent with existing FallbackStrategy semantics -- but the meaning of REQUIRED needs clarification for condition-based features (there is no enum coverage concept for predicates).

### Who

- Java developer | Has condition-based feature with unmatched context | Expects consistent fallback behavior

### Solution

FallbackStrategy for condition-based features: EXCEPTION throws UnmatchedVariantException, NOOP returns safe defaults, REQUIRED requires @DefaultVariant at compile time (since predicate completeness cannot be statically verified).

### Domain Examples

#### 1: EXCEPTION strategy, no match, no default -- Kenji gets clear error

Kenji's PricingStrategy has `fallback = FallbackStrategy.EXCEPTION` and no @DefaultVariant. Context has "plan" = "free", no predicates match. `UnmatchedVariantException` is thrown with message listing the available predicates: "No condition matched for pricing-tier. Predicates: [IsEnterprise(1), IsStartup(2)]."

#### 2: NOOP strategy, no match, no default -- Kenji gets safe default

Kenji's PricingStrategy has `fallback = FallbackStrategy.NOOP`. No predicates match. The proxy returns safe defaults: `calculate()` returns `null` (or Money zero if return type allows).

#### 3: REQUIRED strategy enforces @DefaultVariant at compile time

Kenji's PricingStrategy has `fallback = FallbackStrategy.REQUIRED` (the default). The processor requires @DefaultVariant because predicate completeness cannot be proven at compile time. Without @DefaultVariant, compilation fails: "REQUIRED strategy on condition-based feature pricing-tier requires @DefaultVariant."

### UAT Scenarios (BDD)

#### Scenario: EXCEPTION strategy throws when no predicate matches

Given @Feature PricingStrategy with fallback = EXCEPTION and no @DefaultVariant
And EvaluationContext where no predicate matches
When Kenji resolves PricingStrategy
Then UnmatchedVariantException is thrown
And the exception message lists available predicates

#### Scenario: NOOP strategy returns safe defaults when no predicate matches

Given @Feature PricingStrategy with fallback = NOOP and no @DefaultVariant
And EvaluationContext where no predicate matches
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

#### Scenario: No EvaluationContext falls through to fallback

Given a condition-based @Feature with @DefaultVariant
And no EvaluationContext available
When Kenji resolves the feature
Then @DefaultVariant is selected (predicates cannot match without context)

### Acceptance Criteria

- [ ] EXCEPTION: throws UnmatchedVariantException with predicate list in message
- [ ] NOOP: returns safe default values (consistent with M0 NOOP behavior)
- [ ] REQUIRED: compilation fails without @DefaultVariant on condition-based features
- [ ] @DefaultVariant takes priority over FallbackStrategy (same as M0)
- [ ] Missing EvaluationContext falls through to @DefaultVariant or FallbackStrategy

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

#### 1: Predicate with injected service -- Kenji's IsPremiumCustomer checks the database

Kenji writes `@Component class IsPremiumCustomer implements FeaturePredicate`. It has `IsPremiumCustomer(CustomerRepository repo)` constructor. Spring injects the repository. The predicate queries `repo.findById(ctx.targetingKey())` and checks the plan field.

#### 2: Non-Spring environment -- No-arg constructor still works

Tomas Bergstrom does not use Spring. His `IsEnterprise` has a no-arg constructor and checks context attributes only. It works identically to US-CP-01 through US-CP-07.

#### 3: Mixed predicates -- Some Spring-managed, some plain

Kenji has `@Component class IsPremiumCustomer` (Spring-managed) and `class IsEuRegion` (no-arg constructor, no Spring annotation). Both work: IsPremiumCustomer resolved from ApplicationContext, IsEuRegion instantiated via constructor.

### UAT Scenarios (BDD)

#### Scenario: Spring-managed predicate resolved from ApplicationContext

Given flagzen-spring is on the classpath
And IsPremiumCustomer is annotated with @Component
And IsPremiumCustomer has a constructor with CustomerRepository parameter
When the proxy needs to evaluate IsPremiumCustomer
Then it is resolved from the Spring ApplicationContext
And the CustomerRepository is injected

#### Scenario: Non-Spring predicate uses no-arg constructor

Given flagzen-spring is not on the classpath
And IsEnterprise has a no-arg constructor
When the proxy needs to evaluate IsEnterprise
Then it is instantiated via no-arg constructor

#### Scenario: Mixed Spring and plain predicates coexist

Given flagzen-spring is on the classpath
And IsPremiumCustomer is @Component with injected dependencies
And IsEuRegion has a no-arg constructor and no @Component
When the proxy evaluates both predicates
Then IsPremiumCustomer is resolved from ApplicationContext
And IsEuRegion is instantiated via no-arg constructor

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
