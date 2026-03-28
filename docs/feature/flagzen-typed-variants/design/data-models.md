# Data Models -- Typed Variants (flagzen-typed-variants)

## Overview

This document details the data models introduced or modified by the typed variants milestone. It extends the existing data models documents from M0 and M1.

## 1. New Types

### FeatureType Enum

Defines the type of flag value a `@Feature` dispatches on.

| Constant |           Description            | Dispatch Strategy  |
| -------- | -------------------------------- | ------------------ |
| STRING   | String-valued flag (default, M0) | Exact map lookup   |
| INT      | Integer-valued flag              | Exact map lookup   |
| LONG     | Long-valued flag                 | Exact map lookup   |
| BOOLEAN  | Boolean-valued flag              | Exact map lookup   |
| DOUBLE   | Double-valued flag (approximate) | Iterate with delta |

**Package**: `com.flagzen`

**Constraints**:

- Enum is public, part of the annotation API
- Used at compile time only -- drives code generation, not used at runtime
- STRING is the default for backward compatibility

### @CloseTo Annotation

Expresses an approximate double value for variant matching.

| Attribute |  Type  |  Default   |            Description             |
| --------- | ------ | ---------- | ---------------------------------- |
| value     | double | (required) | Target double value                |
| delta     | double | 1e-10      | Tolerance for approximate matching |

**Package**: `com.flagzen`

**Retention**: CLASS. **Target**: (nested annotation, used within `@Variant.doubleValue`)

**Constraints**:

- `delta` must be positive (> 0) -- validated by processor
- `delta` must be finite (not infinity, not NaN) -- validated by processor
- Zero delta is rejected (semantically identical to exact match but uses iteration)
- Matching logic: `Math.abs(flagValue - value) <= delta`

See ADR-013 for delta strategy rationale.

### @WhenTrue Annotation

Convenience annotation equivalent to `@Variant(booleanValue = true)`.

| Attribute |    Type    |  Default   |                   Description                   |
| --------- | ---------- | ---------- | ----------------------------------------------- |
| of        | `Class<?>` | void.class | Target @Feature interface (multi-feature class) |

**Package**: `com.flagzen`

**Retention**: CLASS. **Target**: TYPE. **@Repeatable** with `@WhenTrues` container.

**Constraints**:

- Processor normalizes to VariantModel with booleanValue `true` before validation
- Only valid on BOOLEAN features -- compile error otherwise
- `of` follows same rules as `@Variant.of` (required for multi-feature classes)

### @WhenFalse Annotation

Convenience annotation equivalent to `@Variant(booleanValue = false)`.

| Attribute |    Type    |  Default   |                   Description                   |
| --------- | ---------- | ---------- | ----------------------------------------------- |
| of        | `Class<?>` | void.class | Target @Feature interface (multi-feature class) |

**Package**: `com.flagzen`

**Retention**: CLASS. **Target**: TYPE. **@Repeatable** with `@WhenFalses` container.

**Constraints**: Same as `@WhenTrue` but normalized to booleanValue `false`.

See ADR-014 for `@WhenTrue`/`@WhenFalse` rationale.

### @WhenTrues Container Annotation

Repeatable container for `@WhenTrue`.

**Package**: `com.flagzen`

**Retention**: CLASS. **Target**: TYPE.

### @WhenFalses Container Annotation

Repeatable container for `@WhenFalse`.

**Package**: `com.flagzen`

**Retention**: CLASS. **Target**: TYPE.

## 2. Modified Annotations

### @Feature (Modified)

| Attribute |       Type       |      Default       |           Description           |  Status  |
| --------- | ---------------- | ------------------ | ------------------------------- | -------- |
| value     | String           | (required)         | Flag key                        | Existing |
| fallback  | FallbackStrategy | REQUIRED           | Fallback strategy               | Existing |
| type      | FeatureType      | FeatureType.STRING | Type of flag value for dispatch | NEW      |

**Constraints**:

- Default `FeatureType.STRING` preserves backward compatibility
- The `type` attribute drives code generation: which `FlagProvider` method the proxy calls and what map type it uses
- Retention stays CLASS

### @Variant (Modified)

|  Attribute   |    Type    |        Default        |                  Description                   |  Status  |
| ------------ | ---------- | --------------------- | ---------------------------------------------- | -------- |
| value        | String     | `""` (empty sentinel) | String variant value                           | MODIFIED |
| of           | `Class<?>` | void.class            | Target @Feature interface                      | Existing |
| intValue     | int        | `Integer.MIN_VALUE`   | Integer variant value (for INT features)       | NEW      |
| longValue    | long       | `Long.MIN_VALUE`      | Long variant value (for LONG features)         | NEW      |
| booleanValue | String     | `""` (empty sentinel) | Boolean variant value as string "true"/"false" | NEW      |
| doubleValue  | @CloseTo   | sentinel @CloseTo     | Double variant value with tolerance            | NEW      |
| order        | int        | -1 (unset sentinel)   | Dispatch order (ADR-008)                       | Existing |

**Constraints**:

