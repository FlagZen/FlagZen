# Component Boundaries -- Condition Predicates (flagzen-conditions)

## Overview

Condition predicates are entirely contained within flagzen-core. No new Gradle submodules are created. The Spring DI extension (US-CP-08) defers to flagzen-spring when M4 is available.

## Module Impact

### flagzen-core (modified)

All condition predicate types, annotation changes, processor extensions, and proxy generation changes live here.

**New types in `com.flagzen`**:

|     Type     |      Kind       |                             Responsibility                              |
| ------------ | --------------- | ----------------------------------------------------------------------- |
| `@Condition` | Annotation type | Declares predicate binding within `@Variant` via `matches`/`notMatches` |

**Modified types in `com.flagzen`**:

|    Type    |                                          Change                                           |
| ---------- | ----------------------------------------------------------------------------------------- |
| `@Variant` | New `when` attribute for condition binding, new `order` attribute for evaluation sequence |

**New types in `com.flagzen.processor`**:

|       Type       |  Kind  |                  Responsibility                  |
| ---------------- | ------ | ------------------------------------------------ |
| `ConditionModel` | Record | Processor-internal model for @Condition metadata |

**Modified types in `com.flagzen.processor`**:

|        Type        |                                                         Change                                                          |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| `VariantModel`     | New `condition` field (nullable ConditionModel), new `order` field                                                      |
| `FeatureModel`     | New `hasOrderedDispatch` field                                                                                          |
| `FlagZenProcessor` | Condition validation (predicate type matches @Feature type, constructor check, order uniqueness, REQUIRED + conditions) |
| `ProxyGenerator`   | Unified ordered dispatch generation path alongside existing value-based path                                            |

**Modified types in `com.flagzen`**:

|            Type             |                                    Change                                     |
| --------------------------- | ----------------------------------------------------------------------------- |
| `UnmatchedVariantException` | Condition-aware error messages (predicate list instead of variant value list) |

### flagzen-test (unchanged)

No changes. `@PinFlag` and `TestFlagContext` remain for value-based features. Testing condition-based features is done by providing flag values and calling methods -- no special test infrastructure needed.

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
  Variant.java                    (annotation -- modified: new 'when' and 'order' attributes)
  DefaultVariant.java             (annotation -- unchanged)
  Condition.java                  (annotation -- NEW)
  FallbackStrategy.java           (enum -- unchanged)
  FeatureDispatcher.java          (interface -- unchanged)
  FlagZen.java                    (factory -- unchanged)
  FlagZenException.java           (exception -- unchanged)
  UnmatchedVariantException.java  (exception -- modified: condition-aware messages)

com.flagzen.spi
  FlagProvider.java               (SPI -- unchanged)
  ContextAccessor.java            (SPI -- unchanged)
  FeatureMetadata.java            (SPI -- may need condition-aware extension)

com.flagzen.internal
  DefaultFeatureDispatcher.java   (implementation -- may need condition-aware proxy construction)

com.flagzen.processor
  FlagZenProcessor.java           (processor -- modified: condition validation)
  FeatureModel.java               (model -- modified: hasOrderedDispatch field)
  VariantModel.java               (model -- modified: condition and order fields)
  ConditionModel.java             (model -- NEW)
  MethodModel.java                (model -- unchanged)
  ProxyGenerator.java             (generator -- modified: unified ordered dispatch path)
```

## Boundary Rules

### Existing rules (maintained)

- All SPIs in `com.flagzen.spi` -- stable public contract
- All internals in `com.flagzen.internal` -- package-private, not public API
- Processor in `com.flagzen.processor` -- compile-time only
- No class in flagzen-core imports from any extension module
- Zero external runtime dependencies in flagzen-core

### New rules

- `@Condition` is public API in `com.flagzen` -- stable, versioned
- `ConditionModel` is processor-internal -- not accessible to consumers
- User-defined predicate implementations must NOT import from `com.flagzen.processor`
- User-defined predicate implementations must NOT import from `com.flagzen.internal`
- Predicates use JDK functional interfaces (`Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`) -- no FlagZen-specific predicate interface

## Dependency Direction

No change to the dependency graph. All dependencies continue to point inward toward flagzen-core. Condition predicates are implemented by user code outside FlagZen -- they implement standard JDK functional interfaces and test flag values directly.

No M1 (EvaluationContext) dependency -- predicates test flag values, not EvaluationContext.

```
User code (implements Predicate<String> / IntPredicate / etc.)
       |
       v
flagzen-core (defines @Condition, processes and generates)
       ^
       |
flagzen-spring (future: resolves @Component predicates from ApplicationContext)
```

## Public API Surface Change

|       Before (M0)       |       After (M6)        |       Delta       |
| ----------------------- | ----------------------- | ----------------- |
| 8 consumer-facing types | 9 consumer-facing types | +1 (`@Condition`) |
| 3 SPI types             | 3 SPI types             | 0                 |
| 4 test types            | 4 test types            | 0                 |

The public API surface remains minimal. One new type in `com.flagzen` with a clear single responsibility. No FlagZen-specific predicate interface -- users implement standard JDK functional interfaces.
