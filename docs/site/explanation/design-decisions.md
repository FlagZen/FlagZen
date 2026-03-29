# Design Decisions Explained

Summary of FlagZen's major architectural decisions and the reasoning behind them.

## Overview

FlagZen's design reflects a series of intentional choices, each made to solve a specific problem or optimize for a particular quality attribute. This document summarizes the decisions codified in ADRs 001-020 and explains the trade-offs.

## Decision Index

|   ADR   |                        Decision                        |  Status  |
| ------- | ------------------------------------------------------ | -------- |
| ADR-001 | Compile-time proxy generation per `@Feature`           | Accepted |
| ADR-002 | JavaPoet for code generation                           | Accepted |
| ADR-003 | `Optional<String> getString(String)` FlagProvider      | Accepted |
| ADR-004 | FeatureDispatcher as interface with factory            | Accepted |
| ADR-005 | Gradle monorepo with per-concern modules               | Accepted |
| ADR-006 | CLASS retention for core, RUNTIME for test             | Accepted |
| ADR-007 | Public class, package-private constructor for proxies  | Accepted |
| ADR-008 | Unified ordered dispatch model                         | Accepted |
| ADR-009 | JDK functional interfaces for predicates               | Accepted |
| ADR-010 | `@Condition` nested inside `@Variant`                  | Accepted |
| ADR-011 | Fixed context resolution order                         | Accepted |
| ADR-012 | ThreadLocal carrier with ScopedValue upgrade path      | Accepted |
| ADR-013 | `@CloseTo` per-variant delta for doubles               | Accepted |
| ADR-014 | `@WhenTrue`/`@WhenFalse` annotation sugar              | Accepted |
| ADR-015 | Module split: separate `flagzen-key-mapping`           | Accepted |
| ADR-016 | Eager loading of environment variables                 | Accepted |
| ADR-017 | Conflict strategy for key mapping                      | Accepted |
| ADR-018 | Variant annotation array migration                     | Proposed |
| ADR-019 | Spring Boot proxy bean registration                    | Proposed |
| ADR-020 | OpenFeature absence detection strategy                 | Proposed |

---

## Core Architecture (ADR-001 through ADR-007)

### ADR-001: Compile-Time Proxy Generation

#### Problem

FlagZen needs to dispatch method calls on a `@Feature` interface to the active `@Variant` implementation based on the current flag value -- with zero runtime reflection.

#### Alternatives Considered

1. **`java.lang.reflect.Proxy`** -- violates the zero-reflection constraint
2. **ByteBuddy runtime codegen** -- adds a ~3 MB runtime dependency, bytecode not debuggable
3. **Single registry class** -- god class, loses package-private access, poor incremental compilation

#### Decision

Generate one concrete proxy class (`{Feature}_FlagZenProxy`) per `@Feature` interface at compile time via annotation processing. Each proxy is self-contained, lives in the same package as its feature interface, and delegates via map lookup.

#### Consequence

All dispatch is plain Java -- debuggable in the IDE, no external runtime dependencies, incremental compilation only regenerates changed features.

### ADR-002: JavaPoet for Code Generation

#### Problem

The annotation processor must produce well-formatted, correct Java source files that handle generics, checked exceptions, and all return types.

#### Alternatives Considered

1. **Raw `StringBuilder` / text blocks** -- no type safety, manual import management, error-prone
2. **Template engines (Velocity, FreeMarker)** -- stringly-typed, same import problems
3. **Roaster** -- viable but smaller community; JStachio is wrong category

#### Decision

Use JavaPoet (`com.squareup:javapoet`) as a `compileOnly` dependency. It provides type-safe Java source generation with automatic import management.

#### Consequence

Compile-time only -- zero impact on consumer classpath. Generated code is consistently formatted and correct.

### ADR-003: FlagProvider SPI Contract

#### Problem

FlagZen needs a pluggable interface for reading flag values from external sources. The interface must be trivial to implement yet sufficient for polymorphic dispatch.

#### Alternatives Considered

1. **Typed multi-method interface** (`getString`, `getBoolean`, `getInteger`, ...) -- premature; only strings needed for R1
2. **Generic `<T> getValue(String, Class<T>)`** -- pushes type coercion into every provider
3. **Context-aware contract** (`getString(key, EvaluationContext)`) -- context design not finalized for R1

