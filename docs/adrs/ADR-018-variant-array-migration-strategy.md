# ADR-018: Variant Annotation Array Migration Strategy

## Status

Proposed

## Context

The `@Variant` annotation currently uses scalar types for its value elements: `String value()`, `int intValue()`, and `long longValue()`. When multiple flag values should map to the same implementation class, developers must repeat the `@Variant` annotation (via `@Repeatable`) -- one annotation per value. This creates boilerplate, especially for features where 3+ values route to the same class.

The `doubleValue()` element already uses `CloseTo[]` (array type), so the multi-value pattern is partially established. The `booleanValue()` element is excluded from multi-value support because it has only two possible values.

Two migration strategies are available for transitioning scalar elements to arrays:

1. **In-place array migration**: change `String value()` to `String[]`, `int intValue()` to `int[]`, `long longValue()` to `long[]`
2. **New elements with deprecation**: add new `String[] values()`, `int[] intValues()`, `long[] longValues()` alongside existing scalars; deprecate the scalar versions

The project is pre-1.0, so binary compatibility is not a constraint.

### Sentinel Value Problem

The current int and long elements use sentinel values (`Integer.MIN_VALUE` and `Long.MIN_VALUE`) to mean "not set". These sentinel values cannot be used as actual variant values. Switching to array types with empty-array defaults (`int[] intValue() default {}`) eliminates the need for sentinels entirely.

## Decision

**In-place array migration**: change element types directly from scalar to array. Sentinels are replaced by empty-array defaults for int and long.

Specifically:

- `String value() default ""` becomes `String[] value() default ""`
- `int intValue() default Integer.MIN_VALUE` becomes `int[] intValue() default {}`
- `long longValue() default Long.MIN_VALUE` becomes `long[] longValue() default {}`

The "not set" detection in the processor changes from sentinel comparison to array length checks.

## Alternatives Considered

### Alternative A: New Elements with Deprecation

Add `String[] values()`, `int[] intValues()`, `long[] longValues()` alongside existing scalar elements. Deprecate the scalars.

**Evaluation:**

- (+) Binary compatible -- existing compiled code continues to work
- (+) Gradual migration path
- (-) Two ways to express the same thing -- processor must check both, increasing complexity
- (-) Interaction between scalar and array elements on the same annotation is ambiguous (which takes precedence?)
- (-) The deprecated scalar elements must still be maintained indefinitely or until a major version bump
- (-) Pre-1.0: binary compatibility is not a meaningful constraint

**Rejection rationale**: Increases processor complexity and annotation surface area for a compatibility guarantee that has no value pre-1.0.

### Alternative B: Keep Scalars, Rely Only on @Repeatable

Keep the annotation schema unchanged and rely entirely on `@Repeatable` for multi-value mapping (one annotation per value).

**Evaluation:**

- (+) Zero schema change, zero migration
- (-) Boilerplate remains the core complaint motivating this feature
- (-) Sentinel values (`Integer.MIN_VALUE`, `Long.MIN_VALUE`) remain reserved, limiting the value space
- (-) Does not solve the problem the feature was created for

**Rejection rationale**: Fails to address the primary requirement.

## Consequences

### Positive

- Single, clean API: one element per type, array syntax for multi-value, scalar auto-wraps for single-value
- Sentinel elimination: `Integer.MIN_VALUE` and `Long.MIN_VALUE` become valid variant values
- Processor simplification: "not set" checks become `length == 0` instead of magic-value comparisons
- Source compatible: existing `@Variant(value = "X")` and `@Variant(intValue = 42)` compile unchanged (Java auto-wraps scalars to single-element arrays)

### Negative

- Binary incompatible: compiled bytecode referencing the old annotation element types will fail at runtime with `AnnotationTypeMismatchException`. Pre-1.0, this is acceptable with changelog documentation.
- `String[] value() default ""` has a subtle interaction: the default produces `{""}`, which the processor must interpret as "not set" (matching the current empty-string sentinel behavior). This requires careful "not set" detection logic in the processor.
- `Map.of()` arity limit: Java's `Map.of()` supports up to 10 key-value pairs. If a feature has more than 10 total variant values (across all implementations and arrays), the generated metadata code must use `Map.ofEntries()` instead. This is an existing limitation amplified by multi-value arrays, not introduced by this decision.
