# Wave Decisions: DESIGN -- flagzen-multi-value-variant (M13)

## Decision 1: No New Modules

All changes are within `flagzen-core`. The `@Variant` annotation, `FlagZenProcessor`, and related processor-internal types are the only modified components. No new Gradle submodules, no new dependencies, no new SPI contracts.

**Rationale**: This feature extends an existing annotation's expressiveness. It does not introduce new capabilities, integration points, or runtime components.

## Decision 2: Array Expansion at Processor Boundary

Array values are expanded into flat `List<VariantModel>` at the `processVariantAnnotation()` boundary. All downstream logic (duplicate detection, enum validation, coverage checks, code generation) operates on the flat list without modification.

**Rationale**: Single point of transformation minimizes blast radius. Adding a new type to arrays or changing expansion logic requires modification in exactly one place.

**Trade-off**: The `VariantModel` list grows proportionally to the total number of values across all arrays. For typical usage (2-5 values per array, < 10 variants per feature), this is negligible. If a feature had 100+ array values, the generated `Map.of(...)` call would exceed `Map.of()` arity limits -- but that scenario is unrealistic and can be addressed if it ever arises.

## Decision 3: In-Place Annotation Schema Migration (ADR-018)

Scalar annotation elements are changed to array types directly, rather than adding new parallel elements. Sentinel values (`Integer.MIN_VALUE`, `Long.MIN_VALUE`) are replaced by empty-array defaults.

**Rationale**: Pre-1.0, binary compatibility is not a constraint. In-place migration avoids dual-element complexity in the processor and keeps the annotation surface clean. See [ADR-018](../../../adrs/ADR-018-variant-array-migration-strategy.md) for full analysis.

## Decision 4: `String[] value() default ""` Sentinel Semantics

The `value()` element retains `default ""` rather than `default {}`. Java auto-wraps the scalar `""` to `{""}`, which the processor treats as "not set" -- matching current behavior exactly.

**Rationale**: Using `default {}` would require all existing `@Variant(intValue = 42, of = X.class)` annotations (which don't specify `value`) to suddenly fail because `{}` means "I set string values to nothing" vs `{""}` meaning "I didn't set string values". The `""` default preserves the semantic that "value element was not explicitly provided by the developer."

**Alternative considered**: `default {}` with updated processor logic. Rejected because it changes the semantics of the default for existing code that relies on `value() == ""` to mean "not set."

## Decision 5: `@CloseTo` Overlap Detection Scope

Both intra-variant and inter-variant `@CloseTo` overlap detection are implemented in M13.

**Background**: Inter-variant overlap (two different classes with overlapping ranges) arguably belongs in M2 (`flagzen-typed-variants`) since it applies even without multi-value arrays. However, it was not caught there. Rather than backporting, both checks are implemented together in M13.

**Rationale**: Implementing both together is simpler than splitting across milestones. The overlap formula is identical for both checks; only the error message and grouping differ.

## Decision 6: DOUBLE `processVariantAnnotation()` Bug Fix

The current processor reads only `closeToValues[0]` from the `doubleValue()` array, ignoring subsequent entries. M13 fixes this to iterate all entries, creating one `VariantModel` per `@CloseTo` element.

**Rationale**: This is a bug in the current implementation -- the annotation schema already supports `CloseTo[]` arrays, but the processor only reads the first element. M13 naturally fixes this as part of the array expansion work.

## Decision 7: No ProxyGenerator Changes

The `ProxyGenerator` is unchanged. It already consumes `List<VariantModel>` and generates one map entry per `VariantModel`. Multi-value arrays produce more entries in the list, which the generator handles naturally.

**Rationale**: The array expansion boundary in the processor decouples the annotation schema change from code generation. This validates the existing architecture's separation of concerns.

## Artifacts Produced

| Artifact | Path |
| --- | --- |
| Architecture design | `docs/feature/flagzen-multi-value-variant/design/architecture-design.md` |
| Component boundaries | `docs/feature/flagzen-multi-value-variant/design/component-boundaries.md` |
| Data models | `docs/feature/flagzen-multi-value-variant/design/data-models.md` |
| ADR-018 | `docs/adrs/ADR-018-variant-array-migration-strategy.md` |
| Wave decisions | `docs/feature/flagzen-multi-value-variant/design/wave-decisions.md` (this file) |
