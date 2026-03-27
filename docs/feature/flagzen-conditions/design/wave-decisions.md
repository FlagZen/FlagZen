# Wave Decisions -- Condition Predicates (flagzen-conditions)

## Decision Log

### WD-01: No new Gradle submodule for conditions

**Decision**: All condition predicate types (`FeaturePredicate`, `@Condition`) live in flagzen-core. No `flagzen-conditions` submodule.

**Rationale**: Condition dispatch is a core capability, not an extension. It modifies the annotation processor and proxy generator -- both are in flagzen-core. Creating a separate module would force a circular dependency (core's processor needs condition annotations, condition module needs core's types).

**Impact**: Zero module structure change. Consumers get conditions by depending on flagzen-core.

### WD-02: @Variant.value default changes from required to ""

**Decision**: The `@Variant.value` attribute changes from required (no default) to defaulting to `""`. Condition-based variants do not use `value`.

**Rationale**: Java annotations cannot have a truly optional attribute without a default. Condition-based variants declare `@Variant(when = @Condition(...))` without a value. The processor validates: value-based variants must have non-empty `value`, condition-based variants ignore `value`.

**Impact**: Source-compatible. All existing `@Variant("VALUE")` declarations continue to compile and work identically.

### WD-03: Sentinel class for @Condition default

**Decision**: `@Variant.when` defaults to a sentinel `@Condition` whose `on` references a sentinel class. The processor detects the sentinel to mean "no condition."

**Rationale**: Java annotation attributes cannot be null. A sentinel pattern is the standard Java approach (see `@Autowired(required = true)`, `@Variant(of = void.class)`). The sentinel class is internal (not public API).

**Impact**: Minor implementation detail. The sentinel class is never instantiated by user code.

### WD-04: FeatureMetadata SPI extension approach deferred to crafter

**Decision**: The architecture does not prescribe how `FeatureMetadata` adapts to condition-based proxies. The crafter determines whether to extend the interface, add overloaded methods, or use a parallel SPI.

**Rationale**: This is an internal implementation detail. The architecture requires that `ServiceLoader` discovery continues to work and that `FeatureDispatcher` can construct both proxy types. The HOW is the crafter's decision.

**Impact**: None on public API. The `FeatureMetadata` SPI may gain methods in a minor-version-compatible way.

### WD-05: Predicate thread safety is user responsibility

**Decision**: FlagZen documents that `FeaturePredicate` implementations must be thread-safe if used in multi-threaded applications. FlagZen does not provide synchronization.

**Rationale**: Predicates are user code. FlagZen cannot know whether a predicate accesses shared mutable state. Adding synchronization would penalize the common case (stateless predicates) for the uncommon case. Consistent with `FlagProvider` thread safety contract.

**Impact**: Javadoc on `FeaturePredicate` documents this contract.

### WD-06: Condition-based features do not query FlagProvider

**Decision**: When a `@Feature` uses condition-based dispatch, the generated proxy does not call `FlagProvider.getString()`. Dispatch is entirely predicate-driven.

**Rationale**: Condition-based dispatch evaluates predicates against `EvaluationContext`. There is no flag value to look up. The `FlagProvider` is irrelevant for condition dispatch. This also means condition-based features work without any `FlagProvider` configured -- only `EvaluationContext` is needed.

**Impact**: Condition-based proxies have no `FlagProvider` dependency. This simplifies the proxy constructor and removes a potential error source (missing provider).

### WD-07: REQUIRED strategy semantics for conditions

**Decision**: `FallbackStrategy.REQUIRED` on condition-based features requires `@DefaultVariant` at compile time.

**Rationale**: For value-based features with enum, `REQUIRED` means "all enum values must have implementations." For conditions, predicate completeness cannot be verified at compile time (predicates are runtime-evaluated). The safest interpretation of `REQUIRED` is "there must be a fallback" -- hence `@DefaultVariant` is mandatory.

**Impact**: Developers using `REQUIRED` (which is not the default -- `EXCEPTION` is) must provide `@DefaultVariant`. This is consistent with the principle that `REQUIRED` = compile-time guarantee of completeness.

### WD-08: Exception propagation from predicates

**Decision**: FlagZen does not catch, wrap, or log exceptions thrown by `FeaturePredicate.test()`. They propagate directly to the caller.

**Rationale**: Predicates are user code with user-defined semantics. Catching exceptions would hide bugs. Wrapping exceptions would change the stack trace. The caller is in the best position to handle predicate failures. This is consistent with how value-based dispatch propagates `FlagProvider` exceptions.

**Impact**: If a predicate throws, the caller sees the original exception. No FlagZen wrapper.