- Exactly one of `value`, `intValue`, `booleanValue`, `longValue`, or `doubleValue` must be set per annotation instance (processor-enforced)
- The set attribute must match the `@Feature.type()` of the target feature (processor-enforced)
- `value` default changes from required to empty sentinel `""` to allow typed attributes without a string value
- `booleanValue` uses String type (`"true"`/`"false"`) instead of primitive `boolean` to enable a tri-state sentinel (empty string = unset). Java annotations do not support nullable boolean.
- `doubleValue` default is a sentinel `@CloseTo` with `value = Double.NaN` -- NaN signals "not set" since NaN is never a valid match target
- Retention stays CLASS. @Repeatable with `@Variants` container stays.

**Sentinel Detection**:

|  Attribute   |         Sentinel Value         |              Detection              |
| ------------ | ------------------------------ | ----------------------------------- |
| value        | `""` (empty string)            | `value.isEmpty()`                   |
| intValue     | `Integer.MIN_VALUE`            | `intValue == Integer.MIN_VALUE`     |
| longValue    | `Long.MIN_VALUE`               | `longValue == Long.MIN_VALUE`       |
| booleanValue | `""` (empty string)            | `booleanValue.isEmpty()`            |
| doubleValue  | `@CloseTo(value = Double.NaN)` | `Double.isNaN(doubleValue.value())` |

**Note on sentinels**: `Integer.MIN_VALUE` and `Long.MIN_VALUE` are technically valid flag values but extremely unlikely in practice. If a developer needs to match `Integer.MIN_VALUE`, they can use string-based dispatch. The crafter may choose alternative sentinel strategies if better options exist -- the behavioral contract is that the processor can distinguish "set" from "unset".

## 3. Modified Compile-Time Models

### FeatureModel (Modified)

|          Field          |         Type         |              Description              |  Status  |
| ----------------------- | -------------------- | ------------------------------------- | -------- |
| packageName             | String               | Package of the @Feature interface     | Existing |
| interfaceName           | String               | Simple class name                     | Existing |
| flagKey                 | String               | Flag key from `@Feature("key")`       | Existing |
| fallbackStrategy        | FallbackStrategy     | REQUIRED, EXCEPTION, or NOOP          | Existing |
| methods                 | List of MethodModel  | Methods on the interface              | Existing |
| variants                | List of VariantModel | Discovered variants                   | Existing |
| defaultVariantClassName | String (nullable)    | @DefaultVariant class name            | Existing |
| featureType             | FeatureType          | STRING, INT, LONG, BOOLEAN, or DOUBLE | NEW      |

The `featureType` field drives:

- Which `@Variant` attribute is expected (validation)
- Which proxy dispatch strategy is generated (code generation)
- Which `FlagProvider` method the proxy calls (code generation)

### VariantModel (Modified)

The VariantModel must store typed values alongside the string value. The current VariantModel is a record with `qualifiedClassName` and `variantValue` (String).

|        Field        |      Type      |                 Description                  |  Status  |
| ------------------- | -------------- | -------------------------------------------- | -------- |
| qualifiedClassName  | String         | Fully qualified variant class name           | Existing |
| variantValue        | String         | String variant value (for STRING features)   | Existing |
| intVariantValue     | Integer (null) | Integer variant value (for INT features)     | NEW      |
| longVariantValue    | Long (null)    | Long variant value (for LONG features)       | NEW      |
| booleanVariantValue | Boolean (null) | Boolean variant value (for BOOLEAN features) | NEW      |
| doubleVariantValue  | Double (null)  | Double variant value (for DOUBLE features)   | NEW      |
| doubleDelta         | Double (null)  | Delta tolerance (for DOUBLE features)        | NEW      |
| order               | int            | Dispatch order (-1 = unset)                  | Existing |

**Constraints**:

- Exactly one value type is populated per VariantModel (the one matching the feature's type)
- For DOUBLE variants, both `doubleVariantValue` and `doubleDelta` are populated
- The record may evolve to a sealed type hierarchy or remain a flat record with nullable fields -- the crafter decides the internal representation

**@WhenTrue/@WhenFalse representation**: After normalization, a `@WhenTrue` becomes a VariantModel with `booleanVariantValue = true` and all other typed values null. Indistinguishable from `@Variant(booleanValue = "true")` post-normalization.

## 4. Modified SPI Contracts

### FlagProvider (Modified)

```
package com.flagzen.spi;

public interface FlagProvider {
    // Existing
    Optional<String> getString(String key);
    default Optional<String> getString(String key, EvaluationContext context) {
        return getString(key);
    }

    // NEW: Typed accessors (default methods parsing from getString)
    default Optional<Boolean> getBoolean(String key) { ... }
    default Optional<Boolean> getBoolean(String key, EvaluationContext context) {
        return getBoolean(key);
    }

    default OptionalInt getInt(String key) { ... }
    default OptionalInt getInt(String key, EvaluationContext context) {
        return getInt(key);
    }

    default OptionalLong getLong(String key) { ... }
    default OptionalLong getLong(String key, EvaluationContext context) {
        return getLong(key);
    }

    default OptionalDouble getDouble(String key) { ... }
    default OptionalDouble getDouble(String key, EvaluationContext context) {
        return getDouble(key);
    }
}
```

**Parse rules for default methods**:

|    Method    |             Parse Logic              |               Empty When               |
| ------------ | ------------------------------------ | -------------------------------------- |
| `getBoolean` | "true"/"false" case-insensitive only | getString empty, not "true"/"false"    |
| `getInt`     | `Integer.parseInt(value.trim())`     | getString empty, parse fails           |
| `getLong`    | `Long.parseLong(value.trim())`       | getString empty, parse fails, overflow |
| `getDouble`  | `Double.parseDouble(value.trim())`   | getString empty, parse fails           |

**Important**: `getBoolean` is stricter than `Boolean.parseBoolean()`. Only "true" and "false" (case-insensitive) return a value. "1", "yes", "0", "no" all return empty. This prevents silent misinterpretation of non-boolean strings.

## 5. Generated Proxy Structure (Updated)

### Proxy Internal State by FeatureType

| FeatureType |                   Variant Map/List Field                   |
| ----------- | ---------------------------------------------------------- |
| STRING      | `Map<String, Supplier<T>>` (unchanged from M0)             |
| INT         | `Map<Integer, Supplier<T>>`                                |
| LONG        | `Map<Long, Supplier<T>>`                                   |
| BOOLEAN     | `Map<Boolean, Supplier<T>>`                                |
| DOUBLE      | List of `(double value, double delta, Supplier<T>)` tuples |

### Proxy Dispatch Logic (Updated)

For STRING features: unchanged.

For INT/LONG/BOOLEAN features:

1. Call typed `FlagProvider` method (with or without context, same as M1 logic)
2. If value present: `map.get(typedValue)` to find variant supplier
3. If found: delegate method call
4. If not found: default variant or fallback strategy (unchanged logic)

For DOUBLE features:

1. Call `flagProvider.getDouble(key)` (with or without context)
2. If value present: iterate variant list
3. For each: `Math.abs(flagValue - variant.value) <= variant.delta`
4. First match: delegate method call
5. No match: default variant or fallback strategy

### FeatureMetadata (Updated)

The generated `{Feature}_FlagZenMetadata` evolves to carry `FeatureType` and construct the proxy with the appropriate typed data structure.

|       Method       |   Return Type    |         Description          |  Status  |
| ------------------ | ---------------- | ---------------------------- | -------- |
| featureType()      | `Class<T>`       | The @Feature interface class | Existing |
| flagKey()          | String           | The flag key                 | Existing |
| fallbackStrategy() | FallbackStrategy | Configured strategy          | Existing |
| featureValueType() | FeatureType      | The FeatureType enum value   | NEW      |
| createProxy(...)   | T                | Factory method (typed)       | MODIFIED |

## 6. Compile-Time Validation Rules (New)

### Type Mismatch Detection

| Feature Type | Valid @Variant Attribute |                          Error on Invalid                          |
| ------------ | ------------------------ | ------------------------------------------------------------------ |
| STRING       | `value`                  | "Feature 'X' declares type STRING but variant Y uses intValue..."  |
| INT          | `intValue`               | "Feature 'X' declares type INT but variant Y uses string value..." |
| LONG         | `longValue`              | "Use @Variant(longValue = ...) instead"                            |
| BOOLEAN      | `booleanValue`           | "Use @Variant(booleanValue = ...) or @WhenTrue/@WhenFalse"         |
| DOUBLE       | `doubleValue`            | "Use @Variant(doubleValue = @CloseTo(...)) instead"                |

### BOOLEAN REQUIRED Completeness

For `@Feature(type = BOOLEAN, fallback = REQUIRED)`:

- Must have variants for both `true` and `false`, OR
- Must have a `@DefaultVariant` covering the missing case

Incomplete BOOLEAN REQUIRED features produce a compile error: "REQUIRED BOOLEAN feature 'X' requires variants for both true and false, or a @DefaultVariant."

### Duplicate Typed Value Detection

Two variants with the same typed value for the same feature produce a compile error:

- INT: duplicate `intValue`
- LONG: duplicate `longValue`
- BOOLEAN: duplicate `booleanValue` (e.g., two `@WhenTrue` on same feature)
- DOUBLE: not checked (overlap resolved by ordering -- see ADR-013)
- STRING: duplicate `value` (existing rule, unchanged)

## 7. Exception Hierarchy -- Unchanged

No new exceptions. `UnmatchedVariantException` message adapts to include typed values:

- STRING: "Flag 'X' returned value 'Y' but no variant matches. Known values: [A, B]"
- INT: "Flag 'X' returned value 42 but no variant matches. Known intValues: [3, 10]"
- BOOLEAN: "Flag 'X' returned true but no variant matches."
- DOUBLE: "Flag 'X' returned 0.75 but no variant matches within delta. Known values: [0.1 +/-1e-10, 0.5 +/-0.01]"
