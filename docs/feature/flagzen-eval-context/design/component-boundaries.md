# Component Boundaries -- Evaluation Context (flagzen-eval-context)

## Overview

All evaluation context types are within flagzen-core. No new modules are introduced. This document specifies the exact package placement of each new and modified type.

## Package Assignments

### com.flagzen (Public API)

|        Type         |      Kind       |  Status  |                    Rationale                     |
| ------------------- | --------------- | -------- | ------------------------------------------------ |
| `EvaluationContext` | Immutable class | NEW      | Public API -- consumers build and pass contexts  |
| `FlagContext`       | Final utility   | NEW      | Public API -- consumers scope context to blocks  |
| `FeatureDispatcher` | Interface       | MODIFIED | New `resolve(Class, EvaluationContext)` overload |
| `FlagZen`           | Factory class   | MODIFIED | Configuration gains `defaultContext()`           |

**Rationale for `com.flagzen`**: These types are directly used by library consumers. They belong in the top-level public API package alongside `FeatureDispatcher` and `FlagZen`.

### com.flagzen.spi (SPI Contracts)

|       Type        |   Kind    |  Status  |                      Rationale                      |
| ----------------- | --------- | -------- | --------------------------------------------------- |
| `FlagProvider`    | Interface | MODIFIED | New default method `getString(key, context)`        |
| `ContextAccessor` | Interface | NEW      | SPI for pluggable context sources (reactor, mutiny) |

**Rationale for `com.flagzen.spi`**: `ContextAccessor` is an SPI -- implemented by extension modules, discovered via ServiceLoader. It follows the same pattern as `FlagProvider`.

### com.flagzen.internal (Internal Implementation)

|            Type            | Kind  |  Status  |                    Rationale                    |
| -------------------------- | ----- | -------- | ----------------------------------------------- |
| `DefaultFeatureDispatcher` | Class | MODIFIED | Integrates ContextResolver into resolution flow |
| `ContextResolver`          | Class | NEW      | Encapsulates resolution chain, not public API   |

**Rationale for `com.flagzen.internal`**: `ContextResolver` is an implementation detail of the resolution chain. It is not exposed to consumers. Package-private visibility enforced.

### com.flagzen.processor (Annotation Processor -- Compile-Time Only)

|       Type       | Kind  |  Status  |                     Rationale                     |
| ---------------- | ----- | -------- | ------------------------------------------------- |
| `ProxyGenerator` | Class | MODIFIED | Generates context-aware dispatch in proxy methods |

No new types in the processor package. `ProxyGenerator` is modified to emit `FlagContext.current()` reads in generated `resolveVariant()` methods.

## Boundary Rules (Unchanged + New)

### Existing Rules (From M0)

- All SPIs are in `com.flagzen.spi` -- stable public contract
- All internals are in `com.flagzen.internal` -- package-private, not part of public API
- Annotation processor is in `com.flagzen.processor` -- compile-time only
- No class in flagzen-core may import from any extension module
- No `java.lang.reflect` in runtime code

### New Rules (This Milestone)

- `EvaluationContext` has no setters and returns unmodifiable collections
- `FlagContext` is final with no public constructors
- `FlagContext.current()` is package-private or internal -- not part of public API
- `ContextResolver` is package-private -- not visible outside `com.flagzen.internal`
- `ContextAccessor` implementations must be thread-safe (documented contract)

## Module Impact

### flagzen-core

All changes are within this module. Summary:

- 3 new types: `EvaluationContext`, `FlagContext`, `ContextAccessor`
- 1 new internal type: `ContextResolver`
- 4 modified types: `FeatureDispatcher`, `FlagProvider`, `DefaultFeatureDispatcher`, `ProxyGenerator`
- 1 modified configuration: `FlagZen.FlagZenConfiguration`

### flagzen-test

No changes required for this milestone. `@PinFlag`, `TestFlagContext`, and `FlagZenExtension` continue to work because:

- `resolve(Class<T>)` is unchanged
- `InMemoryFlagProvider.getString(key)` is unchanged
- The default method on `FlagProvider` handles context transparently

Future enhancement: context-aware test assertions (out of scope for M1).

### Provider Adapter Modules

No changes required for this milestone. Existing adapters compile unchanged because:

- `FlagProvider.getString(key, context)` is a default method
- Adapters override when they add context support (per adapter milestone)

### flagzen-spring

No changes required. Spring auto-configuration creates `DefaultFeatureDispatcher` which internally handles context resolution.

## Public API Surface Evolution

### New Public Types

|        Type         |      Package      |                                              Methods (public)                                              |
| ------------------- | ----------------- | ---------------------------------------------------------------------------------------------------------- |
| `EvaluationContext` | `com.flagzen`     | `targetingKey()`, `attributes()`, `getAttribute(key)`, `builder()`, `toString()`, `equals()`, `hashCode()` |
| `FlagContext`       | `com.flagzen`     | `run(EvaluationContext, Runnable)`, `run(EvaluationContext, Supplier)`                                     |
| `ContextAccessor`   | `com.flagzen.spi` | `getContext()`, `priority()`                                                                               |

### Modified Public Types

|          Type          |      Package      |                      Change                       |
| ---------------------- | ----------------- | ------------------------------------------------- |
| `FeatureDispatcher`    | `com.flagzen`     | +`resolve(Class, EvaluationContext)`              |
| `FlagProvider`         | `com.flagzen.spi` | +`getString(String, EvaluationContext)` (default) |
| `FlagZenConfiguration` | `com.flagzen`     | +`defaultContext(EvaluationContext)`              |

### Updated Count

- Public types: 20 -> 23 (+3)
- Public methods: approximately +10
- Still within target: <25 public types, <60 public methods
