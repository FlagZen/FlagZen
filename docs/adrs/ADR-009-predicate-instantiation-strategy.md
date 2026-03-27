# ADR-009: Predicate Instantiation Strategy

## Status

Accepted (updated: predicates use JDK functional interfaces, not custom FeaturePredicate)

## Context

Condition-based dispatch requires instantiating user-defined predicates inside the generated proxy. The predicates test **flag values** (not EvaluationContext) — e.g., "is this integer above 5?" or "does this string start with v2?". The instantiation strategy must satisfy three constraints:

1. **Zero runtime reflection in flagzen-core** (architectural invariant since M0)
2. **Compile-time safety** — invalid predicates caught before runtime
3. **Extensibility** — Spring DI support as an extension module (not in core)

## Decision

### Predicate types: JDK functional interfaces

Predicates use the standard `java.util.function` predicate types — no custom `FeaturePredicate` interface:

| Feature type | Predicate interface | Example |
|---|---|---|
| STRING | `Predicate<String>` | `class StartsWithV2 implements Predicate<String>` |
| INT | `IntPredicate` | `class HighRetryRange implements IntPredicate` |
| LONG | `LongPredicate` | `class AboveRateLimit implements LongPredicate` |
| DOUBLE | `DoublePredicate` | `class HighSamplingRate implements DoublePredicate` |
| BOOLEAN | — | Only two values, use exact match |

The annotation processor validates that the class referenced in `@Condition(matches = X.class)` implements the correct predicate type for the feature's declared `FeatureType`.

### Instantiation: no-arg constructor

Core module instantiates predicates via **no-arg constructor** called directly in generated code (e.g., `new HighRetryRange()`). The annotation processor validates that the predicate class:

- Implements the correct JDK predicate interface for the feature type
- Is not abstract
- Has an accessible no-arg constructor

Predicate instances are created once at proxy construction time and stored as `final` fields. They are reused across all method invocations.

Spring DI support (deferred) will extend flagzen-spring to resolve `@Component`-annotated predicates from the `ApplicationContext`.

## Alternatives Considered

### 1. Custom `FeaturePredicate` interface

Define `com.flagzen.FeaturePredicate` as the single predicate type, with `boolean test(EvaluationContext ctx)`.

**Rejected because**: (a) Predicates test flag values, not EvaluationContext — context-based targeting belongs in the flag provider. (b) Introduces a new interface when the JDK already provides `Predicate<T>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`. (c) Developers already know the JDK predicates — zero new abstractions to learn.

### 2. Reflection-based instantiation

Use `Class.forName(name).getDeclaredConstructor().newInstance()` in the generated proxy.

**Rejected because**: Violates the zero-runtime-reflection invariant (ADR-001). Direct constructor call is simpler, faster, and AOT-friendly.

### 3. ServiceLoader-based predicate discovery

Register predicate implementations via `META-INF/services/`.

**Rejected because**: Runtime discovery, not compile-time. Cannot validate predicate existence or type compatibility at compile time.

## Consequences

### Positive

- Zero new abstractions — uses JDK's own predicate interfaces
- Zero reflection — generated code calls `new HighRetryRange()` directly
- Compile-time validation — processor validates correct predicate type per feature type
- Type-safe — INT feature requires `IntPredicate`, STRING requires `Predicate<String>`
- AOT/native-image friendly

### Negative

- Predicates cannot have constructor dependencies in core (no DI)
- Predicate instances are shared across threads (user must ensure thread safety)
- `Predicate<String>` uses generics which are erased — processor validates via type hierarchy analysis, not runtime type checking
