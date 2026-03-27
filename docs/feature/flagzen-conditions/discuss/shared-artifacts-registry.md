# Shared Artifacts Registry: Condition Predicates (flagzen-conditions)

## Shared Artifacts

### JDK Predicate Interfaces

```yaml
source_of_truth: "java.util.function.Predicate<String>, IntPredicate, LongPredicate, DoublePredicate (JDK)"
consumers:
  - "Predicate implementation classes (user code)"
  - "Annotation processor validation (FlagZenProcessor)"
  - "Generated proxy dispatch logic (ProxyGenerator)"
  - "@Condition annotation's matches()/notMatches() attribute (type reference)"
owner: "JDK (standard library)"
integration_risk: "LOW -- stable JDK interfaces, no contract to maintain"
validation: "Predicate classes implement one of the supported JDK predicate interfaces"
```

### @Condition Annotation

```yaml
source_of_truth: "com.flagzen.Condition (flagzen-core)"
consumers:
  - "@Variant(when = @Condition(...)) usage in user code"
  - "Annotation processor validation"
  - "Proxy generator code generation"
owner: "flagzen-core"
integration_risk: "MEDIUM -- annotation attributes (matches, notMatches) must match processor expectations"
validation: "matches() or notMatches() returns a class implementing a JDK predicate interface; matches and notMatches are mutually exclusive"
```

### @Variant order Attribute

```yaml
source_of_truth: "com.flagzen.Variant (flagzen-core)"
consumers:
  - "@Variant(order = int) on user variant classes"
  - "Annotation processor duplicate detection"
  - "Proxy generator dispatch sequence"
owner: "flagzen-core"
integration_risk: "MEDIUM -- order attribute must be on @Variant, not on @Condition"
validation: "order values are unique within the same @Feature; optional when unambiguous"
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
validation: "Proxy implements @Feature interface, has zero reflection imports, supports unified dispatch (exact matches + conditions)"
```

### Unified Dispatch Order

```yaml
source_of_truth: "Generated proxy (ProxyGenerator)"
consumers:
  - "Exact match dispatch"
  - "Condition-based predicate evaluation"
owner: "flagzen-core annotation processor"
integration_risk: "MEDIUM -- dispatch order must be deterministic"
validation: "Exact matches checked first, then conditions by @Variant order"
```

## Integration Checkpoints

### Checkpoint 1: JDK Predicate Interface Compatibility

Predicates must implement one of the supported JDK interfaces: `Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`. The annotation processor validates this at compile time.

### Checkpoint 2: Annotation Processor Extension

The existing FlagZenProcessor must be extended (not replaced) to handle @Condition. Value-based validation and condition-based validation must coexist. The processor must support unified dispatch where exact matches and conditions coexist on the same @Feature.

### Checkpoint 3: Proxy Generator Extension

ProxyGenerator must emit unified dispatch logic supporting both exact matches and conditions on the same @Feature. Exact matches are checked first, then conditions by @Variant order. Both modes share the same FallbackStrategy and @DefaultVariant behavior.

### Checkpoint 4: FallbackStrategy Consistency

FallbackStrategy.REQUIRED for condition-based features: compilation fails if no @DefaultVariant is present (since conditions are runtime-evaluated, there is no "complete coverage" equivalent to enum coverage). EXCEPTION and NOOP behave identically to value-based dispatch.
