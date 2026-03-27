# Wave Decisions -- Condition Predicates (flagzen-conditions)

## Decision Log

### WD-01: No new Gradle submodule for conditions

**Decision**: All condition predicate types (`@Condition`) live in flagzen-core. No `flagzen-conditions` submodule.

**Rationale**: Condition dispatch is a core capability, not an extension. It modifies the annotation processor and proxy generator -- both are in flagzen-core. Creating a separate module would force a circular dependency (core's processor needs condition annotations, condition module needs core's types).

**Impact**: Zero module structure change. Consumers get conditions by depending on flagzen-core.

### WD-02: @Variant.value default changes from required to ""

**Decision**: The `@Variant.value` attribute changes from required (no default) to defaulting to `""`. Condition-based variants do not use `value`.

**Rationale**: Java annotations cannot have a truly optional attribute without a default. Condition-based variants declare `@Variant(when = @Condition(...))` without a value. The processor validates: value-based variants must have non-empty `value`, condition-based variants ignore `value`.

**Impact**: Source-compatible. All existing `@Variant("VALUE")` declarations continue to compile and work identically.

### WD-03: Sentinel class for @Condition default

**Decision**: `@Variant.when` defaults to a sentinel `@Condition` whose `matches` references a sentinel class. The processor detects the sentinel to mean "no condition."

**Rationale**: Java annotation attributes cannot be null. A sentinel pattern is the standard Java approach (see `@Autowired(required = true)`, `@Variant(of = void.class)`). The sentinel class is internal (not public API).

**Impact**: Minor implementation detail. The sentinel class is never instantiated by user code.

### WD-04: FeatureMetadata SPI extension approach deferred to crafter

**Decision**: The architecture does not prescribe how `FeatureMetadata` adapts to condition-based proxies. The crafter determines whether to extend the interface, add overloaded methods, or use a parallel SPI.

**Rationale**: This is an internal implementation detail. The architecture requires that `ServiceLoader` discovery continues to work and that `FeatureDispatcher` can construct both proxy types. The HOW is the crafter's decision.

**Impact**: None on public API. The `FeatureMetadata` SPI may gain methods in a minor-version-compatible way.

### WD-05: Predicate thread safety is user responsibility

**Decision**: FlagZen documents that predicate implementations must be thread-safe if used in multi-threaded applications. FlagZen does not provide synchronization.

**Rationale**: Predicates are user code. FlagZen cannot know whether a predicate accesses shared mutable state. Adding synchronization would penalize the common case (stateless predicates) for the uncommon case. Consistent with `FlagProvider` thread safety contract.

**Impact**: Javadoc on predicate usage documents this contract.

### WD-06: No FeaturePredicate interface -- use JDK functional interfaces

**Decision**: No custom `FeaturePredicate` interface. Predicates implement standard JDK functional interfaces: `Predicate<String>` for STRING features, `IntPredicate` for INT, `LongPredicate` for LONG, `DoublePredicate` for DOUBLE. The processor validates that the predicate type matches `@Feature(type=...)`.

**Rationale**: JDK functional interfaces are universally known, require no additional dependency, and make predicates testable without FlagZen imports. Predicates test flag values, not EvaluationContext -- a custom interface adds nothing over the JDK types. This also removes the M1 (EvaluationContext) dependency from flagzen-conditions.

**Impact**: One fewer public API type. Users implement standard interfaces they already know. No FlagZen-specific predicate interface to learn or import. See updated ADR-008.

### WD-07: Predicates test flag values, not EvaluationContext

**Decision**: Predicates receive the flag value (string, int, long, or double) as their input, not an `EvaluationContext`. This removes the M1 dependency from the conditions feature.

**Rationale**: The flag value is the natural input for condition dispatch. A `Predicate<String>` that tests whether a flag value starts with "enterprise-" is simpler and more composable than one that extracts attributes from an opaque context object. FlagProvider already resolves the flag value -- predicates should operate on that resolved value.

**Impact**: No dependency on M1. Conditions can be implemented before or independently of EvaluationContext.

### WD-08: Replace `on` with `matches`/`notMatches` in @Condition

