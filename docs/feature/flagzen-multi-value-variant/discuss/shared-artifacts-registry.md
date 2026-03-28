# Shared Artifacts Registry: flagzen-multi-value-variant

## Artifacts

### variant_annotation_schema

- **Source of truth**: `com.flagzen.Variant` annotation definition
- **Consumers**:
  - `FlagZenProcessor.processVariantAnnotation()` -- reads annotation element values
  - `FlagZenProcessor.hasTypeMismatch()` -- checks which elements are set
  - `FlagZenProcessor.collectVariants()` -- iterates through annotations
  - User code -- developers write annotations on implementation classes
- **Owner**: flagzen-core
- **Integration risk**: HIGH -- changing element types from scalar to array affects all consumers. Source-compatible but not binary-compatible.
- **Validation**: Existing test suite compiles and passes after annotation change.

### variant_model_list

- **Source of truth**: `FlagZenProcessor.collectVariants()` return value
- **Consumers**:
  - `FlagZenProcessor.hasDuplicateVariantValues()` -- groups by variantKeyLiteral
  - `FlagZenProcessor.hasIncompleteVariantCoverage()` -- collects covered values
  - `FlagZenProcessor.validateVariantValuesAgainstEnum()` -- checks each value
  - `ProxyGenerator.generateProxy()` -- builds variant map entries
  - `ProxyGenerator.generateMetadata()` -- lists registered values
- **Owner**: flagzen-core (processor)
- **Integration risk**: MEDIUM -- multi-value arrays produce more VariantModel instances from the same annotation. All downstream consumers must handle N models per annotation (currently assume 1 per annotation).
- **Validation**: VariantModel list contains one entry per array element. Downstream grouping and iteration logic works unchanged.

### sentinel_values

- **Source of truth**: `@Variant` annotation element defaults
- **Consumers**:
  - `FlagZenProcessor.hasTypeMismatch()` -- checks if element is "set" vs default
  - `FlagZenProcessor.processVariantAnnotation()` -- skips unset elements
- **Owner**: flagzen-core
- **Integration risk**: HIGH -- sentinel semantics change from scalar defaults (`Integer.MIN_VALUE`, `Long.MIN_VALUE`) to empty arrays (`{}`). Every "is this element set?" check must be updated.
- **Validation**: Type mismatch detection tests pass with array element types.

### duplicate_detection_scope

- **Source of truth**: `FlagZenProcessor.hasDuplicateVariantValues()`
- **Consumers**:
  - Compile error reporting
- **Owner**: flagzen-core (processor)
- **Integration risk**: LOW -- existing groupBy logic works on flat VariantModel list. No change needed if array expansion is correct upstream.
- **Validation**: Negative compilation tests for all duplicate scopes (intra-array, inter-annotation, inter-class).

## Integration Checkpoints

1. **Annotation schema change**: After changing element types, verify all existing tests compile without source modification.
2. **Array expansion**: After implementing array iteration in `collectVariants()`, verify VariantModel list has correct cardinality.
3. **Sentinel migration**: After changing defaults from scalar sentinels to empty arrays, verify `hasTypeMismatch()` detects correct/incorrect element usage.
4. **Duplicate detection**: After array expansion, verify duplicates detected across all scopes.
5. **Proxy generation**: After generating proxies with multi-value variants, verify map contains all entries.
