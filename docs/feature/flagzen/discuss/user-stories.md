<!-- markdownlint-disable MD024 -->

# User Stories -- FlagZen Release 1: Core Type-Safe Dispatch

## US-01: Define a Feature Flag as a Java Interface

### Problem

Marco Pellegrini is a senior Java developer at a fintech company who manages 15+ feature flags across checkout, payment, and onboarding flows. He finds it tedious and error-prone to define flags as string constants scattered across configuration files and code, with no type-safe contract for what a flag controls or what values it can take.

### Who

- Senior Java developer | Production codebase with 15+ feature flags | Wants type-safe, discoverable flag definitions

### Solution

A `@Feature` annotation on a Java interface that declares a feature flag. The interface defines the contract (methods) that all variants must implement. An optional inner `Variant` enum constrains allowed flag values.

### Domain Examples

#### 1: Happy Path -- Marco defines a checkout flow feature with enum

Marco creates a `CheckoutFlow` interface annotated with `@Feature("checkout-flow")` and defines an inner `Variant` enum with values `CLASSIC`, `STREAMLINED`, `PREMIUM`. The annotation processor registers this as a feature with three constrained variant values.

#### 2: Minimal Feature -- Marco defines a dark mode toggle without enum

Marco creates a `DarkMode` interface annotated with `@Feature("dark-mode")` without a Variant enum. Variant values are free-form strings. The annotation processor registers this as a feature with unconstrained variants.

#### 3: Error -- Marco uses @Feature on a class instead of an interface

Marco accidentally annotates a class with `@Feature("checkout-flow")`. The annotation processor emits a compile error: "@Feature can only be applied to interfaces."

### UAT Scenarios (BDD)

#### Scenario: Define feature with variant enum

Given Marco creates a Java interface `CheckoutFlow`
When he annotates it with `@Feature("checkout-flow")`
And he defines an inner `Variant` enum with values `CLASSIC`, `STREAMLINED`, `PREMIUM`
Then the annotation processor registers `"checkout-flow"` as a feature
And the variant values are constrained to the enum constants

#### Scenario: Define feature without variant enum

Given Marco creates a Java interface `DarkMode`
When he annotates it with `@Feature("dark-mode")`
And there is no inner `Variant` enum
Then the annotation processor registers `"dark-mode"` as a feature
And variant values are accepted as free-form strings

#### Scenario: Compile error when @Feature applied to class

Given Marco annotates a class `CheckoutService` with `@Feature("checkout-flow")`
When the project compiles
Then compilation fails
And the error states `"@Feature can only be applied to interfaces"`

#### Scenario: Feature with fallback strategy

Given Marco annotates an interface with `@Feature(value = "checkout-flow", fallback = FallbackStrategy.REQUIRED)`
When the project compiles
Then the annotation processor records the fallback strategy for later validation

### Acceptance Criteria

- [ ] @Feature annotation accepted on interfaces only (compile error on classes)
- [ ] Feature flag key extracted from annotation value attribute
- [ ] Optional inner Variant enum detected and used for compile-time validation
- [ ] FallbackStrategy configuration stored in annotation metadata
- [ ] Annotation processor emits clear error messages with source location

### Outcome KPIs

- **Who**: Java developers defining feature flags
- **Does what**: Define a feature flag as a typed interface instead of a string constant
- **By how much**: 100% of feature flags in FlagZen projects are type-safe interfaces
- **Measured by**: Annotation processor processes all @Feature annotations without error
- **Baseline**: 0% type-safe flag definitions in current Java ecosystem

### Technical Notes

- Annotation retention: CLASS (available to annotation processor, not needed at runtime)
- Annotation target: TYPE (interfaces only, validated by processor)
- Zero runtime reflection -- all metadata extracted at compile time
- Depends on: javax.annotation.processing / jakarta.annotation.processing API

---

## US-02: Implement Feature Variants as Annotated Classes

### Problem

Marco Pellegrini has defined a `CheckoutFlow` feature interface but needs to implement the different behaviors for each flag value. Currently, he writes if/else blocks that select behavior based on string comparisons, leading to scattered conditionals, no compile-time verification that all cases are covered, and difficulty finding all code paths for a given flag.

### Who

- Senior Java developer | Has defined @Feature interfaces | Wants to implement variant behaviors as separate, testable classes

### Solution

A `@Variant` annotation on implementation classes that declares which flag value the class handles. For multi-feature classes, `@Variant(of = Feature.class, value = "X")` syntax links to the specific feature. A `@DefaultVariant` annotation marks the fallback implementation.

