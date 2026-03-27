# Shared Artifacts Registry: Condition Predicates (flagzen-conditions)

## Shared Artifacts

### FeaturePredicate Interface

```yaml
source_of_truth: "com.flagzen.FeaturePredicate (flagzen-core)"
consumers:
  - "Predicate implementation classes (user code)"
  - "Annotation processor validation (FlagZenProcessor)"
  - "Generated proxy dispatch logic (ProxyGenerator)"
  - "@Condition annotation's on() attribute (type reference)"
owner: "flagzen-core"
integration_risk: "HIGH -- contract change breaks all predicates and generated proxies"
validation: "FeaturePredicate has single method: boolean test(EvaluationContext ctx)"
```

### EvaluationContext

```yaml
source_of_truth: "com.flagzen.EvaluationContext (flagzen-core, M1)"
consumers:
  - "FeaturePredicate.test() parameter"
  - "FlagContext.run() parameter"
  - "FeatureDispatcher.resolve() parameter"
  - "FlagProvider.getString(key, context) parameter"
  - "ContextAccessor.getContext() return type"
owner: "flagzen-core (M1 milestone)"
integration_risk: "HIGH -- M6 depends on M1 delivering this type unchanged"
validation: "Immutable record/class with targetingKey and attributes map"
```

### @Condition Annotation

```yaml
source_of_truth: "com.flagzen.Condition (flagzen-core)"
consumers:
  - "@Variant(when = @Condition(...)) usage in user code"
  - "Annotation processor validation"
  - "Proxy generator code generation"
owner: "flagzen-core"
integration_risk: "MEDIUM -- annotation attributes (on, order) must match processor expectations"
validation: "on() returns Class<? extends FeaturePredicate>, order() returns int"
```

### FallbackStrategy Enum

```yaml
source_of_truth: "com.flagzen.FallbackStrategy (flagzen-core, M0)"
consumers:
  - "@Feature(fallback = ...) attribute"
  - "Value-based proxy dispatch (M0)"
  - "Condition-based proxy dispatch (M6)"
  - "Generated proxy fallback logic"
owner: "flagzen-core (M0 milestone, unchanged)"
integration_risk: "LOW -- existing enum, no changes needed"
validation: "REQUIRED, EXCEPTION, NOOP behavior identical for both dispatch modes"
```

### Generated Proxy (per @Feature)

```yaml
source_of_truth: "Annotation processor output (ProxyGenerator)"
consumers:
  - "FeatureDispatcher runtime resolution"
  - "FeatureMetadata SPI discovery"
  - "Spring FeatureFactoryBean registration"
owner: "flagzen-core annotation processor"
integration_risk: "MEDIUM -- proxy shape changes affect all consumers"
validation: "Proxy implements @Feature interface, has zero reflection imports"
```

### Context Resolution Order

```yaml
source_of_truth: "com.flagzen.internal.DefaultFeatureDispatcher (M1: US-EC-07)"
consumers:
  - "Value-based dispatch (M0+M1)"
  - "Condition-based predicate evaluation (M6)"
owner: "flagzen-core (M1 milestone)"
integration_risk: "LOW -- resolution order is the same for both dispatch modes"
validation: "Order: explicit > accessor > scoped > default"
```

## Integration Checkpoints

### Checkpoint 1: M1 Dependency

EvaluationContext from M1 (US-EC-01) must be available before M6 development begins. FeaturePredicate.test() takes EvaluationContext as its parameter. If EvaluationContext API changes during M1 development, FeaturePredicate contract must be updated.

### Checkpoint 2: Annotation Processor Extension

The existing FlagZenProcessor must be extended (not replaced) to handle @Condition. Value-based validation and condition-based validation must coexist. The processor must reject features that mix both modes.

### Checkpoint 3: Proxy Generator Extension

ProxyGenerator must emit a second dispatch path for condition-based features. The generated proxy determines dispatch mode at construction time (value-based vs condition-based) based on the metadata. Both modes share the same FallbackStrategy and @DefaultVariant behavior.

### Checkpoint 4: FallbackStrategy Consistency

FallbackStrategy.REQUIRED for condition-based features: compilation fails if no @DefaultVariant is present (since conditions are runtime-evaluated, there is no "complete coverage" equivalent to enum coverage). EXCEPTION and NOOP behave identically to value-based dispatch.