**Decision**: `@Condition` uses `matches` (and `notMatches` for negation) instead of `on` to reference the predicate class. `matches` and `notMatches` are mutually exclusive.

**Rationale**: `matches` is more descriptive -- it reads as `@Condition(matches = Enterprise.class)`, which clearly communicates intent. `notMatches` provides built-in negation without requiring users to write a separate negation predicate class. The `on` name was too generic.

**Impact**: Better readability at usage sites. One annotation attribute change.

### WD-09: Move `order` from @Condition to @Variant

**Decision**: The `order` attribute lives on `@Variant`, not on `@Condition`. Declaration: `@Variant(when = @Condition(matches = Enterprise.class), order = 1)`.

**Rationale**: `order` determines variant evaluation sequence, not condition behavior. A variant's position in the dispatch sequence is a property of the variant, not the condition. This also enables `order` on value-based variants for unified ordered dispatch.

**Impact**: Cleaner separation of concerns. Enables unified ordered dispatch where exact matches and conditions can coexist with explicit ordering.

### WD-10: Unified ordered dispatch replaces mutually exclusive modes

**Decision**: Exact-match variants and condition-based variants can coexist on the same `@Feature`. The `order` attribute on `@Variant` controls evaluation sequence. When no `order` is specified, the proxy uses map-based O(1) lookup (no regression). When `order` is present, variants are evaluated as an ordered list -- first match wins.

**Rationale**: Mutually exclusive modes create an artificial constraint. Real-world features often need both exact matches (for known values) and predicates (for patterns or ranges). Unified dispatch with explicit ordering is simpler to understand and more flexible. The O(1) fast path for order-less features prevents performance regression.

**Impact**: No compile error for mixing. Users get more flexibility. Existing value-based-only features are unaffected. See updated ADR-008.

### WD-11: Drop "Is" prefix from predicate class names

**Decision**: Predicate class naming convention drops the "Is" prefix. Example: `Enterprise` instead of `IsEnterprise`, `Startup` instead of `IsStartup`.

**Rationale**: Predicates test flag values, not EvaluationContext attributes. `@Condition(matches = Enterprise.class)` reads naturally. The "Is" prefix was a remnant of the EvaluationContext-based design where `IsEnterprise` tested whether the context "is enterprise." With flag-value predicates, the name should describe what the predicate matches, not what the subject "is."

**Impact**: Documentation and examples updated. No enforcement in code -- naming is a convention.

### WD-12: REQUIRED strategy semantics for conditions

**Decision**: `FallbackStrategy.REQUIRED` on features with condition-based variants requires `@DefaultVariant` at compile time.

**Rationale**: For value-based features with enum, `REQUIRED` means "all enum values must have implementations." For conditions, predicate completeness cannot be verified at compile time (predicates are runtime-evaluated). The safest interpretation of `REQUIRED` is "there must be a fallback" -- hence `@DefaultVariant` is mandatory.

**Impact**: Developers using `REQUIRED` (which is not the default -- `EXCEPTION` is) must provide `@DefaultVariant`. This is consistent with the principle that `REQUIRED` = compile-time guarantee of completeness.

### WD-13: Exception propagation from predicates

**Decision**: FlagZen does not catch, wrap, or log exceptions thrown by predicate `test()` methods. They propagate directly to the caller.

**Rationale**: Predicates are user code with user-defined semantics. Catching exceptions would hide bugs. Wrapping exceptions would change the stack trace. The caller is in the best position to handle predicate failures. This is consistent with how value-based dispatch propagates `FlagProvider` exceptions.

**Impact**: If a predicate throws, the caller sees the original exception. No FlagZen wrapper.

## ADR References

- **ADR-008**: Unified Ordered Dispatch -- replaces the earlier "Mutually Exclusive Dispatch Modes" decision. Exact matches and conditions coexist, ordered by `@Variant(order = N)`.
- **ADR-009**: Predicate Instantiation Strategy -- no-arg constructor instantiation at proxy construction time.
- **ADR-010**: @Condition Annotation Nesting in @Variant -- `@Condition` is nested inside `@Variant(when = ...)`.