### Domain Examples

#### 1: Happy Path -- Marco implements three checkout variants

Marco creates `ClassicCheckout`, `StreamlinedCheckout`, and `PremiumCheckout`, each annotated with `@Variant("CLASSIC")`, `@Variant("STREAMLINED")`, and `@Variant("PREMIUM")` respectively. All implement `CheckoutFlow`. The annotation processor links them to the `checkout-flow` feature.

#### 2: Multi-Feature Variant -- PremiumCreditCheckout handles two features

Marco creates `PremiumCreditCheckout` annotated with `@Variant(of = CheckoutFlow.class, value = "PREMIUM")` and `@Variant(of = PaymentMethod.class, value = "CREDIT_CARD")`. It implements both interfaces. The processor registers it for both features.

#### 3: Default Variant -- Marco provides a fallback implementation

Marco creates `DefaultCheckout` annotated with `@DefaultVariant` implementing `CheckoutFlow`. This is used when the flag value does not match any named variant.

### UAT Scenarios (BDD)

#### Scenario: Implement a valid variant

Given `CheckoutFlow` is annotated with `@Feature("checkout-flow")`
When Marco creates `ClassicCheckout` annotated with `@Variant("CLASSIC")`
And `ClassicCheckout` implements `CheckoutFlow`
Then compilation succeeds
And the annotation processor links `ClassicCheckout` to feature `"checkout-flow"` with value `"CLASSIC"`

#### Scenario: Implement a multi-feature variant

Given `CheckoutFlow` is annotated with `@Feature("checkout-flow")`
And `PaymentMethod` is annotated with `@Feature("payment-method")`
When Marco creates `PremiumCreditCheckout` with `@Variant(of = CheckoutFlow.class, value = "PREMIUM")` and `@Variant(of = PaymentMethod.class, value = "CREDIT_CARD")`
And `PremiumCreditCheckout` implements both `CheckoutFlow` and `PaymentMethod`
Then compilation succeeds
And the processor registers it for both features

#### Scenario: Implement a default variant

Given `CheckoutFlow` is annotated with `@Feature("checkout-flow")`
When Marco creates `DefaultCheckout` annotated with `@DefaultVariant`
And `DefaultCheckout` implements `CheckoutFlow`
Then compilation succeeds
And `DefaultCheckout` is registered as the fallback for `"checkout-flow"`

#### Scenario: Variant class does not implement feature interface

Given `CheckoutFlow` is annotated with `@Feature("checkout-flow")`
When Marco creates `BrokenVariant` annotated with `@Variant("CLASSIC")`
And `BrokenVariant` does not implement `CheckoutFlow`
Then compilation fails
And the error states the variant class must implement the feature interface

### Acceptance Criteria

- [ ] @Variant annotation accepted on classes implementing a @Feature interface
- [ ] @Variant(of = X.class, value = "Y") syntax supported for multi-feature implementations
- [ ] @DefaultVariant annotation marks fallback implementation
- [ ] Compile error if @Variant class does not implement the target @Feature interface
- [ ] Annotation processor links variant classes to their feature by interface type

### Outcome KPIs

- **Who**: Java developers implementing feature flag behaviors
- **Does what**: Implement each variant as a separate class instead of an if/else branch
- **By how much**: Zero if/else blocks for flag dispatch in application code
- **Measured by**: Absence of flag-conditional code in application layer (code review)
- **Baseline**: Average 3-5 if/else blocks per feature flag in current codebases

### Technical Notes

- @Variant annotation retention: CLASS
- @Variant target: TYPE (classes only)
- @Repeatable for multi-feature classes (Java 8+ repeatable annotations)
- @DefaultVariant is mutually exclusive with a specific value (compile-time validation)
- Package-private variant classes must be supported

---

## US-03: Validate Variant Values at Compile Time

### Problem

Marco Pellegrini once spent 45 minutes debugging a production issue caused by a typo in a flag value -- the flag provider returned `"PREMIUM"` but the code checked for `"PREMIMUM"`. Runtime string matching for flag values is error-prone and catches mistakes too late. In Java, where type safety is a core value, flag configuration errors should be caught at compile time.

### Who

- Senior Java developer | Values compile-time safety | Has been burned by runtime flag value mismatches

### Solution