#### Decision

Single method: `Optional<String> getString(String key)`. String values match `@Variant` values for dispatch. `Optional.empty()` signals "flag not configured." Typed accessors deferred to a future release as `default` methods.

#### Consequence

Implementing a new provider is a one-method job. Evolution path is non-breaking (add typed methods as defaults later).

### ADR-004: FeatureDispatcher Design

#### Problem

The runtime entry point (`dispatcher.resolve(CheckoutFlow.class)`) must be testable, injectable by DI frameworks, and support multiple instances with different providers.

#### Alternatives Considered

1. **Static methods on a final class** -- untestable without PowerMock, no DI injection, global state
2. **Abstract class with factory** -- less idiomatic than interface, prevents test doubles
3. **Public concrete implementation** -- couples consumers to internals

#### Decision

`FeatureDispatcher` is a public interface. The concrete `DefaultFeatureDispatcher` is package-private, created via `FlagZen.dispatcher()`. Proxy instances are singletons per feature per dispatcher.

#### Consequence

Interface is trivially mockable. DI frameworks inject it as a bean. Internal implementation can evolve without API breaks.

### ADR-005: Module Structure

#### Problem

FlagZen spans multiple concerns (core, testing, DI integration, provider adapters, reactive). The structure determines what dependencies consumers pay for.

#### Alternatives Considered

1. **Monolith JAR** -- forces Spring/LaunchDarkly/Reactor as transitive deps on everyone
2. **Two modules (core + extensions)** -- extensions module becomes a dependency magnet
3. **Separate annotations and processor modules** -- industry convention is to ship them together

#### Decision

Gradle monorepo with per-concern submodules (`flagzen-core`, `flagzen-test`, `flagzen-env`, `flagzen-spring`, provider adapters, reactive modules). All share a single version. R1 ships core + test only.

#### Consequence

Consumers declare exactly the modules they need. Provider SDK version conflicts are isolated to their respective modules.

### ADR-006: Annotation Retention and Targets

#### Problem

Core annotations (`@Feature`, `@Variant`) and test annotations (`@PinFlag`, `@FlagSource`) need different retention policies.

#### Alternatives Considered

1. **RUNTIME for all** -- signals runtime processing is expected, contradicts zero-reflection
2. **SOURCE for core** -- invisible to annotation processors (they require CLASS minimum)
3. **`@Feature` on FIELD for injection** -- conflates definition with consumption

#### Decision

Core annotations use CLASS retention (consumed by the processor, discarded by the JVM). Test annotations use RUNTIME retention (read by JUnit extension via reflection). `@Variant` is `@Repeatable` for multi-feature implementations.

#### Consequence

Spring cannot discover `@Feature` via classpath scanning -- `flagzen-spring` uses generated metadata instead. Test module does use reflection (acceptable -- it is not core).

### ADR-007: Generated Proxy Visibility

#### Problem

The generated proxy must be accessible to `FeatureDispatcher` (different package) and DI frameworks, but should not be directly constructable by users.

#### Alternatives Considered

1. **Package-private class** -- dispatcher and DI frameworks cannot access it
2. **Public class + public constructor** -- leaks internal dependencies (`FlagProvider`, variant map)
3. **Public class + private constructor + static factory** -- DI frameworks cannot call private constructors without reflection

#### Decision

Public class with package-private constructor. The class is visible for type references; construction is controlled by the dispatcher or DI framework.

#### Consequence

`FeatureDispatcher` discovers proxies via `ServiceLoader`/generated `FeatureMetadata`. DI modules (Spring) use reflection for the package-private constructor -- acceptable outside core.

---

## Dispatch and Conditions (ADR-008 through ADR-010)

### ADR-008: Unified Ordered Dispatch Model

#### Problem

M6 introduces condition-based dispatch (predicates on flag values) alongside M0's value-based dispatch (exact string match). Should they be mutually exclusive per feature, or can they coexist?

#### Alternatives Considered

