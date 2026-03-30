# ADR-021: LaunchDarkly Absence Detection and Long Type via JSON Value

## Status

Proposed

## Context

`LaunchDarklyFlagProvider` implements `FlagProvider`, whose contract returns optionals -- empty means "flag not set or unavailable." The LaunchDarkly Java Server SDK v7 provides typed `*VariationDetail` methods that return `EvaluationDetail<T>` with an `EvaluationReason` object.

Two design decisions are needed:

1. **Absence detection**: How to determine when a LaunchDarkly evaluation represents "flag absent" vs "flag resolved."
2. **Long type**: LaunchDarkly has no `longVariationDetail` method. How to support `FlagProvider.getLong()`.

### Quality Attributes at Stake

- **Correctness**: False positives (returning a value when the flag is absent) break FlagZen's fallback logic. False negatives (returning empty when the flag exists) break dispatch.
- **Completeness**: `FlagProvider` defines `getLong` and `getLong(key, ctx)`. The adapter should support the full long range if possible.
- **Consistency**: The absence detection strategy should be principled and explainable, consistent with the approach in ADR-020 (reason-based, not sentinel-based).

## Decision

### Absence Detection

Use **reason kind-based detection** via `EvaluationDetail.getReason().getKind()`.

An evaluation is **absent** when:

1. `reason.getKind() == EvaluationReason.Kind.ERROR` -- evaluation failed (flag not found, malformed, wrong type, client not ready, etc.)
2. `reason.getKind() == EvaluationReason.Kind.PREREQUISITE_FAILED` -- evaluation could not complete because a prerequisite flag was not met

All other reason kinds (`OFF`, `FALLTHROUGH`, `TARGET_MATCH`, `RULE_MATCH`) indicate a real resolution, and the adapter returns the resolved value.

**Key difference from OpenFeature (ADR-020)**: LaunchDarkly uses a typed `EvaluationReason.Kind` enum, not a `String` reason field. The `OFF` kind returns a deliberately configured off-variation value (not the client default), so it is treated as a real resolution.

### Long Type via JSON Value

Use `LDClient.jsonValueVariationDetail(key, LDValue.ofNull(), context)` for `getLong`:

1. Call `jsonValueVariationDetail` with `LDValue.ofNull()` as default
2. Apply the same absence detection (reason kind check)
3. If present, check `LDValue.isNumber()` -- if false, return empty
4. Extract via `LDValue.longValue()` which returns the full long range

## Alternatives Considered

### Alternative A (Absence): Variation Index Check

Check `EvaluationDetail.getVariationIndex()` -- if `-1` or `null`, the flag was not resolved.

**Evaluation:**

- (+) Simple numeric check
- (-) `variationIndex` can be `-1` in some edge cases where a value was still returned (e.g., certain error recovery paths)
- (-) Less semantically clear than reason-based detection
- (-) Not documented as the primary absence signal in LaunchDarkly SDK docs

**Rejected because**: reason kind is the documented and semantic way to determine evaluation outcome. Variation index is a secondary signal.

### Alternative B (Absence): Treat `OFF` as Absent

When a flag is OFF, return `empty` (treating it like OpenFeature's `DEFAULT` reason).

**Evaluation:**

- (+) Simpler mental model: "flag off = flag absent"
- (-) **Discards intentional value**: LaunchDarkly flags have a configured off-variation. When a flag is OFF, it deliberately returns a specific value chosen by the flag administrator. Treating it as absent throws away this intentional configuration.
- (-) Inconsistent with LaunchDarkly's own semantics: the SDK returns a value and a valid variation index for OFF flags

**Rejected because**: OFF is a real resolution with a deliberately configured value. Treating it as absent would be a correctness bug for users who rely on off-variation values.

### Alternative C (Long): Use `intVariationDetail` + Widening

Same approach as the OpenFeature adapter (ADR-020 design): call `intVariationDetail(key, 0)` and widen `int` to `long`.

**Evaluation:**

- (+) Consistent with OpenFeature adapter pattern
- (-) **Truncates values outside int range**: Any long value outside `Integer.MIN_VALUE` to `Integer.MAX_VALUE` cannot be represented
- (-) LaunchDarkly offers a better alternative via `jsonValueVariationDetail` that the OpenFeature SDK lacks

**Rejected because**: LaunchDarkly's `LDValue.longValue()` supports the full long range. Using the inferior approach when a better one is available would be a design failure.

### Alternative D (Long): Use `doubleVariationDetail` + Cast

Call `doubleVariationDetail` and cast to `long`.

**Evaluation:**

- (+) Native typed method, no JSON parsing
- (-) **Precision loss**: `double` has 53-bit mantissa, `long` has 63 bits. Values above 2^53 lose precision.
- (-) Semantic mismatch: requesting a double to get an integer value

**Rejected because**: precision loss for large long values is unacceptable. JSON value approach is lossless.

## Consequences

### Positive

- **Correct absence detection**: Uses LaunchDarkly's documented evaluation reason as the primary signal. OFF flags return their configured value.
- **Full long range support**: `jsonValueVariationDetail` + `LDValue.longValue()` handles the complete `Long.MIN_VALUE` to `Long.MAX_VALUE` range.
- **Consistent with ADR-020 principle**: Reason-based detection, not sentinel-based. The specific reason kinds differ (enum vs string) but the approach is the same.

### Negative

- **`PREREQUISITE_FAILED` treated as absent**: This is debatable. The prerequisite flag not being met could be considered a valid state (return default). We treat it as absent because the flag evaluation was incomplete, and FlagZen's fallback strategy is the appropriate handler for incomplete evaluations.
- **JSON value for long is indirect**: The `jsonValueVariationDetail` call returns an `LDValue` that must be checked for type (`isNumber()`). Non-numeric JSON values result in `empty`, which is correct but adds a code path.
- **JSON value performance**: `jsonValueVariationDetail` may involve more processing than typed methods. In practice, LaunchDarkly's SDK handles this efficiently, and the difference is negligible compared to network/cache overhead.