The annotation processor validates `@Variant` values against the optional `Variant` enum on the `@Feature` interface. When the enum is present, any `@Variant` with a value not in the enum produces a compile error. When `FallbackStrategy.REQUIRED` is set, all enum values must have a corresponding `@Variant` implementation.

### Domain Examples

#### 1: Happy Path -- All variants match enum values

`CheckoutFlow` defines `Variant { CLASSIC, STREAMLINED, PREMIUM }`. Marco creates three variant classes with matching values. Compilation succeeds. The processor confirms completeness.

#### 2: Typo Caught at Compile Time -- "TURBO" is not a valid variant

Marco creates `TurboCheckout` with `@Variant("TURBO")` implementing `CheckoutFlow`. Compilation fails: `"@Variant("TURBO") does not match any value in CheckoutFlow.Variant. Valid values: CLASSIC, STREAMLINED, PREMIUM."` Marco fixes the typo immediately.

#### 3: Missing Variant with REQUIRED Strategy

`CheckoutFlow` uses `FallbackStrategy.REQUIRED` and defines three enum values. Marco only implements `ClassicCheckout` and `StreamlinedCheckout`. Compilation fails: `"Feature 'checkout-flow' uses REQUIRED fallback but variant PREMIUM has no implementation."`

### UAT Scenarios (BDD)

#### Scenario: Variant value matches enum

Given `CheckoutFlow` defines `Variant` enum with `CLASSIC`, `STREAMLINED`, `PREMIUM`
When Marco creates `ClassicCheckout` with `@Variant("CLASSIC")`
Then compilation succeeds

#### Scenario: Variant value does not match enum

Given `CheckoutFlow` defines `Variant` enum with `CLASSIC`, `STREAMLINED`, `PREMIUM`
When Marco creates `TurboCheckout` with `@Variant("TURBO")`
Then compilation fails
And the error states `"TURBO"` is not in `CheckoutFlow.Variant`
And the error lists valid values

#### Scenario: Duplicate variant value

Given `ClassicCheckout` with `@Variant("CLASSIC")` for `CheckoutFlow` exists
When Marco creates `LegacyCheckout` also with `@Variant("CLASSIC")` for `CheckoutFlow`
Then compilation fails
And the error identifies both classes as conflicting

#### Scenario: REQUIRED strategy with incomplete coverage

Given `CheckoutFlow` uses `FallbackStrategy.REQUIRED`
And enum defines `CLASSIC`, `STREAMLINED`, `PREMIUM`
And only `CLASSIC` and `STREAMLINED` have `@Variant` implementations
When the project compiles
Then compilation fails
And the error lists `PREMIUM` as missing

#### Scenario: REQUIRED strategy satisfied by @DefaultVariant

Given `CheckoutFlow` uses `FallbackStrategy.REQUIRED`
And enum defines `CLASSIC`, `STREAMLINED`, `PREMIUM`
And only `CLASSIC` and `STREAMLINED` have `@Variant` implementations
And `DefaultCheckout` has `@DefaultVariant` implementing `CheckoutFlow`
When the project compiles
Then compilation succeeds because `@DefaultVariant` covers unmatched values

### Acceptance Criteria

- [ ] Compile error for @Variant value not in the Variant enum (when enum present)
- [ ] Error message includes the invalid value and all valid values
- [ ] Compile error for duplicate @Variant values on the same feature
- [ ] REQUIRED fallback strategy enforces all enum values have implementations
- [ ] @DefaultVariant satisfies REQUIRED for uncovered enum values
- [ ] No validation when Variant enum is absent (free-form strings allowed)

### Outcome KPIs

- **Who**: Java developers configuring feature flags
- **Does what**: Catch flag value errors at compile time instead of runtime
- **By how much**: 100% of enum-constrained variant mismatches caught before deployment
- **Measured by**: Annotation processor test suite (compile-error test cases)
- **Baseline**: 0% compile-time flag validation in current Java ecosystem

### Technical Notes

- Annotation processor accesses enum constants via `javax.lang.model` API
- Cross-compilation-unit validation (feature in module A, variants in module B) is NOT covered here -- that requires a runtime startup check (separate story)
- Error messages must include source file location for IDE click-through

---

## US-04: Generate Dispatch Proxy at Compile Time

### Problem

Marco Pellegrini wants the polymorphic dispatch to happen automatically -- he does not want to manually wire a factory or registry that maps flag values to variant instances. The dispatch logic should be generated by the annotation processor so that there is zero runtime reflection and the proxy is a concrete class visible in the IDE.

