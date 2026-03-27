# ADR-009: Predicate Instantiation Strategy

## Status

Accepted

## Context

Condition-based dispatch requires instantiating user-defined `FeaturePredicate` implementations inside the generated proxy. The proxy is constructed at `FeatureDispatcher.resolve()` time and lives as a singleton for the lifetime of the dispatcher. The instantiation strategy must satisfy three constraints:

1. **Zero runtime reflection in flagzen-core** (architectural invariant since M0)
2. **Compile-time safety** -- invalid predicates caught before runtime
3. **Extensibility** -- Spring DI support as an extension module (not in core)

## Decision

Core module instantiates predicates via **no-arg constructor** called directly in generated code (e.g., `new IsEnterprise()`). The annotation processor validates that the predicate class:

- Implements `FeaturePredicate`
- Is not abstract
- Has an accessible no-arg constructor

Predicate instances are created once at proxy construction time and stored as `final` fields. They are reused across all method invocations.

Spring DI support (US-CP-08, deferred) will extend flagzen-spring to resolve `@Component`-annotated predicates from the `ApplicationContext`. When flagzen-spring is present, the compile-time no-arg constructor check is relaxed for classes bearing Spring stereotype annotations. This is an extension of the existing flagzen-spring module.

## Alternatives Considered

### 1. ServiceLoader-based predicate discovery

Register `FeaturePredicate` implementations via `META-INF/services/`. The proxy discovers them at runtime.

**Rejected because**: ServiceLoader discovery is runtime, not compile-time. The processor cannot validate that the referenced predicate class exists and is registered. Also introduces indirection between the `@Condition(on = X.class)` annotation and the actual instance -- the class reference in the annotation would become a key rather than a direct type reference. Adds unnecessary complexity for a problem that direct instantiation solves.

### 2. Reflection-based instantiation

Use `Class.forName(name).getDeclaredConstructor().newInstance()` in the generated proxy.

**Rejected because**: Violates the zero-runtime-reflection invariant of flagzen-core (ADR-001). Reflection is slower, produces less helpful error messages, and is not compatible with GraalVM native image without additional configuration. Direct constructor call is simpler, faster, and AOT-friendly.

### 3. Factory method pattern

Require predicates to expose a `static FeaturePredicate create()` factory method instead of a no-arg constructor.

**Rejected because**: Adds ceremony without benefit. A no-arg constructor is the simplest instantiation contract. Java developers expect `new X()`. A factory method would require additional annotation attributes or conventions to specify the method name, and the processor validation becomes more complex. If a predicate needs complex initialization, it can do so in its constructor body.

### 4. Predicate instances passed to proxy externally

Instead of the proxy instantiating predicates, `FeatureDispatcher` creates them and passes them to the proxy constructor. This centralizes instantiation and allows the dispatcher to use different strategies.

**Rejected because**: This shifts instantiation responsibility to the runtime API layer (FeatureDispatcher/FeatureMetadata), which means the generated metadata must know about predicate classes. This is viable but deferred -- it may be the approach used when Spring DI support is added. For core (no-DI) usage, direct instantiation in the proxy is simpler and self-contained.

## Consequences

### Positive

- Zero reflection -- generated code calls `new IsEnterprise()` directly
- Compile-time validation -- processor catches invalid predicate classes before runtime
- AOT/native-image friendly -- no reflective access needed
- Simple mental model -- predicates are POJOs with no-arg constructors
- Consistent with how variant classes are already instantiated in M0 proxies

### Negative

- Predicates cannot have constructor dependencies in core (no DI)
- Developers needing DI must wait for flagzen-spring extension or use service locator workarounds
- Predicate instances are shared across threads (user responsibility for thread safety)
