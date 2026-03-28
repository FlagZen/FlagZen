# Design Decisions Explained

Summary of FlagZen's major architectural decisions and the reasoning behind them.

## Overview

FlagZen's design reflects a series of intentional choices, each made to solve a specific problem or optimize for a particular quality attribute. This document summarizes the decisions codified in ADRs 015-020 and explains the trade-offs.

## Decision Index

| ADR | Decision | Status |
|-----|----------|--------|
| ADR-015 | Module split: separate `flagzen-key-mapping` | Accepted |
| ADR-016 | Eager loading of environment variables | Accepted |
| ADR-017 | Conflict strategy for key mapping | Accepted |
| ADR-018 | Variant annotation array migration | Proposed |
| ADR-019 | Spring Boot proxy bean registration | Proposed |
| ADR-020 | OpenFeature absence detection strategy | Proposed |

## ADR-015: Why a Separate Key Mapping Module?

### The Problem

The `flagzen-env` module parses environment variable names into flag keys. The parsing/formatting pipeline (parsers, formatters, conflict handling) is not specific to env vars. Future providers (`flagzen-file`, `flagzen-vault`, `flagzen-consul`) all face the same problem: translating source names to flag keys.

Should this infrastructure live in `flagzen-env`, or be extracted?

### Alternatives Considered

1. **Keep in `flagzen-env`** (simpler)
   - Pro: Fewer modules to maintain
   - Con: Future providers depend on env-var module just for key mapping — false dependency

2. **Move to `flagzen-core`** (more central)
   - Pro: Available to all modules
   - Con: Bloats core with provider-specific concerns; violates "minimal core" principle

3. **Extract to `flagzen-key-mapping`** (chosen)
   - Pro: Reusable by all future providers; zero external dependencies
   - Con: Slightly more project structure to maintain

### Decision

Extract to `flagzen-key-mapping`. This module:

- Has zero external dependencies (pure Java)
- Is reused by `flagzen-env` and future providers
- Solves a general problem (key parsing/formatting) that is separate from flag provision

### Consequence

`flagzen-env` depends on `flagzen-key-mapping`, which depends on nothing. Future providers can depend on `flagzen-key-mapping` directly without pulling in env-var provider code.

## ADR-016: Why Eager Loading of Environment Variables?

### The Problem

`EnvironmentVariableFlagProvider` must read environment variables and map them to flag keys. The question is **when**: eagerly at construction or lazily on each read?

### Context

- Environment variables are process-level and effectively immutable (set at startup, not changed)
- Quality priorities: **Performance** (O(1) reads) and **Testability** (deterministic, mockable)

### Alternatives Considered

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

### Decision

Eager load all environment variables at construction time into an immutable map. The provider's `getString()` method becomes a pure `Map.get()`.

### Consequence

- Flag resolution is guaranteed O(1)
- Immutable map requires no synchronization
- Testing accepts a `Supplier<Map<String, String>>` for injection, avoiding mocking frameworks
- Builder accepts a supplier defaulting to `System::getenv`

## ADR-017: Conflict Strategy for Key Mapping

### The Problem

When multiple parsers and/or formatters are configured, different source names can map to the same flag key:

```
Parser 1 (SCREAMING_SNAKE_CASE + "FLAGZEN_"):
  FLAGZEN_CHECKOUT_FLOW => checkout-flow

Parser 2 (camelCase + "myApp"):
  myAppCheckoutFlow => checkout-flow
```

Both map to `checkout-flow`. What should happen?

### Context

Conflict risk varies by cardinality:

| Parsers | Formatters | Risk | Example |
|---------|------------|------|---------|
| 1 | 1 | Low | Single convention, unlikely collision |
| N | 1 | Medium | Multiple parsers may match overlapping env vars |
| 1 | N | Medium | Different formatters reduce surface |
| N | N | High | Cartesian product maximizes collisions |

### Alternatives Considered

1. **Silent last-write-wins** (no handling)
   - Con: Silent data loss; developer doesn't know a flag value was overwritten

2. **Always ERROR** (fail on any conflict)
   - Con: Too strict for migration scenarios (intentional overlapping conventions during transition)

3. **Priority-based resolution** (explicit ordering)
   - Con: Still silently discards one value; adds complexity

4. **Warn + last-wins** (chosen)
   - Pro: Alerts to conflicts at construction and first access; last-wins is deterministic
   - Con: First-access warning requires mutable state (set of warned keys)

### Decision

Introduce `ConflictStrategy` enum with `WARN` and `ERROR` values. Cardinality-based defaults:

| Parsers | Formatters | Default |
|---------|------------|---------|
| 1 | 1 | WARN |
| N | 1 | WARN |
| 1 | N | WARN |
| N | N | ERROR |

High-cardinality configs (N×N) default to `ERROR` to fail fast. Lower cardinalities default to `WARN` for flexibility.

### Consequence

Conflicts are never silent. `WARN` logs at construction and first access; `ERROR` fails immediately. Developers must intentionally choose `onConflict(WARN)` for high-cardinality configs, signaling awareness.

## ADR-018: Variant Annotation Array Migration

### The Problem