1. **Mutually exclusive modes** (original design) -- prevents valid "exact match + range fallback" patterns
2. **Implicit ordering by source order** -- Java annotation ordering not guaranteed across compilers
3. **Separate `@DispatchPriority`** -- redundant; `order` on `@Variant` keeps all metadata in one place

#### Decision

Value-based and condition-based variants coexist. An optional `order` attribute on `@Variant` controls dispatch sequence. When only exact matches exist (no conditions), behavior is unchanged -- O(1) map lookup. When `order` is present, variants are evaluated sequentially (first match wins). `@DefaultVariant` is always last.

#### Consequence

Simple cases have zero regression. Mixed dispatch enables "exact match with condition fallback." The processor enforces `order` when ambiguity exists.

### ADR-009: Predicate Instantiation Strategy

#### Problem

Condition-based dispatch needs user-defined predicates instantiated inside the generated proxy without runtime reflection.

#### Alternatives Considered

1. **Custom `FeaturePredicate` interface** -- new abstraction when JDK already provides `Predicate<T>`, `IntPredicate`, etc.
2. **Reflection-based instantiation** -- violates zero-reflection invariant
3. **ServiceLoader discovery** -- runtime, not compile-time

#### Decision

Use JDK functional interfaces (`Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`). The processor validates the correct type per `FeatureType`. Core instantiates via direct `new` call in generated code. Instances are `final` fields, created once at proxy construction.

#### Consequence

Zero new abstractions, zero reflection, AOT/native-image friendly. Predicates cannot have constructor dependencies in core (DI extension deferred to Spring module).

### ADR-010: `@Condition` Annotation Design

#### Problem

How should a predicate be bound to a `@Variant`? Where does it live syntactically, what are its attributes, and where is dispatch order specified?

#### Alternatives Considered

1. **`on` attribute** -- `notOn` is awkward for negation
2. **`is`/`isNot` attributes** -- scales poorly to arrays ("condition is A and B" -- AND or OR?)
3. **`order` inside `@Condition`** -- order applies to the variant, not just the condition
4. **Standalone `@Condition`** -- cross-annotation matching is error-prone with `@Repeatable` `@Variant`

#### Decision

`@Condition` is nested: `@Variant(when = @Condition(matches = X.class), order = 2)`. Attributes are `matches` and `notMatches` (mutually exclusive). `order` lives on `@Variant` to unify ordering across exact-match and condition-based variants (per ADR-008).

#### Consequence

Reads as English. All dispatch metadata is self-contained in one `@Variant` annotation. Negation works without wrapper predicates.

---

## Context and Typing (ADR-011 through ADR-014)

### ADR-011: Context Resolution Order

#### Problem

Multiple context sources can be active simultaneously: explicit parameter, `ContextAccessor` SPI, block-scoped `FlagContext.run()`, and default context. Which wins?

#### Alternatives Considered

1. **Configurable order** -- complexity without practical benefit; no use case found for non-default ordering
2. **Merge contexts from multiple sources** -- ambiguous merge semantics, no flag SDK does this
3. **Scoped before accessor** -- accessor context is typically more specific (per-request identity)

#### Decision

Fixed order: explicit > accessor > scoped > default. Hardcoded, non-configurable. When none provides context, falls back to contextless `getString(key)`.

#### Consequence

Deterministic, intuitive (most specific wins), zero configuration. Inflexible by design -- use explicit context if the default order does not fit.

### ADR-012: FlagContext Carrier Strategy

#### Problem

`FlagContext.run()` needs thread-local storage for `EvaluationContext`. ThreadLocal works everywhere but has virtual-thread overhead. ScopedValue (Java 21+) is better but unavailable on Java 17-20.

#### Alternatives Considered

1. **ThreadLocal only, forever** -- misses ScopedValue performance benefits
2. **Multi-release JAR** -- overkill for a single carrier swap; duplicates most of `FlagContext`
3. **Separate `flagzen-scoped-value` module** -- adds a module for an internal implementation detail

#### Decision

R1: ThreadLocal only. R2: ScopedValue on Java 21+ with ThreadLocal fallback, detected once at class-loading time via `Class.forName("java.lang.ScopedValue")`. Internal `ContextCarrier` strategy pattern keeps `FlagContext` API identical across versions.

#### Consequence

