# Wave Decisions: flagzen-multi-value-variant (M13)

## Decision 1: Feature Type

**Backend** -- annotation processing, code generation, compile-time validation. No UI, no CLI.

## Decision 2: Walking Skeleton

**Yes** -- thinnest slice through annotation change + processor modification + proxy generation. String multi-value first, then typed multi-value.

## Decision 3: UX Research Depth

**Lightweight** -- well-understood compiler extension pattern. The "users" are Java developers writing annotations. Mental model is clear: array syntax in annotations is standard Java.

## Decision 4: JTBD

**No** -- motivations clear. When multiple flag values need the same variant implementation, developers currently duplicate `@Variant` annotations via `@Repeatable`. Array syntax reduces boilerplate.

## Decision 5: Discovery Depth

**Minimal** -- pre-filled context from project owner. No ambiguity in scope. Six checklist items in progress.md map directly to stories.

## Key Technical Context

- Current `@Variant` has scalar elements: `String value()`, `int intValue()`, `long longValue()`
- Changing to array types (`String[] value()`, `int[] intValue()`, `long[] longValue()`) is source-compatible (single value auto-wraps) but NOT binary compatible
- Pre-1.0, so binary incompatibility is acceptable with documentation
- `booleanValue` multi-value skipped (only two possible values: true/false)
- `doubleValue` already uses `CloseTo[]` array -- multi-value already works for double
- `@Repeatable` composability with array values must be tested

## Decision 7: @CloseTo Overlapping Range Detection (US-07 addition)

**Added post-initial discovery.** Two scopes of overlap detection:

1. **Inter-variant overlap** (M2 concern): two different variant classes have `@CloseTo` ranges that overlap for the same DOUBLE-typed feature. This applies even without multi-value arrays and arguably belongs in `flagzen-typed-variants` (M2). However, it was not caught there, so it is tracked here.
2. **Intra-variant overlap** (M13 concern): a single variant class has overlapping `@CloseTo` entries within its `doubleValue` array. This is M13-specific since M13 introduces multi-value `@CloseTo[]` arrays.

Both checks use the formula `|value1 - value2| < delta1 + delta2` to determine overlap. Error messages name the overlapping variants, show computed ranges, and suggest remediation (reduce delta or merge variants).

**Scope note**: Inter-variant overlap detection could be backported to M2 as a separate story if the team prefers. For now, both checks are tracked under M13 to avoid blocking delivery.
