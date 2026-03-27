# Data Models -- Condition Predicates (flagzen-conditions)

## Overview

This document describes the new and modified data models for condition-based predicate dispatch. All models are additive -- no existing models are removed or have breaking changes.

## 1. New Public API Types

### FeaturePredicate

Functional interface in `com.flagzen`. The contract for all condition predicates.

| Aspect | Value |
|--------|-------|
| Package | `com.flagzen` |
| Kind | `@FunctionalInterface` interface |
| Type parameters | None (not generic) |
| Depends on | `EvaluationContext` (M1) |

| Method | Return Type | Parameters | Description |
|--------|------------|------------|-------------|
| `test` | `boolean` | `EvaluationContext ctx` | Evaluates the predicate against the given context |

Design decisions:
- Not `Predicate<T>` -- always takes `EvaluationContext`, keeping the contract simple and annotation processor validation straightforward
- `@FunctionalInterface` annotation enables lambda and method reference usage
- Implementations must be thread-safe if used in multi-threaded applications (documented contract)
- Implementations should be fast (sub-millisecond) -- FlagZen evaluates predicates on every method call

### @Condition

Nested annotation type used exclusively within `@Variant(when = ...)`.

| Aspect | Value |
|--------|-------|
| Package | `com.flagzen` |
| Kind | Annotation type |
| Retention | `CLASS` |
| Target | Implicitly scoped via `@Variant` (applies to `TYPE`) |
| Standalone usage | Not supported -- used only as `@Variant(when = @Condition(...))` |

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `on` | `Class<? extends FeaturePredicate>` | (required -- see sentinel note) | The predicate class to evaluate |
| `order` | `int` | `0` | Evaluation sequence (ascending). Must be unique within the @Feature. |

Sentinel handling: `@Condition` needs a default value for `on` to serve as the sentinel for `@Variant.when`. A package-private or nested sentinel class (e.g., `FeaturePredicate.None`) that implements `FeaturePredicate` can serve this purpose. The processor detects this sentinel to distinguish "no condition" from a real condition.

## 2. Modified Annotation Types

### @Variant (modified)

New `when` attribute added. Existing attributes unchanged.

| Attribute | Type | Default | Status | Description |
|-----------|------|---------|--------|-------------|
| `value` | `String` | `""` | Existing (default changed from required to `""`) | Variant value for value-based dispatch |
| `of` | `Class<?>` | `void.class` | Existing (unchanged) | Target @Feature interface |
| `when` | `@Condition` | sentinel `@Condition` | **New** | Condition for predicate-based dispatch |

Backward compatibility: The `value` attribute default changes from required to `""`. For value-based variants, `value` must be non-empty (processor validates). For condition-based variants, `value` is ignored. The processor determines dispatch mode from the `when` attribute.

## 3. Processor-Internal Models (compile-time only)

### ConditionModel (new)

Represents a processed `@Condition` annotation. Internal to the processor, not part of the public API.

| Field | Type | Description |
|-------|------|-------------|
| predicateClass | `TypeMirror` | Fully qualified type of the FeaturePredicate implementor |
| order | `int` | Evaluation sequence value |

### VariantModel (modified)

Extended with optional condition metadata.

| Field | Type | Status | Description |
|-------|------|--------|-------------|
| qualifiedClassName | `String` (qualified name) | Existing | Fully qualified name of the @Variant class |
| variantValue | `String` | Existing | Variant value from `@Variant("VALUE")` |
| condition | `ConditionModel` (nullable) | **New** | Condition metadata, or null for value-based variants |

A variant is condition-based if `condition != null`. A variant is value-based if `condition == null` and `variantValue` is non-empty.

### FeatureModel (modified)

Gains awareness of dispatch mode.

| Field | Type | Status | Description |
|-------|------|--------|-------------|
| packageName | `String` | Existing | Package of the @Feature interface |
| interfaceName | `String` | Existing | Simple class name of the @Feature interface |
| flagKey | `String` | Existing | Value from `@Feature("flag-key")` |
| fallbackStrategy | `FallbackStrategy` | Existing | REQUIRED, EXCEPTION, or NOOP |
| methods | `List<MethodModel>` | Existing | Methods declared on the interface |
| variants | `List<VariantModel>` | Existing | All variant models (value-based or condition-based) |
| defaultVariantClassName | `String` (nullable) | Existing | Qualified name of @DefaultVariant class |
| dispatchMode | `DispatchMode` | **New** | `VALUE_BASED` or `CONDITION_BASED` |