R1 is simple and correct everywhere. R2 transparently leverages ScopedValue. One reflective class lookup at init time (not dispatch-time reflection).

### ADR-013: `@CloseTo` Delta Strategy

#### Problem

DOUBLE-typed feature dispatch must tolerate IEEE 754 floating-point imprecision (e.g., `0.1 + 0.2 != 0.3`). How should tolerance be specified?

#### Alternatives Considered

1. **Global delta on `@Feature`** -- cannot express different tolerances per variant
2. **Exact double matching (`==`)** -- fails for rounding errors from JS backends / JSON parsing
3. **Relative epsilon** -- breaks near zero, overkill for typical small-magnitude flag values

#### Decision

`@CloseTo(value = ..., delta = ...)` with per-variant delta defaulting to `1e-10`. Matching: `Math.abs(flagValue - variantValue) <= delta`. DOUBLE dispatch iterates sequentially (first match wins), consistent with ADR-008. Processor validates delta is positive and finite.

#### Consequence

Default delta handles standard rounding errors transparently. DOUBLE dispatch is O(n) per call (vs O(1) for other types). No compile-time overlap detection -- ordering resolves ambiguity.

### ADR-014: `@WhenTrue`/`@WhenFalse` Annotation Sugar

#### Problem

`@Variant(booleanValue = true)` is verbose for the most common boolean pattern. Also, Java `boolean` has no null, so the sentinel problem makes `booleanValue = true` ambiguous (intended value or default?).

#### Alternatives Considered

1. **`BooleanValue` enum (`TRUE`/`FALSE`/`UNSET`)** -- solves sentinel but worsens verbosity
2. **Meta-annotation composition** -- Java annotation processing does not support meta-annotation inheritance
3. **Separate `@BooleanFeature`** -- fragments the annotation model

#### Decision

`@WhenTrue` and `@WhenFalse` as standalone annotations, normalized to `@Variant(booleanValue = ...)` before validation or code generation. Both support `of()` for multi-feature classes and are `@Repeatable`. Mixed usage with `@Variant` is valid.

#### Consequence

9 characters (`@WhenTrue`) vs 28+ (`@Variant(booleanValue = true)`). Zero divergent processing paths -- normalization feeds into the unified validation and codegen pipeline.

---

## Key Mapping and Providers (ADR-015 through ADR-017)

### ADR-015: Why a Separate Key Mapping Module?

#### Problem

The `flagzen-env` module parses environment variable names into flag keys. The parsing/formatting pipeline (parsers, formatters, conflict handling) is not specific to env vars. Future providers (`flagzen-file`, `flagzen-vault`, `flagzen-consul`) all face the same problem: translating source names to flag keys.

Should this infrastructure live in `flagzen-env`, or be extracted?

#### Alternatives Considered

1. **Keep in `flagzen-env`** (simpler)
   - Pro: Fewer modules to maintain
   - Con: Future providers depend on env-var module just for key mapping -- false dependency

2. **Move to `flagzen-core`** (more central)
   - Pro: Available to all modules
   - Con: Bloats core with provider-specific concerns; violates "minimal core" principle

3. **Extract to `flagzen-key-mapping`** (chosen)
   - Pro: Reusable by all future providers; zero external dependencies
   - Con: Slightly more project structure to maintain

#### Decision

Extract to `flagzen-key-mapping`. This module:

- Has zero external dependencies (pure Java)
- Is reused by `flagzen-env` and future providers
- Solves a general problem (key parsing/formatting) that is separate from flag provision

#### Consequence

`flagzen-env` depends on `flagzen-key-mapping`, which depends on nothing. Future providers can depend on `flagzen-key-mapping` directly without pulling in env-var provider code.

### ADR-016: Why Eager Loading of Environment Variables?

#### Problem

`EnvironmentVariableFlagProvider` must read environment variables and map them to flag keys. The question is **when**: eagerly at construction or lazily on each read?

#### Context

- Environment variables are process-level and effectively immutable (set at startup, not changed)
- Quality priorities: **Performance** (O(1) reads) and **Testability** (deterministic, mockable)

#### Alternatives Considered