### Who

- Senior Java developer | Wants zero-boilerplate dispatch | Expects generated code to be debuggable and transparent

### Solution

The annotation processor generates a proxy class (`{Feature}_FlagZenProxy`) for each `@Feature` interface. The proxy implements the feature interface and delegates method calls to the active variant based on the current flag value from the configured `FlagProvider`.

### Domain Examples

#### 1: Happy Path -- Proxy generated for CheckoutFlow

After compiling, the annotation processor generates `CheckoutFlow_FlagZenProxy` in the same package as `CheckoutFlow`. The proxy implements `CheckoutFlow` and contains a dispatch method that consults the `FlagProvider` for `"checkout-flow"` and delegates to the matching `@Variant` instance.

#### 2: Generated Code Quality -- Readable and debuggable

Marco opens `CheckoutFlow_FlagZenProxy` in his IDE (generated sources). The code is well-formatted, has a clear `toString()` returning `"FlagZenProxy[checkout-flow]"`, and uses a simple map lookup -- no reflection, no dynamic proxy.

#### 3: Proxy with @DefaultVariant fallback

The generated proxy for `CheckoutFlow` includes fallback logic: if the flag value does not match any known variant, it delegates to the `@DefaultVariant` implementation (or executes the configured `FallbackStrategy`).

### UAT Scenarios (BDD)

#### Scenario: Proxy class generated during compilation

Given `CheckoutFlow` is annotated with `@Feature("checkout-flow")`
And `ClassicCheckout` and `StreamlinedCheckout` are annotated with `@Variant`
When the project compiles
Then `CheckoutFlow_FlagZenProxy` is generated in the same package
And it implements the `CheckoutFlow` interface

#### Scenario: Generated proxy has readable toString

Given `CheckoutFlow_FlagZenProxy` has been generated
When Marco calls `toString()` on the proxy
Then it returns `"FlagZenProxy[checkout-flow]"`

#### Scenario: Generated proxy uses zero reflection

Given `CheckoutFlow_FlagZenProxy` has been generated
When Marco inspects the generated source code
Then it contains no `java.lang.reflect` imports
And dispatch is via a map lookup or switch statement

#### Scenario: Proxy handles fallback strategy

Given `CheckoutFlow` uses `FallbackStrategy.NOOP`
And the flag provider returns `"BETA"` which has no matching variant
When Marco calls `checkoutFlow.execute(cart)` on the proxy
Then the method is a no-op (void methods) or returns a safe default
And no exception is thrown

### Acceptance Criteria

- [ ] Proxy class generated for each @Feature interface during annotation processing
- [ ] Proxy implements the @Feature interface with all declared methods
- [ ] Proxy uses map lookup or switch for dispatch (zero java.lang.reflect usage)
- [ ] Proxy toString() returns a descriptive name including the flag key
- [ ] Proxy handles all three FallbackStrategy values (REQUIRED validated at compile time, EXCEPTION and NOOP at runtime)
- [ ] Generated source is human-readable and formatted

### Outcome KPIs

- **Who**: Java developers using FlagZen
- **Does what**: Get automatic dispatch without writing factory/registry code
- **By how much**: Zero boilerplate dispatch code in application layer
- **Measured by**: Generated proxy test suite; absence of manual dispatch code
- **Baseline**: Manual strategy pattern wiring requires 20-50 lines per feature

### Technical Notes

- Code generation via JavaPoet or direct string templates
- Generated sources go to standard annotation processor output directory
- Proxy must be in same package as @Feature interface (package-private access)
- Must handle void methods, return types, checked exceptions, generics
- Dependency on FlagProvider interface at runtime (compile-time for code gen)

---

## US-05: Resolve Active Variant at Runtime via FeatureDispatcher

### Problem

Marco Pellegrini has `@Feature` interfaces, `@Variant` implementations, and generated proxies. Now he needs a simple API to obtain the proxy and use it. He wants a single entry point (`FeatureDispatcher`) that returns the proxy, configured with the active flag provider, so he can call feature methods without thinking about flag resolution.

### Who

- Senior Java developer | Has defined features and variants | Wants a simple resolution API

### Solution

`FeatureDispatcher` is the runtime entry point. `dispatcher.resolve(CheckoutFlow.class)` returns the generated proxy. The proxy re-evaluates the flag on each method call (dynamic resolution), ensuring runtime flag changes take effect without re-injection.

### Domain Examples

