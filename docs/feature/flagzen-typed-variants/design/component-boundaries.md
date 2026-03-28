# Component Boundaries -- Typed Variants (flagzen-typed-variants)

## Overview

All typed variant types are within flagzen-core. No new modules are introduced. This document specifies the exact package placement of each new and modified type.

## Package Assignments

### com.flagzen (Public API)

|     Type      |         Kind         |  Status  |                      Rationale                       |
| ------------- | -------------------- | -------- | ---------------------------------------------------- |
| `FeatureType` | Enum                 | NEW      | Annotation attribute type, used by `@Feature.type()` |
| `@CloseTo`    | Annotation           | NEW      | Nested annotation within `@Variant.doubleValue()`    |
| `@WhenTrue`   | Annotation           | NEW      | Convenience sugar for `@Variant(booleanValue=true)`  |
| `@WhenFalse`  | Annotation           | NEW      | Convenience sugar for `@Variant(booleanValue=false)` |
| `@WhenTrues`  | Container annotation | NEW      | Repeatable container for `@WhenTrue`                 |
| `@WhenFalses` | Container annotation | NEW      | Repeatable container for `@WhenFalse`                |
| `@Feature`    | Annotation           | MODIFIED | New `type()` attribute with FeatureType default      |
| `@Variant`    | Annotation           | MODIFIED | New typed attributes (intValue, longValue, etc.)     |

**Rationale for `com.flagzen`**: All annotation types and their supporting enums belong in the top-level public API package. Developers write `@Feature`, `@Variant`, `@WhenTrue`, `@WhenFalse`, and `@CloseTo` directly -- they must be importable from the same package as existing annotations.

### com.flagzen.spi (SPI Contracts)

|      Type      |   Kind    |  Status  |                      Rationale                       |
| -------------- | --------- | -------- | ---------------------------------------------------- |
| `FlagProvider` | Interface | MODIFIED | New typed default methods (getInt, getBoolean, etc.) |

**Rationale for `com.flagzen.spi`**: `FlagProvider` remains the SPI interface. Typed methods are additions to the existing contract via default methods.

### com.flagzen.processor (Annotation Processor -- Compile-Time Only)

|        Type        |  Kind  |  Status  |                                        Rationale                                         |
| ------------------ | ------ | -------- | ---------------------------------------------------------------------------------------- |
| `FlagZenProcessor` | Class  | MODIFIED | Reads `@Feature.type()`, normalizes `@WhenTrue`/`@WhenFalse`, validates type consistency |
| `FeatureModel`     | Record | MODIFIED | New `featureType` field                                                                  |
| `VariantModel`     | Record | MODIFIED | New typed value fields (intVariantValue, etc.)                                           |
| `ProxyGenerator`   | Class  | MODIFIED | Generates typed dispatch logic per FeatureType                                           |

**Rationale for `com.flagzen.processor`**: All processor-internal types stay in the processor package. No new types -- only modifications to existing records and classes.

### com.flagzen.internal (Internal Implementation)

No new types. No modifications.

`DefaultFeatureDispatcher` is unaffected -- typed dispatch is entirely within the generated proxy. The dispatcher resolves the proxy via `FeatureMetadata` as before; the proxy handles typed flag resolution internally.

## Boundary Rules

### Existing Rules (Unchanged)

- All annotations and public enums in `com.flagzen`
- All SPIs in `com.flagzen.spi`
- All internals in `com.flagzen.internal` (package-private)
- Annotation processor in `com.flagzen.processor` (compile-time only)
- No class in flagzen-core may import from any extension module
- No `java.lang.reflect` in runtime code

### New Rules (This Milestone)

- `FeatureType` enum is public and available to annotation consumers
- `@CloseTo` is public but designed for use as a nested annotation within `@Variant.doubleValue()`
- `@WhenTrue`/`@WhenFalse` are public annotations alongside `@Variant`
- `FlagProvider` typed methods are default methods only -- never abstract
- Processor normalization of `@WhenTrue`/`@WhenFalse` happens before any validation pass

## Module Impact

### flagzen-core

All changes are within this module. Summary:

- 6 new types: `FeatureType`, `@CloseTo`, `@WhenTrue`, `@WhenFalse`, `@WhenTrues`, `@WhenFalses`
- 6 modified types: `@Feature`, `@Variant`, `FlagProvider`, `FlagZenProcessor`, `FeatureModel`, `VariantModel`, `ProxyGenerator`
- 0 new internal types

### flagzen-test

No changes required. `@PinFlag` continues to specify variant values as strings. Typed proxies parse via `FlagProvider` default methods, which delegate to `getString()`. The test framework is unaware of `FeatureType`.

Future enhancement: typed `@PinFlag` variants (out of scope for M2).

### Provider Adapter Modules

No changes required for this milestone. Existing adapters compile unchanged because:

- `FlagProvider` typed methods are default methods
- Adapters override typed methods when they add native type support (per adapter milestone)

### flagzen-spring

No changes required. Spring auto-configuration creates `DefaultFeatureDispatcher` which is unaffected. Generated proxies handle typed dispatch internally.

## Public API Surface Evolution

### New Public Types

|     Type      |    Package    |                    Description                    |
| ------------- | ------------- | ------------------------------------------------- |
| `FeatureType` | `com.flagzen` | Enum: STRING, INT, LONG, BOOLEAN, DOUBLE          |
| `@CloseTo`    | `com.flagzen` | Annotation: value() + delta() for double matching |
| `@WhenTrue`   | `com.flagzen` | Annotation: convenience for boolean true variant  |
| `@WhenFalse`  | `com.flagzen` | Annotation: convenience for boolean false variant |
| `@WhenTrues`  | `com.flagzen` | Container annotation for repeatable `@WhenTrue`   |
| `@WhenFalses` | `com.flagzen` | Container annotation for repeatable `@WhenFalse`  |

### Modified Public Types

|      Type      |      Package      |                               Change                               |
| -------------- | ----------------- | ------------------------------------------------------------------ |
| `@Feature`     | `com.flagzen`     | +`type()` attribute (FeatureType, default STRING)                  |
| `@Variant`     | `com.flagzen`     | +`intValue`, `longValue`, `booleanValue`, `doubleValue` attributes |
| `FlagProvider` | `com.flagzen.spi` | +8 default methods (4 typed + 4 context-aware overloads)           |

### Updated Count

- Public types: 23 -> 29 (+6, including container annotations)
- Public methods on `FlagProvider`: approximately +8
- Still within target: <35 public types, <75 public methods