1. **Lazy loading** (query `System.getenv()` on every call)
   - Pro: No upfront cost
   - Con: Performance penalty on every flag read; reverse-mapping complexity; testing requires mocking `System.getenv()`

2. **Lazy with per-key cache** (read once per key)
   - Pro: Amortized O(1)
   - Con: Equivalent to eager but with extra complexity; first-call latency; cache needs synchronization

3. **Periodic refresh** (reload on timer)
   - Pro: Could pick up env var changes (hypothetical)
   - Con: Env vars don't change in running JVM; adds unnecessary complexity

4. **Eager loading** (chosen)
   - Pro: O(1) reads with zero allocation; immutable map is inherently thread-safe; deterministic; testable
   - Con: All env vars processed upfront (negligible for typical env var counts)

#### Decision

Eager load all environment variables at construction time into an immutable map. The provider's `getString()` method becomes a pure `Map.get()`.

#### Consequence

- Flag resolution is guaranteed O(1)
- Immutable map requires no synchronization
- Testing accepts a `Supplier<Map<String, String>>` for injection, avoiding mocking frameworks
- Builder accepts a supplier defaulting to `System::getenv`

### ADR-017: Conflict Strategy for Key Mapping

#### Problem

When multiple parsers and/or formatters are configured, different source names can map to the same flag key:

```text
Parser 1 (SCREAMING_SNAKE_CASE + "FLAGZEN_"):
  FLAGZEN_CHECKOUT_FLOW => checkout-flow

Parser 2 (camelCase + "myApp"):
  myAppCheckoutFlow => checkout-flow
```

Both map to `checkout-flow`. What should happen?

#### Context

Conflict risk varies by cardinality:

| Parsers | Formatters |  Risk  |                     Example                     |
| ------- | ---------- | ------ | ----------------------------------------------- |
| 1       | 1          | Low    | Single convention, unlikely collision           |
| N       | 1          | Medium | Multiple parsers may match overlapping env vars |
| 1       | N          | Medium | Different formatters reduce surface             |
| N       | N          | High   | Cartesian product maximizes collisions          |

#### Alternatives Considered

1. **Silent last-write-wins** (no handling)
   - Con: Silent data loss; developer doesn't know a flag value was overwritten

2. **Always ERROR** (fail on any conflict)
   - Con: Too strict for migration scenarios (intentional overlapping conventions during transition)

3. **Priority-based resolution** (explicit ordering)
   - Con: Still silently discards one value; adds complexity

4. **Warn + last-wins** (chosen)
   - Pro: Alerts to conflicts at construction and first access; last-wins is deterministic
   - Con: First-access warning requires mutable state (set of warned keys)

#### Decision

Introduce `ConflictStrategy` enum with `WARN` and `ERROR` values. Cardinality-based defaults:

| Parsers | Formatters | Default |
| ------- | ---------- | ------- |
| 1       | 1          | WARN    |
| N       | 1          | WARN    |
| 1       | N          | WARN    |
| N       | N          | ERROR   |

High-cardinality configs (N x N) default to `ERROR` to fail fast. Lower cardinalities default to `WARN` for flexibility.

#### Consequence

Conflicts are never silent. `WARN` logs at construction and first access; `ERROR` fails immediately. Developers must intentionally choose `onConflict(WARN)` for high-cardinality configs, signaling awareness.

---

## Extensions (ADR-018 through ADR-020)

### ADR-018: Variant Annotation Array Migration

#### Problem

