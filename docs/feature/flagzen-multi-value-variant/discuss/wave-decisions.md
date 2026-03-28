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