#### 1: Happy Path -- Marco resolves and uses CheckoutFlow

Marco obtains a `FeatureDispatcher` via `FlagZen.dispatcher()`, calls `dispatcher.resolve(CheckoutFlow.class)`, and gets a `CheckoutFlow` proxy. He calls `flow.execute(cart)` and the proxy delegates to `StreamlinedCheckout` because the flag provider returns `"STREAMLINED"`.

#### 2: Flag Changes at Runtime -- Proxy follows new value

The flag provider initially returns `"CLASSIC"`. Marco calls `flow.execute(cart)` -- delegates to `ClassicCheckout`. The flag provider value changes to `"PREMIUM"`. Marco calls `flow.execute(cart)` again -- now delegates to `PremiumCheckout`. No re-resolution needed.

#### 3: No Provider Configured -- Clear error

Marco calls `FlagZen.dispatcher()` without configuring any provider. On first `resolve()` call, a `FlagZenException` is thrown with message: `"No FlagProvider configured"` and suggestions for how to add one.

### UAT Scenarios (BDD)

#### Scenario: Resolve feature to active variant

Given an in-memory flag provider is configured with `"checkout-flow" = "STREAMLINED"`
When Marco calls `dispatcher.resolve(CheckoutFlow.class)`
Then the returned proxy delegates `execute(cart)` to `StreamlinedCheckout`

#### Scenario: Proxy follows runtime flag changes

Given the flag provider returns `"CLASSIC"` for `"checkout-flow"`
And Marco has resolved `CheckoutFlow` via the dispatcher
When the flag provider value changes to `"PREMIUM"`
Then the next call to `execute(cart)` on the same proxy delegates to `PremiumCheckout`

#### Scenario: No flag provider configured

Given no `FlagProvider` is configured
When Marco calls `dispatcher.resolve(CheckoutFlow.class)`
Then a `FlagZenException` is thrown
And the message includes `"No FlagProvider configured"`
And the message suggests adding `flagzen-env` or a custom provider

#### Scenario: Resolve returns same proxy instance (singleton)

Given Marco calls `dispatcher.resolve(CheckoutFlow.class)` twice
Then both calls return the same proxy instance

### Acceptance Criteria

- [ ] `FeatureDispatcher.resolve(Feature.class)` returns the generated proxy
- [ ] Proxy re-evaluates flag value on each method call (dynamic dispatch)
- [ ] Proxy is singleton per feature per dispatcher
- [ ] Clear exception with actionable message when no provider configured
- [ ] `FlagZen.dispatcher()` factory creates a configured dispatcher

### Outcome KPIs

- **Who**: Java developers resolving feature flags
- **Does what**: Obtain a type-safe proxy with one method call instead of writing flag lookup + conditional
- **By how much**: 1 line of resolution code vs. 5-10 lines of manual dispatch
- **Measured by**: API surface analysis (single resolve method)
- **Baseline**: Existing libraries require multi-step lookup + conditional

### Technical Notes

- FeatureDispatcher holds references to generated proxy classes (discovered at startup)
- Proxy discovery: service loader pattern or explicit registration
- Thread safety: dispatcher and proxies must be thread-safe
- FlagProvider interface: `Optional<String> getString(String key)` (minimal contract)

---

## US-06: Configure Flag Source via FlagProvider SPI

### Problem

Marco Pellegrini needs a way to tell FlagZen where flag values come from. For development and testing, he wants a simple in-memory provider. For production, he will use environment variables or a remote provider. He does not want to be locked into a specific flag source -- the provider should be pluggable via a standard Java SPI pattern.

### Who

- Senior Java developer | Needs pluggable flag sources | Values SPI patterns for extensibility

### Solution

`FlagProvider` is a Java interface (SPI) with a minimal contract. An `InMemoryFlagProvider` is included in flagzen-core for testing and development. Providers are discovered via `ServiceLoader` or registered programmatically.

### Domain Examples

#### 1: Happy Path -- Marco uses in-memory provider for development

Marco creates an `InMemoryFlagProvider`, sets `"checkout-flow"` to `"STREAMLINED"`, and configures the dispatcher. All feature resolutions use the in-memory values.

#### 2: Programmatic Configuration

Marco builds a custom provider that reads from a `Map<String, String>` and registers it: `FlagZen.configure(config -> config.provider(myMapProvider))`.

#### 3: No Provider Found -- Clear guidance

Marco forgets to configure a provider. On first resolution attempt, the error message lists available providers on the classpath and suggests adding one.