### DispatchMode (new, processor-internal)

Enum representing the dispatch mode of a feature.

| Constant | Description |
|----------|-------------|
| `VALUE_BASED` | Dispatch via FlagProvider string lookup (M0) |
| `CONDITION_BASED` | Dispatch via predicate evaluation (M6) |

Determined by the processor after collecting all variants: if any variant has a non-sentinel `@Condition`, the feature is `CONDITION_BASED`. If all variants have `value` attributes, the feature is `VALUE_BASED`. Mixed = compile error.

## 4. Generated Proxy Structure (condition-based)

For condition-based features, the generated proxy has a different internal structure than value-based proxies.

### Proxy Internal State (condition-based)

| Field | Type | Description |
|-------|------|-------------|
| predicates | `FeaturePredicate[]` (final) | Predicate instances, sorted by order (ascending) |
| conditionVariants | Feature interface array (final) | Variant instances corresponding to each predicate (same index) |
| defaultVariant | Feature interface instance (nullable, final) | @DefaultVariant instance, if any |
| fallbackStrategy | `FallbackStrategy` (final) | Configured strategy |

### Proxy Constructor (condition-based)

Parameters:
- Predicate-variant pairs (or parallel arrays), sorted by order
- Default variant instance (nullable)
- FallbackStrategy

No `FlagProvider` parameter -- condition-based proxies do not query flag providers.

### Proxy Dispatch Logic (condition-based, per method)

1. Obtain `EvaluationContext` from resolution chain
2. If context is null: delegate to default variant or apply fallback
3. For each predicate in order:
   - Call `predicate.test(ctx)`
   - If true: delegate method call to corresponding variant, return
4. If no predicate matched: delegate to default variant or apply fallback

## 5. Generated Metadata (condition-based)

The `{Feature}_FlagZenMetadata` class for condition-based features must provide condition-aware factory methods. The `FeatureMetadata` SPI may need extension or the metadata class may use a condition-specific approach to construct the proxy.

Design consideration: The existing `FeatureMetadata.createProxy()` signature takes `FlagProvider` and a variant map -- neither is needed for condition-based dispatch. Options:

1. **Overloaded createProxy**: Add a condition-specific factory method to `FeatureMetadata`
2. **Unified factory**: Pass condition metadata through the existing map parameter with a convention
3. **Separate SPI**: A `ConditionFeatureMetadata` interface extending `FeatureMetadata`

The crafter will determine the internal approach. The architecture requires that the metadata SPI continues to support `ServiceLoader` discovery and that the `FeatureDispatcher` can construct both value-based and condition-based proxies through it.

## 6. Exception Changes

### UnmatchedVariantException (modified message)

For condition-based features, the exception message includes predicate information:

- Value-based: `"No variant matched for flag key 'checkout-flow' with value 'UNKNOWN'. Known variants: [CLASSIC, STREAMLINED]"`
- Condition-based: `"No condition matched for flag key 'pricing-tier'. Predicates evaluated: [IsEnterprise(order=1), IsStartup(order=2)]"`

No new exception types are introduced.

## 7. Type Summary

### New types

| Type | Package | Kind | Public API |
|------|---------|------|------------|
| `FeaturePredicate` | `com.flagzen` | `@FunctionalInterface` interface | Yes |
| `@Condition` | `com.flagzen` | Annotation type | Yes |
| `ConditionModel` | `com.flagzen.processor` | Record (processor-internal) | No |
| `DispatchMode` | `com.flagzen.processor` | Enum (processor-internal) | No |

### Modified types

| Type | Change |
|------|--------|
| `@Variant` | New `when` attribute, `value` default changed to `""` |
| `VariantModel` | New `condition` field (nullable) |
| `FeatureModel` | New `dispatchMode` field |
| `FlagZenProcessor` | Condition validation, dispatch mode detection, mixing rejection |
| `ProxyGenerator` | Condition-based proxy generation path |
| `UnmatchedVariantException` | Condition-aware error messages |

### Unchanged types

All other existing types remain unchanged: `@Feature`, `@DefaultVariant`, `FallbackStrategy`, `FeatureDispatcher`, `FlagZen`, `FlagProvider`, `ContextAccessor`, `InMemoryFlagProvider`, `FeatureMetadata`, `MethodModel`.