The `@Variant` annotation currently uses scalar elements (`String value()`, `int intValue()`). When one implementation should handle multiple flag values, developers repeat the annotation:

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
@Variant(value = "LEGACY", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

The `doubleValue()` element already supports arrays, creating inconsistency. Scalars also use sentinel values (`Integer.MIN_VALUE`, `Long.MIN_VALUE`), which cannot be actual flag values.

#### Alternatives Considered

1. **New array elements alongside scalars** (new `@Variant(values = {...})` attribute)
   - Pro: Backward compatible
   - Con: Two ways to express the same thing; processor complexity; interaction between scalar and array unclear
   - Rejected: Pre-1.0; compatibility not a concern

2. **Keep scalars, rely only on @Repeatable** (status quo)
   - Con: Doesn't solve the stated problem; sentinels remain

3. **In-place array migration** (chosen)
   - Pro: Single clean API; eliminates sentinels; processor simplification
   - Con: Binary incompatible with pre-migration compiled code

#### Decision

Change element types directly:

- `String value() default ""` -> `String[] value() default ""`
- `int intValue() default Integer.MIN_VALUE` -> `int[] intValue() default {}`
- `long longValue() default Long.MIN_VALUE` -> `long[] longValue() default {}`

Java auto-wraps scalars to single-element arrays, so existing source code like `@Variant(value = "X")` compiles unchanged.

#### Consequence

Developers can now write multi-value annotations in a single line:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

Integer/long values like `Integer.MIN_VALUE` become valid flag values. Processor "not set" detection uses array length instead of sentinel comparison.

### ADR-019: Spring Bean Registration Strategy

#### Problem

`flagzen-spring` needs to register one bean per discovered `@Feature` interface. The number and types are not known at compile time (depends on which `@Feature` interfaces the consumer's processor generated).

Requirements:

1. Register beans with correct feature interface type (for `@Autowired CheckoutFlow`)
2. Resolve lazily via `FeatureDispatcher.resolve()` after dispatcher bean exists
3. Integrate cleanly with Spring Boot `@AutoConfiguration` patterns
4. Handle zero metadata gracefully

#### Alternatives Considered

1. **`BeanDefinitionRegistryPostProcessor`**
   - Con: Complex lifecycle; fragile interaction with other post-processors; requires additional conditional logic

2. **`FactoryBean<T>` per feature**
   - Con: Cannot register dynamic `@Bean` methods; would need registrar anyway, combining two mechanisms

3. **Programmatic `GenericApplicationContext.registerBean()`**
   - Con: Bypasses auto-configuration lifecycle; doesn't respect `@ConditionalOnMissingBean`

4. **`ImportBeanDefinitionRegistrar`** (chosen)
   - Pro: Standard Spring mechanism for dynamic bean registration; tied to `@Import`; respects auto-configuration lifecycle

#### Decision

Implement `FeatureProxyRegistrar` as `ImportBeanDefinitionRegistrar` imported via `@Import(FeatureProxyRegistrar.class)` from `FlagZenAutoConfiguration`.

#### Consequence

- Per-feature beans are registered dynamically when auto-configuration runs
- Beans are lazy-initialized and singleton-scoped
- Clean separation: `FlagZenAutoConfiguration` handles `FlagProvider`/`FeatureDispatcher`; registrar handles per-feature beans
- ServiceLoader discovers feature metadata at registration time

### ADR-020: OpenFeature Absence Detection

#### Problem

`OpenFeatureFlagProvider` implements `FlagProvider`, whose contract returns `Optional<String>` (absent = "flag not set"). OpenFeature's `Client` API does not return optionals. Calling `client.getStringValue(key, default)` always returns a value (either resolved or the caller's default), with no way to distinguish.

How can the adapter detect when a flag was genuinely resolved vs. when OpenFeature returned the default?

#### Alternatives Considered

1. **Sentinel value detection** (pass unique sentinel, check if returned)
   - Con: Fragile; a real flag value could equal the sentinel; no safe sentinel for boolean
   - Rejected: Correctness guaranteed to fail for booleans

2. **Two-call strategy** (call with two different sentinels)
   - Con: Doubles evaluation calls (performance penalty); race condition (flag could change between calls)
   - Rejected: Performance overhead; still fails for booleans

3. **Exception-based detection** (configure to throw on missing)
   - Con: OpenFeature SDK doesn't support this
   - Rejected: Not supported by API

4. **Reason-based detection** (use `getStringDetails()`, inspect reason field) (chosen)
   - Pro: Works for all types; no magic values; handles all error cases; forward-compatible
   - Con: Depends on provider's correct implementation of `reason` semantics

#### Decision

Call `client.getStringDetails(key, "")` and inspect the `FlagEvaluationDetails` response:

1. If `errorCode` is non-null -> evaluation failed, return empty
2. If `reason == "DEFAULT"` -> default was used, return empty
3. Otherwise -> flag was resolved, return the value

#### Consequence

The adapter correctly handles all flag value types. Relies on OpenFeature providers correctly implementing the `reason` field as specified (not a concern in practice, but documents a dependency).

---

## Theme: "Simple First, Complexity Earned"

Across all decisions, FlagZen follows a principle: **choose the simplest solution that solves the problem, and accept complexity only when it is necessary.**

Examples:

- **ADR-001**: One proxy per feature is simpler than a registry class, so we chose one-per-feature
- **ADR-003**: Single `getString` method is simpler than a typed multi-method interface, so we chose it
- **ADR-009**: JDK predicates are simpler than a custom interface, so we used the JDK
- **ADR-016**: Eager loading is simpler than lazy + caching, so we chose eager
- **ADR-017**: `WARN` + last-wins is simpler than priority-based resolution, so we chose it
- **ADR-020**: Reason-based detection is simpler than sentinel-based, so we chose it

This is why FlagZen has fewer features than some competitors (e.g., no gradual rollout, no targeting rules), but the features it has are clean and maintainable.

## Quality Attributes Prioritized

Across all decisions, FlagZen optimizes for:

1. **Correctness** (no silent failures, fail fast)
2. **Performance** (O(1) dispatch, zero reflection)
3. **Maintainability** (simple designs, modular structure)
4. **Testability** (easy to mock/control flags)
5. **Portability** (GraalVM native image, no external deps in core)

Each decision trades off these attributes. For example, ADR-017 prioritizes correctness (no silent conflicts) over simplicity (eager-loading all env vars).

## Future Decisions

These ADRs are not immutable. As FlagZen evolves:

- **Cross-module variants** (ADR mentioned as Release 2 item) may lead to runtime startup validation
- **Configuration properties** (Spring) may be added for fine-tuning
- **Multi-value providers** (handling flags with multiple dimensions) may require new SPI methods

New decisions will follow the same principle: simple first, earned complexity.

## Further Reading

- [ADR-001](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-001-proxy-generation-strategy.md) -- Proxy Generation Strategy (full rationale)
- [ADR-002](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-002-code-generation-tooling.md) -- Code Generation Tooling (full rationale)
- [ADR-003](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-003-flagprovider-contract.md) -- FlagProvider SPI Contract (full rationale)
- [ADR-004](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-004-feature-dispatcher-design.md) -- FeatureDispatcher Design (full rationale)
- [ADR-005](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-005-module-structure.md) -- Module Structure (full rationale)
- [ADR-006](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-006-annotation-retention-and-targets.md) -- Annotation Retention and Targets (full rationale)
- [ADR-007](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-007-generated-proxy-visibility.md) -- Generated Proxy Visibility (full rationale)
- [ADR-008](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-008-mutually-exclusive-dispatch-modes.md) -- Unified Ordered Dispatch Model (full rationale)
- [ADR-009](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-009-predicate-instantiation-strategy.md) -- Predicate Instantiation Strategy (full rationale)
- [ADR-010](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-010-condition-annotation-nesting.md) -- Condition Annotation Design (full rationale)
- [ADR-011](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-011-context-resolution-order.md) -- Context Resolution Order (full rationale)
- [ADR-012](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-012-flagcontext-carrier-strategy.md) -- FlagContext Carrier Strategy (full rationale)
- [ADR-013](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-013-closeto-delta-strategy.md) -- CloseTo Delta Strategy (full rationale)
- [ADR-014](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-014-whentrue-whenfalse-sugar.md) -- WhenTrue/WhenFalse Annotation Sugar (full rationale)
- [ADR-015](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-015-key-mapping-module-split.md) -- Key Mapping Module Split (full rationale)
- [ADR-016](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-016-eager-loading-strategy.md) -- Eager Loading Strategy (full rationale)
- [ADR-017](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-017-conflict-strategy-design.md) -- Conflict Strategy Design (full rationale)
- [ADR-018](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-018-variant-array-migration-strategy.md) -- Variant Array Migration (full rationale)
- [ADR-019](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-019-proxy-bean-registration-strategy.md) -- Spring Bean Registration (full rationale)
- [ADR-020](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-020-absent-flag-detection-strategy.md) -- OpenFeature Absence Detection (full rationale)
- [Architecture Explanation](architecture.md) -- how all these decisions fit together