### UAT Scenarios (BDD)

#### Scenario: Configure in-memory flag provider

Given Marco creates an `InMemoryFlagProvider`
And sets `"checkout-flow"` to `"STREAMLINED"`
When he configures the dispatcher with this provider
Then `dispatcher.resolve(CheckoutFlow.class).execute(cart)` delegates to `StreamlinedCheckout`

#### Scenario: Provider SPI contract is minimal

Given a class implements `FlagProvider`
When it implements `Optional<String> getString(String key)`
Then it can be used as a flag source for FlagZen

#### Scenario: Programmatic provider registration

Given Marco has a custom `FlagProvider` implementation
When he calls `FlagZen.configure(config -> config.provider(myProvider))`
Then the dispatcher uses his provider for all flag resolutions

### Acceptance Criteria

- [ ] `FlagProvider` interface defines `Optional<String> getString(String key)`
- [ ] `InMemoryFlagProvider` included in flagzen-core for development/testing
- [ ] Programmatic registration via `FlagZen.configure()`
- [ ] ServiceLoader discovery as fallback for classpath-based providers
- [ ] Clear error when no provider configured (lists available providers)

### Outcome KPIs

- **Who**: Java developers configuring flag sources
- **Does what**: Plug in any flag source without modifying application code
- **By how much**: Zero application code changes when switching providers
- **Measured by**: Provider swap test (change provider, zero code changes in app layer)
- **Baseline**: Existing libraries require provider-specific code throughout application

### Technical Notes

- FlagProvider interface in flagzen-core (no external dependencies)
- InMemoryFlagProvider is mutable and thread-safe (ConcurrentHashMap)
- Consider FlagProvider.getBoolean, FlagProvider.getInt for type-safe variants (or keep string-only for simplicity in MVP)
- SPI registration via META-INF/services/com.flagzen.spi.FlagProvider

---

## US-07: Pin Flag Values in Tests with @PinFlag

### Problem

Marco Pellegrini spends 15-30 lines of setup code per test to mock his LaunchDarkly flag provider. The test setup is more complex than the test itself. He wants to pin a specific flag value for a test method with a single annotation, with no mock framework or provider infrastructure needed.

### Who

- Senior Java developer | Writes tests for flag-dependent code daily | Frustrated by test setup verbosity

### Solution

`@PinFlag(feature = "x", variant = "y")` annotation on test methods pins a flag value for the duration of the test. A `FlagZenExtension` for JUnit 5 processes the annotation. `TestFlagContext` parameter injection enables programmatic pinning.

### Domain Examples

#### 1: Happy Path -- Marco pins a variant with one annotation

Marco annotates his test with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")`. The test resolves `CheckoutFlow` to `PremiumCheckout` without any setup code. Total test setup: 1 annotation.

#### 2: Programmatic Pinning -- Marco switches mid-test

Marco's test receives `TestFlagContext` as a parameter. He pins `"checkout-flow"` to `"CLASSIC"`, verifies behavior, then repins to `"PREMIUM"` and verifies again. Both pins are scoped to the test method.

#### 3: Multiple Flags -- Marco pins two flags in one test

Marco annotates with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")` and `@PinFlag(feature = "payment-method", variant = "CREDIT_CARD")`. Both flags are pinned for the test.

### UAT Scenarios (BDD)

#### Scenario: Pin flag with annotation

Given Marco has a test class with `@ExtendWith(FlagZenExtension.class)`
When he annotates a test method with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")`
Then `CheckoutFlow` resolves to `PremiumCheckout` for the duration of the test
And no mock setup is needed

#### Scenario: Programmatic pinning via TestFlagContext

Given Marco has a test method receiving `TestFlagContext flags` as parameter
When he calls `flags.pin("checkout-flow", "PREMIUM")`
Then `CheckoutFlow` resolves to `PremiumCheckout`
And the pin is scoped to the current test method only

#### Scenario: Multiple @PinFlag annotations

Given Marco annotates a test with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")` and `@PinFlag(feature = "payment-method", variant = "CREDIT_CARD")`
Then both flags are pinned for the test duration

#### Scenario: Pin isolation between tests

Given test A pins `"checkout-flow"` to `"PREMIUM"`
And test B pins `"checkout-flow"` to `"CLASSIC"`
When both tests execute
Then each test sees only its own pinned value
And tests can run in parallel without interference

#### Scenario: Pin resolves injected feature parameter

