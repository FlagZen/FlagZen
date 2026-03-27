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
integration_risk: "HIGH -- enum values drive processor validation, proxy generation, and FlagProvider method selection. Mismatch breaks the entire typed dispatch pipeline."
validation: "FeatureType enum constants (STRING, INT, BOOLEAN) must be consistent across annotation definition, processor validation rules, and proxy generator code paths"
```

### variant_typed_attributes

```yaml
source_of_truth: "@Variant annotation definition (intValue, booleanValue attributes)"
consumers:
  - "FlagZenProcessor type consistency validation"
  - "ProxyGenerator variant map construction"
  - "FeatureModel/VariantModel compile-time models"
owner: "flagzen-core"
integration_risk: "HIGH -- new annotation attributes must be read by processor and used by code generator. Missing attribute handling causes silent dispatch failures."
validation: "Each @Variant for a typed feature uses exactly one typed attribute matching the @Feature type. Processor rejects mixed usage."
```

### flag_provider_typed_methods

```yaml
source_of_truth: "com.flagzen.spi.FlagProvider interface"
consumers:
  - "Generated proxies for INT features (call getInt)"
  - "Generated proxies for BOOLEAN features (call getBoolean)"
  - "Application code for conditional API (non-polymorphic)"
  - "Provider adapter implementations (flagzen-env, flagzen-openfeature, etc.)"
  - "TestFlagContext and InMemoryFlagProvider"
owner: "flagzen-core"
integration_risk: "HIGH -- FlagProvider is an SPI. Adding default methods is backward compatible, but provider adapters that override getString() must also consider getInt/getBoolean behavior. Default implementation delegates to getString + parse."
validation: "All typed methods are default methods parsing from getString(). Providers that natively support typed values can override for efficiency. Parse failure returns Optional.empty(), not exception."
```

### compile_time_models_extension

```yaml
source_of_truth: "FeatureModel and VariantModel in com.flagzen.processor"
consumers:
  - "FlagZenProcessor validation"
  - "ProxyGenerator"
owner: "flagzen-core (processor package)"
integration_risk: "MEDIUM -- internal models, but changes affect both validation and generation. FeatureModel needs featureType field. VariantModel needs typed value fields."
validation: "FeatureModel.featureType and VariantModel.intValue/booleanValue are populated during processing and consumed during generation."
```

### proxy_dispatch_type

```yaml
source_of_truth: "ProxyGenerator dispatch template selection"
consumers:
  - "Generated proxy classes"
  - "Runtime dispatch behavior"
owner: "flagzen-core (processor package)"
integration_risk: "MEDIUM -- proxy must call correct FlagProvider method (getString vs getInt vs getBoolean) based on FeatureType. Wrong method selection causes ClassCastException or incorrect dispatch."
validation: "Generated proxy for STRING features calls getString(), for INT features calls getInt(), for BOOLEAN features calls getBoolean(). Variant map key type matches."
```

## Integration Checkpoints

1. **Annotation Model Consistency**: @Feature.type() values match FeatureType enum constants. @Variant attribute names (value, intValue, booleanValue) are mutually exclusive per annotation instance.

2. **Processor-Generator Handoff**: FeatureModel carries featureType to ProxyGenerator. VariantModel carries typed values. Generator selects dispatch template based on featureType.

3. **Proxy-Provider Contract**: Generated proxy for INT feature calls FlagProvider.getInt(). Generated proxy for BOOLEAN feature calls FlagProvider.getBoolean(). FlagProvider default methods parse from getString() as fallback.

4. **Backward Compatibility Gate**: Existing @Feature annotations without type attribute default to STRING. Existing @Variant annotations with value attribute continue to work. FlagProvider.getString() remains the primitive -- new methods are additive default methods.