The `@Variant` annotation currently uses scalar elements (`String value()`, `int intValue()`). When one implementation should handle multiple flag values, developers repeat the annotation:

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
@Variant(value = "LEGACY", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

The `doubleValue()` element already supports arrays, creating inconsistency. Scalars also use sentinel values (`Integer.MIN_VALUE`, `Long.MIN_VALUE`), which cannot be actual flag values.

### Alternatives Considered

1. **New array elements alongside scalars** (new `@Variant(values = {...})` attribute)
   - Pro: Backward compatible
   - Con: Two ways to express the same thing; processor complexity; interaction between scalar and array unclear
   - Rejected: Pre-1.0; compatibility not a concern

2. **Keep scalars, rely only on @Repeatable** (status quo)
   - Con: Doesn't solve the stated problem; sentinels remain

3. **In-place array migration** (chosen)
   - Pro: Single clean API; eliminates sentinels; processor simplification
   - Con: Binary incompatible with pre-migration compiled code

### Decision

Change element types directly:

- `String value() default ""` → `String[] value() default ""`
- `int intValue() default Integer.MIN_VALUE` → `int[] intValue() default {}`
- `long longValue() default Long.MIN_VALUE` → `long[] longValue() default {}`

Java auto-wraps scalars to single-element arrays, so existing source code like `@Variant(value = "X")` compiles unchanged.

### Consequence

Developers can now write multi-value annotations in a single line:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

Integer/long values like `Integer.MIN_VALUE` become valid flag values. Processor "not set" detection uses array length instead of sentinel comparison.

## ADR-019: Spring Bean Registration Strategy

### The Problem

`flagzen-spring` needs to register one bean per discovered `@Feature` interface. The number and types are not known at compile time (depends on which `@Feature` interfaces the consumer's processor generated).

Requirements:
1. Register beans with correct feature interface type (for `@Autowired CheckoutFlow`)
2. Resolve lazily via `FeatureDispatcher.resolve()` after dispatcher bean exists
3. Integrate cleanly with Spring Boot `@AutoConfiguration` patterns
4. Handle zero metadata gracefully

### Alternatives Considered

1. **`BeanDefinitionRegistryPostProcessor`**
   - Con: Complex lifecycle; fragile interaction with other post-processors; requires additional conditional logic

2. **`FactoryBean<T>` per feature**
   - Con: Cannot register dynamic `@Bean` methods; would need registrar anyway, combining two mechanisms

3. **Programmatic `GenericApplicationContext.registerBean()`**
   - Con: Bypasses auto-configuration lifecycle; doesn't respect `@ConditionalOnMissingBean`

4. **`ImportBeanDefinitionRegistrar`** (chosen)
   - Pro: Standard Spring mechanism for dynamic bean registration; tied to `@Import`; respects auto-configuration lifecycle

### Decision

Implement `FeatureProxyRegistrar` as `ImportBeanDefinitionRegistrar` imported via `@Import(FeatureProxyRegistrar.class)` from `FlagZenAutoConfiguration`.

### Consequence

- Per-feature beans are registered dynamically when auto-configuration runs
- Beans are lazy-initialized and singleton-scoped
- Clean separation: `FlagZenAutoConfiguration` handles `FlagProvider`/`FeatureDispatcher`; registrar handles per-feature beans
- ServiceLoader discovers feature metadata at registration time

## ADR-020: OpenFeature Absence Detection

### The Problem

`OpenFeatureFlagProvider` implements `FlagProvider`, whose contract returns `Optional<String>` (absent = "flag not set"). OpenFeature's `Client` API does not return optionals. Calling `client.getStringValue(key, default)` always returns a value (either resolved or the caller's default), with no way to distinguish.

How can the adapter detect when a flag was genuinely resolved vs. when OpenFeature returned the default?

### Alternatives Considered

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

### Decision

Call `client.getStringDetails(key, "")` and inspect the `FlagEvaluationDetails` response:

1. If `errorCode` is non-null → evaluation failed, return empty
2. If `reason == "DEFAULT"` → default was used, return empty
3. Otherwise → flag was resolved, return the value

### Consequence

The adapter correctly handles all flag value types. Relies on OpenFeature providers correctly implementing the `reason` field as specified (not a concern in practice, but documents a dependency).

## Theme: "Simple First, Complexity Earned"

Across all decisions, FlagZen follows a principle: **choose the simplest solution that solves the problem, and accept complexity only when it is necessary.**

Examples:

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

- [ADR-015](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-015-key-mapping-module-split.md) — Key Mapping Module Split (full rationale)
- [ADR-016](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-016-eager-loading-strategy.md) — Eager Loading Strategy (full rationale)
- [ADR-017](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-017-conflict-strategy-design.md) — Conflict Strategy Design (full rationale)
- [ADR-018](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-018-variant-array-migration-strategy.md) — Variant Array Migration (full rationale)
- [ADR-019](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-019-proxy-bean-registration-strategy.md) — Spring Bean Registration (full rationale)
- [ADR-020](https://github.com/FlagZen/FlagZen/blob/main/docs/adrs/ADR-020-absent-flag-detection-strategy.md) — OpenFeature Absence Detection (full rationale)
- [Architecture Explanation](architecture.md) — how all these decisions fit together