Given Marco's test method declares `CheckoutFlow flow` as a parameter
And the method has `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")`
Then `flow` is injected by the extension as a proxy resolving to `PremiumCheckout`

### Acceptance Criteria

- [ ] @PinFlag annotation pins a specific variant for a test method
- [ ] @PinFlag is @Repeatable for multiple flags
- [ ] FlagZenExtension processes @PinFlag and sets up test flag context
- [ ] TestFlagContext injectable as a JUnit 5 parameter for programmatic control
- [ ] @Feature interface injectable as a JUnit 5 parameter (resolved proxy)
- [ ] Pins are isolated per test method (parallel test safe)

### Outcome KPIs

- **Who**: Java developers testing flag-dependent code
- **Does what**: Write flag tests with 1 line of setup instead of 15-30
- **By how much**: 90% reduction in test setup code for flag-dependent tests
- **Measured by**: Line count comparison with LaunchDarkly/Togglz test setup
- **Baseline**: 15-30 lines of setup for LaunchDarkly, 10 lines for Togglz

### Technical Notes

- JUnit 5 Extension API: BeforeEachCallback, ParameterResolver
- @PinFlag retention: RUNTIME (read by JUnit extension at test time)
- TestFlagContext uses an InMemoryFlagProvider scoped to the test
- Must support both annotation-based and programmatic pinning in the same test
- @PinFlag overrides programmatic pins (annotation wins)

---

## US-08: Configure Test Flags from Properties Files

### Problem

Marco Pellegrini has a test suite where most tests share a common flag configuration (e.g., all tests run against `"CLASSIC"` checkout unless overridden). He does not want to repeat `@PinFlag` on every test method. He wants to declare a baseline configuration in a properties file and override per-test as needed.

### Who

- Senior Java developer | Has 50+ flag-dependent tests | Wants DRY test configuration

### Solution

`@FlagSource("flags-test.properties")` annotation on test classes loads flag values from a file. Method-level `@PinFlag` overrides file values. Priority: `@PinFlag` > `@FlagSource` > default provider.

### Domain Examples

#### 1: Happy Path -- Marco uses file-based configuration for test suite

Marco creates `src/test/resources/flags-test.properties` with `checkout-flow=CLASSIC` and `payment-method=CREDIT_CARD`. He annotates his test class with `@FlagSource("flags-test.properties")`. All tests use these values unless overridden.

#### 2: Method Override -- One test needs PREMIUM

In the same class, Marco annotates one test with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")`. That test uses PREMIUM; all others use CLASSIC from the file.

#### 3: File Not Found -- Clear error

Marco annotates with `@FlagSource("nonexistent.properties")`. The test fails immediately with: `"Flag source file not found: nonexistent.properties. Searched in: classpath, test resources."

### UAT Scenarios (BDD)

#### Scenario: Load flags from properties file

Given a file `src/test/resources/flags-test.properties` contains `checkout-flow=CLASSIC`
When Marco annotates the test class with `@FlagSource("flags-test.properties")`
Then all tests in the class resolve `"checkout-flow"` to `"CLASSIC"`

#### Scenario: @PinFlag overrides @FlagSource

Given the test class has `@FlagSource("flags-test.properties")` with `checkout-flow=CLASSIC`
When a test method has `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")`
Then that test resolves `"checkout-flow"` to `"PREMIUM"`
And other tests in the class still resolve to `"CLASSIC"`

#### Scenario: File not found produces clear error

Given Marco annotates with `@FlagSource("nonexistent.properties")`
When the test class initializes
Then a clear error is thrown stating the file was not found
And the searched locations are listed

### Acceptance Criteria

- [ ] @FlagSource loads flag values from a properties file on classpath
- [ ] @FlagSource supports .properties, .json, and .yaml formats
- [ ] Priority: @PinFlag > @FlagSource > default provider
- [ ] Clear error when file not found, including searched locations
- [ ] @FlagSource can be applied at class or method level

### Outcome KPIs

- **Who**: Java developers with large test suites using feature flags
- **Does what**: Share baseline flag configuration across tests via file
- **By how much**: Eliminate repeated @PinFlag annotations on 50+ test methods
- **Measured by**: Reduction in annotation repetition in test classes
- **Baseline**: No file-based flag test configuration in any Java library

### Technical Notes

- Properties file format: `flag.key=variant.value` (one per line)
- JSON/YAML support is nice-to-have for MVP; properties is sufficient
- File resolution: classpath first, then absolute path
- @FlagSource retention: RUNTIME

