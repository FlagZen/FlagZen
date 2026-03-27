# Component Boundaries -- Condition Predicates (flagzen-conditions)

## Overview

Condition predicates are entirely contained within flagzen-core. No new Gradle submodules are created. The Spring DI extension (US-CP-08) defers to flagzen-spring when M4 is available.

## Module Impact

### flagzen-core (modified)

All condition predicate types, annotation changes, processor extensions, and proxy generation changes live here.

**New types in `com.flagzen`**:

| Type | Kind | Responsibility |
|------|------|---------------|
| `FeaturePredicate` | `@FunctionalInterface` interface | Contract for user-defined condition predicates |
| `@Condition` | Annotation type | Declares predicate binding and evaluation order within `@Variant` |

**Modified types in `com.flagzen`**:

| Type | Change |
|------|--------|
| `@Variant` | New `when` attribute for condition binding |

**New types in `com.flagzen.processor`**:

| Type | Kind | Responsibility |
|------|------|---------------|
| `ConditionModel` | Record | Processor-internal model for @Condition metadata |
| `DispatchMode` | Enum | `VALUE_BASED` or `CONDITION_BASED` (processor-internal) |

**Modified types in `com.flagzen.processor`**:

| Type | Change |
|------|--------|
| `VariantModel` | New `condition` field (nullable ConditionModel) |
| `FeatureModel` | New `dispatchMode` field |
| `FlagZenProcessor` | Dispatch mode detection, condition validation (type check, constructor check, order uniqueness, mixing rejection, REQUIRED + conditions) |
| `ProxyGenerator` | Condition-based proxy generation path alongside existing value-based path |

**Modified types in `com.flagzen`**:

| Type | Change |
|------|--------|
| `UnmatchedVariantException` | Condition-aware error messages (predicate list instead of variant value list) |

### flagzen-test (unchanged)

No changes. `@PinFlag` and `TestFlagContext` remain for value-based features. Testing condition-based features is done by constructing `EvaluationContext` and calling methods -- no special test infrastructure needed.

### flagzen-spring (future -- US-CP-08)

When M4 is available, flagzen-spring gains:

- Predicate resolution from `ApplicationContext` for `@Component`-annotated predicates
- Relaxed no-arg constructor validation for Spring-managed predicates
- A `PredicateFactory` abstraction (or equivalent) that the proxy uses to obtain predicate instances

This is an extension of the existing flagzen-spring module, not a new module.

### All other modules (unchanged)

flagzen-env, flagzen-reactor, flagzen-mutiny, flagzen-launchdarkly, flagzen-togglz, flagzen-openfeature -- no changes.

## Package Structure (updated)

```
com.flagzen
  Feature.java                    (annotation -- unchanged)
  Variant.java                    (annotation -- modified: new 'when' attribute)
  DefaultVariant.java             (annotation -- unchanged)
  Condition.java                  (annotation -- NEW)
  FeaturePredicate.java           (interface -- NEW)
  FallbackStrategy.java           (enum -- unchanged)
  FeatureDispatcher.java          (interface -- unchanged)
  FlagZen.java                    (factory -- unchanged)
  FlagZenException.java           (exception -- unchanged)
  UnmatchedVariantException.java  (exception -- modified: condition-aware messages)

com.flagzen.spi
  FlagProvider.java               (SPI -- unchanged)
  ContextAccessor.java            (SPI -- unchanged, from M1)
  FeatureMetadata.java            (SPI -- may need condition-aware extension)

com.flagzen.internal
  DefaultFeatureDispatcher.java   (implementation -- may need condition-aware proxy construction)

com.flagzen.processor
  FlagZenProcessor.java           (processor -- modified: condition validation)
  FeatureModel.java               (model -- modified: dispatchMode field)
  VariantModel.java               (model -- modified: condition field)
  ConditionModel.java             (model -- NEW)
  DispatchMode.java               (enum -- NEW)
  MethodModel.java                (model -- unchanged)
  ProxyGenerator.java             (generator -- modified: condition dispatch path)
```

## Boundary Rules

### Existing rules (maintained)

- All SPIs in `com.flagzen.spi` -- stable public contract
- All internals in `com.flagzen.internal` -- package-private, not public API
- Processor in `com.flagzen.processor` -- compile-time only
- No class in flagzen-core imports from any extension module
- Zero external runtime dependencies in flagzen-core

### New rules

- `FeaturePredicate` and `@Condition` are public API in `com.flagzen` -- stable, versioned
- `ConditionModel` and `DispatchMode` are processor-internal -- not accessible to consumers
- User-defined `FeaturePredicate` implementations must NOT import from `com.flagzen.processor`
- `FeaturePredicate` implementations must NOT import from `com.flagzen.internal`

## Dependency Direction

No change to the dependency graph. All dependencies continue to point inward toward flagzen-core. Condition predicates are implemented by user code outside FlagZen -- they depend on `com.flagzen.FeaturePredicate` and `com.flagzen.EvaluationContext`, both in the core public API.

```
User code (implements FeaturePredicate)
       |
       v
flagzen-core (defines FeaturePredicate, @Condition, processes and generates)
       ^
       |
flagzen-spring (future: resolves @Component predicates from ApplicationContext)
```

## Public API Surface Change

| Before (M0) | After (M6) | Delta |
|-------------|------------|-------|
| 8 consumer-facing types | 10 consumer-facing types | +2 (`FeaturePredicate`, `@Condition`) |
| 3 SPI types | 3 SPI types | 0 |
| 4 test types | 4 test types | 0 |

The public API surface remains minimal. Two new types, both in `com.flagzen`, both with clear single responsibilities.
