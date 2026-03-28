# Component Boundaries -- flagzen-multi-value-variant (M13)

## Modified Components

All modifications are within `flagzen-core`. No other module is affected.

### 1. `com.flagzen.Variant` (annotation)

**Responsibility**: Declares variant value bindings for a feature implementation class.

**Modification**: Three element types change from scalar to array.

| Element | Before | After | Default |
| --- | --- | --- | --- |
| `value()` | `String` | `String[]` | `""` (auto-wraps to `{""}`) |
| `intValue()` | `int` | `int[]` | `{}` |
| `longValue()` | `long` | `long[]` | `{}` |
| `doubleValue()` | `CloseTo[]` | `CloseTo[]` | `{}` (no change) |
| `booleanValue()` | `String` | `String` | `""` (no change) |
| `of()` | `Class<?>` | `Class<?>` | `void.class` (no change) |

### 2. `com.flagzen.processor.FlagZenProcessor` (annotation processor)

**Responsibility**: Validates `@Feature`/`@Variant` annotations, orchestrates code generation.

**Modifications by method:**

| Method | Change Type | Description |
| --- | --- | --- |
| `processVariantAnnotation()` | Modified | Iterate array elements; create one `VariantModel` per element instead of one per annotation |
| `hasTypeMismatch()` | Modified | Replace sentinel checks (`!= MIN_VALUE`) with array length checks (`length > 0`) |
| New validation method | Added | `@CloseTo` overlap detection (intra-variant and inter-variant) |
| New validation method | Added | Empty string detection within `value()` arrays |
| `collectVariants()` | Unchanged | Calls `processVariantAnnotation()` per annotation; array expansion happens inside that call |
| `hasDuplicateVariantValues()` | Unchanged | Operates on flat `List<VariantModel>`; array expansion is upstream |
| `validateVariantValuesAgainstEnum()` | Unchanged | Operates on flat `List<VariantModel>` |
| `hasIncompleteVariantCoverage()` | Unchanged | Operates on flat `List<VariantModel>` |

### 3. `com.flagzen.processor.VariantModel` (record)

**Responsibility**: Compile-time model of a single variant value binding.

**Modification**: None. The record already represents a single value. Array expansion produces multiple `VariantModel` instances with the same `qualifiedClassName` but different values. Factory methods (`ofLong`, `ofDouble`, `ofBoolean`) and `variantKeyLiteral()` are unchanged.

### 4. `com.flagzen.processor.ProxyGenerator` (code generator)

**Responsibility**: Generates `{Feature}_FlagZenProxy` and `{Feature}_FlagZenMetadata` classes.

**Modification**: None. Consumes `FeatureModel.variants()` which is `List<VariantModel>`. Multiple entries with the same class produce multiple map entries pointing to the same `Supplier`. The `variantSuppliers()` metadata method iterates the list and registers each key -- naturally handles multi-value because the list now contains more entries.

### 5. `com.flagzen.processor.FeatureModel` (record)

**Responsibility**: Compile-time model of a `@Feature`-annotated interface.

**Modification**: None. Holds `List<VariantModel> variants` -- the list is longer after array expansion, but the type and contract are unchanged.

## Unchanged Components

| Component | Why unchanged |
| --- | --- |
| `com.flagzen.CloseTo` | Annotation schema unchanged |
| `com.flagzen.Feature` | Not affected by variant value changes |
| `com.flagzen.DefaultVariant` | Not affected |
| `com.flagzen.FallbackStrategy` | Not affected |
| `com.flagzen.FeatureType` | Not affected |
| `com.flagzen.FeatureDispatcher` | Runtime interface; unchanged |
| `com.flagzen.internal.DefaultFeatureDispatcher` | Runtime impl; unchanged |
| `com.flagzen.spi.FlagProvider` | SPI; unchanged |
| `com.flagzen.spi.FeatureMetadata` | SPI; unchanged |
| All other modules | No dependency on `@Variant` element types at the module level |

## Dependency Flow (unchanged)

```
@Variant annotation
    |
    v
FlagZenProcessor.processVariantAnnotation()  <-- array expansion here
    |
    v
List<VariantModel>  (flat list, one entry per value)
    |
    +---> hasDuplicateVariantValues()         (unchanged)
    +---> validateVariantValuesAgainstEnum()  (unchanged)
    +---> hasIncompleteVariantCoverage()      (unchanged)
    +---> @CloseTo overlap detection          (NEW)
    |
    v
FeatureModel.variants
    |
    v
ProxyGenerator  (unchanged -- iterates list, generates map entries)
```

The key architectural insight: array expansion at the `processVariantAnnotation()` boundary means all downstream components are decoupled from the scalar-to-array change. This is the single point of transformation.