---

## US-09: Handle Missing or Unmatched Variants with FallbackStrategy

### Problem

Marco Pellegrini's flag provider sometimes returns values that do not match any `@Variant` implementation -- for example, when a new variant is added in the flag provider but the code has not been updated yet. He needs configurable behavior for this case: fail loudly (catch bugs), fail silently (graceful degradation), or enforce completeness at compile time.

### Who

- Senior Java developer | Deals with flag value mismatches between provider and code | Wants configurable error handling

### Solution

`FallbackStrategy` enum on `@Feature` annotation: `REQUIRED` (compile-time completeness), `EXCEPTION` (runtime exception on mismatch), `NOOP` (silent no-op for void, safe defaults for return types).

### Domain Examples

#### 1: EXCEPTION Strategy -- Loud failure for debugging

Marco's `CheckoutFlow` uses `FallbackStrategy.EXCEPTION`. The provider returns `"BETA"`. The proxy throws `UnmatchedVariantException` with message listing known variants and suggesting fixes.

#### 2: NOOP Strategy -- Graceful degradation for non-critical features

Marco's `DarkMode` uses `FallbackStrategy.NOOP`. The provider returns `"midnight"` which has no variant. The proxy calls a no-op for void methods and returns `false` for boolean, `0` for int, `null` for objects, `Optional.empty()` for Optional, `List.of()` for collections.

#### 3: REQUIRED Strategy -- Compile-time safety for critical features

Marco's `PaymentMethod` uses `FallbackStrategy.REQUIRED` with enum values `CREDIT_CARD`, `DEBIT`, `PAYPAL`. He must implement all three variants or add a `@DefaultVariant`. Missing one fails compilation.

### UAT Scenarios (BDD)

#### Scenario: EXCEPTION strategy throws on unmatched variant

Given `CheckoutFlow` uses `FallbackStrategy.EXCEPTION`
And the flag provider returns `"BETA"` for `"checkout-flow"`
When Marco calls `checkoutFlow.execute(cart)`
Then an `UnmatchedVariantException` is thrown
And the message lists known variants: `CLASSIC`, `STREAMLINED`, `PREMIUM`

#### Scenario: NOOP strategy returns safe defaults

Given `DarkMode` uses `FallbackStrategy.NOOP`
And the flag provider returns `"midnight"` with no matching variant
When Marco calls `darkMode.apply(ui)` (void method)
Then no exception is thrown and the method is a no-op
When Marco calls `darkMode.isEnabled()` (boolean method)
Then `false` is returned

#### Scenario: REQUIRED strategy enforces completeness at compile time

Given `PaymentMethod` uses `FallbackStrategy.REQUIRED` with enum `CREDIT_CARD`, `DEBIT`, `PAYPAL`
And only `CREDIT_CARD` and `DEBIT` have implementations
When the project compiles
Then compilation fails listing `PAYPAL` as missing

#### Scenario: @DefaultVariant handles unmatched values

Given `CheckoutFlow` uses `FallbackStrategy.EXCEPTION`
And `DefaultCheckout` is annotated with `@DefaultVariant`
When the flag provider returns `"BETA"` with no matching `@Variant`
Then the proxy delegates to `DefaultCheckout` instead of throwing

### Acceptance Criteria

- [ ] FallbackStrategy.EXCEPTION throws UnmatchedVariantException with known variants
- [ ] FallbackStrategy.NOOP returns safe defaults (false, 0, null, Optional.empty(), List.of())
- [ ] FallbackStrategy.REQUIRED enforces compile-time completeness against Variant enum
- [ ] @DefaultVariant is used before fallback strategy is applied
- [ ] Exception messages include the unmatched value, known variants, and fix suggestions

### Outcome KPIs

- **Who**: Java developers handling flag value mismatches
- **Does what**: Configure explicit behavior for edge cases instead of silent bugs
- **By how much**: 100% of unmatched variant scenarios handled explicitly
- **Measured by**: No silent misbehavior from flag value mismatches in flagged code
- **Baseline**: Most libraries silently ignore mismatches or throw generic exceptions

### Technical Notes

- NOOP default return values: follow Java language defaults + collection empty instances
- UnmatchedVariantException extends RuntimeException
- REQUIRED validation at compile time only works within the same compilation unit
- Default FallbackStrategy when not specified: EXCEPTION (fail-fast principle)
