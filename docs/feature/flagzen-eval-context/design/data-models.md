# Data Models -- Evaluation Context (flagzen-eval-context)

## Overview

This document details the data models introduced or modified by the evaluation context milestone. It extends the existing data models document from M0.

## 1. New Runtime Models

### EvaluationContext

Carries contextual information for targeted flag resolution (A/B testing, user segmentation, tenant-scoped features).

|    Field     |        Type         |                   Description                    |
| ------------ | ------------------- | ------------------------------------------------ |
| targetingKey | String (nullable)   | Primary identifier (user ID, session ID, tenant) |
| attributes   | Map<String, Object> | Custom attributes for targeting rules            |

**Constraints**:

- Immutable after construction (no setters)
- `attributes` is never null (empty `Map.of()` when no attributes set)
- `attributes` map is unmodifiable (wraps builder input in `Collections.unmodifiableMap`)
- Attribute values are `Object` to support String, Boolean, Integer, Double, List without type explosion
- No validation on attribute values -- FlagZen is not a rules engine
- Thread-safe by design (immutability)
- Meaningful `toString()`, `equals()`, `hashCode()`

**Construction**: Builder pattern via `EvaluationContext.builder()`.

|           Builder Method            |             Description             |
| ----------------------------------- | ----------------------------------- |
| `targetingKey(String)`              | Sets the targeting key (nullable)   |
| `attribute(String key, Object val)` | Adds a single attribute             |
| `attributes(Map<String, Object>)`   | Sets all attributes (replaces)      |
| `build()`                           | Returns immutable EvaluationContext |

**Design note**: Consider implementing as a Java class with a nested static `Builder` rather than a record, since records do not support the builder pattern naturally without a companion class. The crafter decides the final approach. The behavioral contract is: immutable, builder-constructed, content-based equality.

### FlagContext

Provides block-scoped evaluation context to avoid parameter drilling through call stacks.

|    Aspect    |                            Description                             |
| ------------ | ------------------------------------------------------------------ |
| Type         | Final class with static methods only (no instantiation)            |
| Storage (R1) | `ThreadLocal<EvaluationContext>`                                   |
| Storage (R2) | `ScopedValue<EvaluationContext>` on Java 21+, ThreadLocal fallback |

**Static methods**:

|                   Method                    |                       Description                        |
| ------------------------------------------- | -------------------------------------------------------- |
| `run(EvaluationContext, Runnable)`          | Scopes context to block, cleans up on exit               |
| `<T> T run(EvaluationContext, Supplier<T>)` | Scopes context to block, returns result, cleans up       |
| `current()`                                 | Returns current scoped context or null (package-private) |

**Behavioral contracts**:

- Nested `run()` calls: inner context overrides outer; outer restored on inner exit
- Exception safety: context cleaned up in finally block, exception propagates
- Thread isolation: each thread/virtual-thread has its own scope
- `current()` is package-private or internal -- not part of public API (only used by generated proxies and DefaultFeatureDispatcher)

### ContextResolver (Internal)

Encapsulates the resolution chain logic. Not part of the public API.

|     Field      |               Type               |             Description             |
| -------------- | -------------------------------- | ----------------------------------- |
| accessors      | `List<ContextAccessor>` (sorted) | Immutable, sorted by priority       |
| defaultContext | `EvaluationContext` (nullable)   | Fallback context from configuration |

|                  Method                  |                      Description                       |
| ---------------------------------------- | ------------------------------------------------------ |
| `resolve(EvaluationContext explicitCtx)` | Returns resolved context or null following chain order |

## 2. Modified SPI Contracts

### FlagProvider (Modified)

```
package com.flagzen.spi;

public interface FlagProvider {
    Optional<String> getString(String key);

    // NEW: default method for backward compatibility
    default Optional<String> getString(String key, EvaluationContext context) {
        return getString(key);
    }
}
```

The default method delegates to `getString(key)`, ignoring context. Context-aware providers override this method.

### ContextAccessor (New SPI)

```
package com.flagzen.spi;

public interface ContextAccessor {
    Optional<EvaluationContext> getContext();
    int priority();  // lower number = higher priority, default: 100
}
```

Discovery: `java.util.ServiceLoader` via `META-INF/services/com.flagzen.spi.ContextAccessor`.

Multiple accessors sorted by priority. First non-empty result wins. Empty results skipped gracefully.

## 3. Modified Runtime Models

### FeatureDispatcher (Modified Interface)

```
package com.flagzen;

public interface FeatureDispatcher {
    <T> T resolve(Class<T> featureType);

    // NEW: resolve with explicit context
    <T> T resolve(Class<T> featureType, EvaluationContext context);
}
```

### FlagZenConfiguration (Modified)

|     Field      |             Type             |              Description              |
| -------------- | ---------------------------- | ------------------------------------- |
| provider       | FlagProvider                 | The configured flag source (existing) |
| defaultContext | EvaluationContext (nullable) | Default context for resolution chain  |

New configuration method: `defaultContext(EvaluationContext)`.

## 4. Generated Proxy Structure (Modified)

### Proxy Dispatch Logic (per method) -- Updated

For each method on the @Feature interface, the proxy's `resolveVariant()` method is modified:

1. Reads context from `FlagContext.current()`
2. If context is present: queries `flagProvider.getString(flagKey, context)`
3. If context is absent: queries `flagProvider.getString(flagKey)` (M0 behavior)
4. Remainder of dispatch logic (variant lookup, default, fallback) is unchanged

### Proxy Class Shape -- Unchanged

|    Aspect    |                                  Value                                  |
| ------------ | ----------------------------------------------------------------------- |
| Class name   | `{FeatureSimpleName}_FlagZenProxy`                                      |
| Package      | Same as @Feature interface                                              |
| Visibility   | Public class, package-private constructor                               |
| Implements   | The @Feature interface                                                  |
| Dependencies | `FlagProvider`, variant instance suppliers, `FlagContext` (static read) |

The constructor signature is unchanged. No new fields are added to the proxy. Context is read from `FlagContext.current()` at dispatch time.

## 5. Exception Hierarchy -- Unchanged

No new exceptions are introduced. Existing exceptions cover all error cases:

- `FlagZenException` -- base exception (covers configuration errors)
- `UnmatchedVariantException` -- flag value has no matching variant (unchanged behavior)
