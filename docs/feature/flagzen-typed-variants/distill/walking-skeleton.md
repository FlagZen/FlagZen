# Walking Skeleton -- flagzen-typed-variants

## Skeleton Identification

Three walking skeletons cover the thinnest vertical slices through typed annotation, typed dispatch, and boolean convenience dispatch.

### Skeleton 1: Integer Feature Declaration and Proxy Generation

**Stories**: US-M2-01, US-M2-02

**User goal**: Developer declares an integer-typed feature with typed variant values and the project compiles with a generated proxy.

**Slice**: Annotation model (FeatureType enum + `@Feature.type` + `@Variant.intValue`) through annotation processor through code generator.

**Stakeholder demo**: "I can annotate a feature as INT and give each variant an integer value, and it compiles."

### Skeleton 2: Integer Runtime Dispatch

**Stories**: US-M2-05

**User goal**: Developer resolves an integer-typed feature at runtime and the correct variant handles the call.

**Slice**: `FlagProvider.getInt()` through generated proxy (integer map lookup) through `FeatureDispatcher`.

**Stakeholder demo**: "When the flag returns 3, the conservative retry strategy handles my call."

### Skeleton 3: Boolean Dispatch with Convenience Annotations

**Stories**: US-M2-03, US-M2-06

**User goal**: Developer uses `@WhenTrue`/`@WhenFalse` to define boolean variants and the proxy dispatches on boolean flag values.

**Slice**: Convenience annotations through processor normalization through boolean proxy dispatch.

**Stakeholder demo**: "I annotate one variant as active-when-true and another as active-when-false, and the proxy picks the right one."

## Implementation Sequence

1. Enable Skeleton 1 (compile-time). Pass criteria: project with `@Feature(type = INT)` and `@Variant(intValue = N)` compiles and generates proxy.
2. Enable Skeleton 2 (runtime). Pass criteria: `FeatureDispatcher` resolves INT feature, proxy calls `getInt()` and dispatches to correct variant.
3. Enable Skeleton 3 (boolean convenience). Pass criteria: `@WhenTrue`/`@WhenFalse` compile, proxy dispatches on boolean flag value.

## Litmus Test

| Check | Skeleton 1 | Skeleton 2 | Skeleton 3 |
| ----- | ---------- | ---------- | ---------- |
| Title describes user goal? | Yes: "declares integer-typed feature" | Yes: "resolves to matching variant" | Yes: "dispatches boolean feature" |
| Then describes user observation? | Yes: "compilation succeeds, proxy generated" | Yes: "variant handles method call" | Yes: "variant handles method call" |
| Non-technical stakeholder confirms? | Yes: type-safe annotation compiles | Yes: right variant selected | Yes: boolean convenience works |
