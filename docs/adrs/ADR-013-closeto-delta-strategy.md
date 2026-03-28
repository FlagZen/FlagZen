# ADR-013: @CloseTo Delta Strategy for Approximate Double Matching

## Status

Accepted

## Context

FlagZen M2 introduces `FeatureType.DOUBLE` for polymorphic dispatch on double-valued feature flags. Floating-point arithmetic is inherently imprecise -- flag backends (especially JavaScript-based) may return values where `0.1 + 0.2 != 0.3`. The dispatch mechanism must tolerate this imprecision while remaining deterministic and debuggable.

The design requires a `@CloseTo` annotation on `@Variant(doubleValue = @CloseTo(...))` to express approximate matching. The critical question is how to specify and default the tolerance (delta).

## Decision

`@CloseTo` has two attributes: `value()` (the target double) and `delta()` (the tolerance), with `delta()` defaulting to `1e-10`.

Matching logic: `Math.abs(flagValue - variantValue) <= delta`.

Each variant specifies its own delta, allowing per-variant tolerance. The default `1e-10` covers standard IEEE 754 double rounding errors without being so large that it causes false matches between distinct values.

DOUBLE proxy dispatch iterates variants sequentially (not map lookup) because approximate matching cannot use hash-based lookup. First match wins. When `order` is present (ADR-008), variants are iterated in order. When no `order` is present and only one variant exists, the single iteration is trivially correct. When multiple DOUBLE variants exist without `order`, the processor requires `order` to eliminate ambiguity (consistent with ADR-008's rule for multiple conditions).

### Delta Validation

The annotation processor validates at compile time:

- `delta` must be positive (> 0). Zero delta is rejected because it is semantically identical to exact match but pays the iteration cost.
- `delta` must be finite (not `Double.POSITIVE_INFINITY`, not `Double.NaN`).
- No overlap validation: the processor does NOT check whether two variants' delta ranges overlap. Overlap is resolved by ordering (first match wins). This avoids halting-problem-adjacent complexity in compile-time analysis and keeps the model simple.

## Alternatives Considered

### 1. Global delta on @Feature

A single delta for the entire feature: `@Feature(value = "ratio", type = DOUBLE, delta = 1e-10)`.

- **Pro**: Simpler -- one delta for all variants
- **Pro**: Less annotation verbosity
- **Con**: Cannot express different tolerances per variant (tight tolerance for 0.0/1.0, loose for 0.33333)
- **Con**: Mixes feature metadata with dispatch parameters

**Rejected**: Per-variant delta is strictly more expressive. The common case (default delta) requires no extra annotation text. The uncommon case (explicit delta) is only possible with per-variant specification.

### 2. Exact double matching (no delta)

Use `==` comparison for doubles, requiring flag providers to return exact values.

- **Pro**: Simplest possible implementation
- **Pro**: Deterministic -- no ambiguity from overlapping deltas
- **Con**: Fails for IEEE 754 rounding errors from JS backends, JSON parsing, or arithmetic
- **Con**: Developers must understand floating-point internals to avoid silent dispatch failures
- **Con**: Violates the principle of least surprise for a developer writing `@Variant(doubleValue = 0.3)`

**Rejected**: Floating-point imprecision is the primary motivation for `@CloseTo`. Exact matching would make DOUBLE dispatch unreliable in production.

### 3. Relative epsilon instead of absolute delta

Use relative tolerance: `Math.abs(flagValue - variantValue) / Math.max(Math.abs(flagValue), Math.abs(variantValue)) <= epsilon`.

- **Pro**: Scales with magnitude -- works for both small and large values
- **Con**: Breaks near zero (division by near-zero magnitude)
- **Con**: More complex to explain and debug
- **Con**: Flag values are typically small (0.0-1.0 for ratios, small integers for configs) -- absolute delta is sufficient

**Rejected**: Feature flag values are typically small-magnitude numbers where absolute delta works well. Relative epsilon adds complexity without benefit for the use case. If future use cases demand it, `@CloseTo` can be extended with a `relative` boolean attribute.

## Consequences

### Positive

- Default `1e-10` handles IEEE 754 rounding errors transparently
- Per-variant delta supports mixed-precision scenarios
- Compile-time delta validation prevents nonsensical values
- First-match-wins with ordering is consistent with the unified dispatch model (ADR-008)

### Negative

- DOUBLE dispatch is O(n) per call (vs O(1) for INT/LONG/STRING map lookup)
- No compile-time overlap detection -- ambiguous deltas resolved at runtime by order
- Developers using DOUBLE dispatch must understand that order matters when deltas overlap
