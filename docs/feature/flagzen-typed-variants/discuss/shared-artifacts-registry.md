# Shared Artifacts Registry: flagzen-typed-variants

## Artifacts

### feature_type_enum

```yaml
source_of_truth: "com.flagzen.FeatureType"
consumers:
  - "@Feature annotation type attribute"
  - "FlagZenProcessor validation logic"
  - "ProxyGenerator dispatch mode selection"
  - "Documentation and Javadoc"
owner: "flagzen-core"
integration_risk: "HIGH -- enum values drive processor validation, proxy generation, and FlagProvider method selection. Adding LONG/DOUBLE extends existing STRING/INT/BOOLEAN. Mismatch breaks the entire typed dispatch pipeline."
validation: "FeatureType enum constants (STRING, INT, LONG, BOOLEAN, DOUBLE) must be consistent across annotation definition, processor validation rules, and proxy generator code paths"
```

### variant_typed_attributes

```yaml
source_of_truth: "@Variant annotation definition (intValue, booleanValue, longValue, doubleValue attributes)"
consumers:
  - "FlagZenProcessor type consistency validation"
  - "ProxyGenerator variant map construction"
  - "FeatureModel/VariantModel compile-time models"
owner: "flagzen-core"
integration_risk: "HIGH -- new annotation attributes must be read by processor and used by code generator. Missing attribute handling causes silent dispatch failures."
validation: "Each @Variant for a typed feature uses exactly one typed attribute matching the @Feature type. Processor rejects mixed usage."
```

### when_true_false_annotations

```yaml
source_of_truth: "@WhenTrue and @WhenFalse annotation definitions in com.flagzen"
consumers:
  - "FlagZenProcessor (treats as @Variant(booleanValue = true/false))"
  - "Multi-feature classes via of= attribute"
  - "Documentation showing convenience syntax"
owner: "flagzen-core"
integration_risk: "MEDIUM -- syntactic sugar must be exactly equivalent to @Variant(booleanValue=...). Processor must normalize before validation and code generation."
validation: "@WhenTrue produces identical VariantModel as @Variant(booleanValue = true). @WhenFalse produces identical VariantModel as @Variant(booleanValue = false). of= attribute handled identically to @Variant(of=...)."
```

### close_to_annotation

```yaml
source_of_truth: "@CloseTo annotation definition in com.flagzen"
consumers:
  - "@Variant(doubleValue = @CloseTo(...)) attribute"
  - "FlagZenProcessor validation (DOUBLE features only)"
  - "ProxyGenerator approximate matching code (Math.abs comparison)"
owner: "flagzen-core"
integration_risk: "MEDIUM -- delta value flows from annotation through processor to generated proxy. Default delta 1e-10 must be consistent. Floating-point semantics require careful handling."
validation: "@CloseTo.value() is the target double value. @CloseTo.delta() defaults to 1e-10. Proxy iterates variants using Math.abs(flagValue - variantValue) <= delta."
```

### flag_provider_typed_methods

```yaml
source_of_truth: "com.flagzen.spi.FlagProvider interface"
consumers:
  - "Generated proxies for INT features (call getInt)"
  - "Generated proxies for LONG features (call getLong)"
  - "Generated proxies for BOOLEAN features (call getBoolean)"
  - "Generated proxies for DOUBLE features (call getDouble)"
  - "Application code for conditional API (non-polymorphic)"
  - "Provider adapter implementations (flagzen-env, flagzen-openfeature, etc.)"
  - "TestFlagContext and InMemoryFlagProvider"
owner: "flagzen-core"
integration_risk: "HIGH -- FlagProvider is an SPI. Adding default methods is backward compatible, but provider adapters may override for native typed support. Return types use JDK primitive optionals: OptionalInt, OptionalLong, OptionalDouble, Optional<Boolean>."
validation: "All typed methods are default methods parsing from getString(). Parse failure returns empty optional, not exception. Providers with native typed support override for efficiency."
```

### compile_time_models_extension

```yaml
source_of_truth: "FeatureModel and VariantModel in com.flagzen.processor"
consumers:
  - "FlagZenProcessor validation"
  - "ProxyGenerator"
owner: "flagzen-core (processor package)"
integration_risk: "MEDIUM -- internal models, but changes affect both validation and generation. FeatureModel needs featureType field. VariantModel needs typed value fields (int, boolean, long, double + delta)."
validation: "FeatureModel.featureType populated during processing. VariantModel.intValue/booleanValue/longValue/doubleValue/delta populated during processing and consumed during proxy generation."
```

### proxy_dispatch_strategy

```yaml
source_of_truth: "ProxyGenerator dispatch template selection in com.flagzen.processor"
consumers:
  - "Generated proxy classes"
  - "Runtime dispatch behavior"
owner: "flagzen-core (processor package)"
integration_risk: "HIGH -- proxy must call correct FlagProvider method and use correct dispatch strategy. INT/LONG/BOOLEAN: map lookup. DOUBLE: iterate with approximate matching. Wrong method selection causes incorrect dispatch."
validation: |
  STRING: proxy calls getString(), Map<String, Supplier<T>>
  INT: proxy calls getInt(), Map<Integer, Supplier<T>>
  LONG: proxy calls getLong(), Map<Long, Supplier<T>>
  BOOLEAN: proxy calls getBoolean(), Map<Boolean, Supplier<T>>
  DOUBLE: proxy calls getDouble(), iterates variants with Math.abs(flagValue - variantValue) <= delta
```

## Integration Checkpoints

1. **Annotation Model Consistency**: `@Feature.type()` values match `FeatureType` enum constants (STRING, INT, LONG, BOOLEAN, DOUBLE). `@Variant` attribute names (`value`, `intValue`, `booleanValue`, `longValue`, `doubleValue`) are mutually exclusive per annotation instance. `@WhenTrue`/`@WhenFalse` are normalized to `@Variant(booleanValue=...)` before further processing.

2. **Processor-Generator Handoff**: FeatureModel carries `featureType` to ProxyGenerator. VariantModel carries typed values (including `@CloseTo` value and delta for DOUBLE). Generator selects dispatch template based on `featureType`.

3. **Proxy-Provider Contract**: Generated proxy for INT feature calls `FlagProvider.getInt()` returning `OptionalInt`. LONG calls `getLong()` returning `OptionalLong`. BOOLEAN calls `getBoolean()` returning `Optional<Boolean>`. DOUBLE calls `getDouble()` returning `OptionalDouble`. All typed methods have context-aware overloads accepting `EvaluationContext`.

4. **Backward Compatibility Gate**: Existing `@Feature` annotations without type attribute default to STRING. Existing `@Variant` annotations with `value` attribute continue to work. `FlagProvider.getString()` remains the primitive -- new methods are additive default methods. No existing tests break.

5. **Context Integration**: Typed proxy dispatch uses same context resolution chain as string dispatch (explicit > accessor > scoped > default). Typed `FlagProvider` methods all have context-aware overloads following the same pattern as `getString(key, context)`.
