# Data Models -- Condition Predicates (flagzen-conditions)

## Overview

This document describes the new and modified data models for condition-based predicate dispatch. All models are additive -- no existing models are removed or have breaking changes.

## 1. New Public API Types

### @Condition

Nested annotation type used exclusively within `@Variant(when = ...)`.

|      Aspect      |                              Value                               |
| ---------------- | ---------------------------------------------------------------- |
| Package          | `com.flagzen`                                                    |
| Kind             | Annotation type                                                  |
| Retention        | `CLASS`                                                          |
| Target           | Implicitly scoped via `@Variant` (applies to `TYPE`)             |
| Standalone usage | Not supported -- used only as `@Variant(when = @Condition(...))` |

| Attribute    |       Type        |             Default             |                                                  Description                                                  |
| ------------ | ----------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `matches`    | `Class<?>`        | (required -- see sentinel note) | The predicate class to evaluate. Must match `@Feature(type=...)`: `Predicate<String>`, `IntPredicate`, etc.   |
| `notMatches` | `Class<?>`        | sentinel                        | Negation predicate (mutually exclusive with `matches`). Same type constraints apply.                          |

`matches` and `notMatches` are mutually exclusive. The processor emits a compile error if both are specified on the same `@Condition`.

Predicate type constraints -- the processor validates that the predicate class matches the feature's declared type:

| `@Feature(type=...)` | Required predicate type       |
| --------------------- | ----------------------------- |
| `STRING` (default)    | `java.util.function.Predicate<String>` |
| `INT`                 | `java.util.function.IntPredicate`      |
| `LONG`                | `java.util.function.LongPredicate`     |
| `DOUBLE`              | `java.util.function.DoublePredicate`   |

Sentinel handling: `@Condition` needs a default value for `matches` to serve as the sentinel for `@Variant.when`. A package-private or nested sentinel class can serve this purpose. The processor detects this sentinel to distinguish "no condition" from a real condition.

## 2. Modified Annotation Types

### @Variant (modified)

New `when` and `order` attributes added. Existing attributes unchanged.

| Attribute |     Type     |        Default        |                      Status                      |                   Description                    |
| --------- | ------------ | --------------------- | ------------------------------------------------ | ------------------------------------------------ |
| `value`   | `String`     | `""`                  | Existing (default changed from required to `""`) | Variant value for value-based dispatch           |
| `of`      | `Class<?>`   | `void.class`          | Existing (unchanged)                             | Target @Feature interface                        |
| `when`    | `@Condition` | sentinel `@Condition` | **New**                                          | Condition for predicate-based dispatch           |
| `order`   | `int`        | `Integer.MAX_VALUE`   | **New**                                          | Evaluation sequence (ascending, first match wins)|

`order` controls the evaluation sequence for variants with conditions. When no `order` is specified on any variant, the proxy uses map-based O(1) lookup (no performance regression for value-based features). When `order` is present, variants are evaluated as an ordered list; the first match wins.

Backward compatibility: The `value` attribute default changes from required to `""`. For value-based variants, `value` must be non-empty (processor validates). For condition-based variants, `value` is ignored. The processor determines dispatch mode from the `when` attribute.

## 3. Processor-Internal Models (compile-time only)

### ConditionModel (new)

Represents a processed `@Condition` annotation. Internal to the processor, not part of the public API.

|     Field      |     Type      |                       Description                        |
| -------------- | ------------- | -------------------------------------------------------- |
| predicateClass | `TypeMirror`  | Fully qualified type of the predicate implementor        |
| negated        | `boolean`     | `true` if declared via `notMatches`, `false` for `matches` |

### VariantModel (modified)

Extended with optional condition metadata and order.

|       Field        |            Type             |  Status  |                     Description                      |
| ------------------ | --------------------------- | -------- | ---------------------------------------------------- |
| qualifiedClassName | `String` (qualified name)   | Existing | Fully qualified name of the @Variant class           |
| variantValue       | `String`                    | Existing | Variant value from `@Variant("VALUE")`               |
| condition          | `ConditionModel` (nullable) | **New**  | Condition metadata, or null for value-based variants |
| order              | `int`                       | **New**  | Evaluation sequence from `@Variant(order = N)`       |

A variant is condition-based if `condition != null`. A variant is value-based if `condition == null` and `variantValue` is non-empty.

### FeatureModel (modified)

Extended with unified ordered dispatch awareness.

|          Field          |         Type         |  Status  |                     Description                     |
| ----------------------- | -------------------- | -------- | --------------------------------------------------- |
| packageName             | `String`             | Existing | Package of the @Feature interface                   |
| interfaceName           | `String`             | Existing | Simple class name of the @Feature interface         |
| flagKey                 | `String`             | Existing | Value from `@Feature("flag-key")`                   |
| fallbackStrategy        | `FallbackStrategy`   | Existing | REQUIRED, EXCEPTION, or NOOP                        |
| methods                 | `List<MethodModel>`  | Existing | Methods declared on the interface                   |
| variants                | `List<VariantModel>` | Existing | All variant models (value-based or condition-based) |
| defaultVariantClassName | `String` (nullable)  | Existing | Qualified name of @DefaultVariant class             |
| hasOrderedDispatch      | `boolean`            | **New**  | `true` if any variant specifies `order`             |

Note: Exact matches and conditions can coexist on the same `@Feature`. The `order` attribute on `@Variant` determines evaluation sequence. When `hasOrderedDispatch` is `false`, the proxy uses map-based O(1) lookup. When `true`, variants are evaluated as an ordered list.

## 4. Generated Proxy Structure

### Unified Ordered Dispatch

Exact-match variants and condition-based variants can coexist on the same `@Feature`. The proxy generation strategy depends on whether `order` is present:

**No `order` on any variant (value-based only)**: Map-based O(1) lookup, identical to M0. No performance regression.

**`order` present on any variant**: Ordered list evaluation, first match wins. The proxy stores an ordered list of evaluation entries:

|       Field       |                     Type                      |                          Description                           |
| ----------------- | --------------------------------------------- | -------------------------------------------------------------- |
| entries           | Ordered evaluation entry array (final)        | Entries sorted by order (ascending), mixing exact + conditions |
| defaultVariant    | Feature interface instance (nullable, final)  | @DefaultVariant instance, if any                               |
| fallbackStrategy  | `FallbackStrategy` (final)                    | Configured strategy                                            |
| flagProvider      | `FlagProvider` (final, if exact matches exist) | Used for exact-match entries that need flag value               |

Each evaluation entry is either:
- **Exact match**: compares the flag value (from FlagProvider) against a known string
- **Condition match**: evaluates a predicate against the flag value

### Proxy Constructor

Parameters:

- Evaluation entries (sorted by order)
- FlagProvider (if any exact-match variants exist)
- Default variant instance (nullable)
- FallbackStrategy

### Proxy Dispatch Logic (ordered, per method)

1. Obtain the flag value from `FlagProvider.getString(key)`
2. If flag value is absent: delegate to default variant or apply fallback
3. For each entry in order:
   - **Exact match entry**: compare flag value to entry's expected value; if equal, delegate and return
   - **Condition entry**: convert flag value to the feature's type, call `predicate.test(value)`; if true, delegate and return
4. If no entry matched: delegate to default variant or apply fallback

## 5. Generated Metadata

The `{Feature}_FlagZenMetadata` class must support both dispatch strategies. The crafter determines the internal approach. The architecture requires that the metadata SPI continues to support `ServiceLoader` discovery and that the `FeatureDispatcher` can construct proxies through it.

## 6. Exception Changes

### UnmatchedVariantException (modified message)

For features with conditions, the exception message includes predicate information:

- Value-based only: `"No variant matched for flag key 'checkout-flow' with value 'UNKNOWN'. Known variants: [CLASSIC, STREAMLINED]"`
- With conditions: `"No variant matched for flag key 'pricing-tier' with value 'enterprise'. Predicates evaluated: [Enterprise(order=1), Startup(order=2)]"`

No new exception types are introduced.

## 7. Type Summary

### New types

|        Type        |         Package         |          Kind          | Public API |
| ------------------ | ----------------------- | ---------------------- | ---------- |
| `@Condition`       | `com.flagzen`           | Annotation type        | Yes        |
| `ConditionModel`   | `com.flagzen.processor` | Record (processor-internal) | No    |

### Modified types

|            Type             |                             Change                              |
| --------------------------- | --------------------------------------------------------------- |
| `@Variant`                  | New `when` and `order` attributes, `value` default changed to `""` |
| `VariantModel`              | New `condition` field (nullable), new `order` field             |
| `FeatureModel`              | New `hasOrderedDispatch` field                                  |
| `FlagZenProcessor`          | Condition validation, predicate type matching, order-based dispatch |
| `ProxyGenerator`            | Unified ordered dispatch generation path                        |
| `UnmatchedVariantException` | Condition-aware error messages                                  |

### Unchanged types

All other existing types remain unchanged: `@Feature`, `@DefaultVariant`, `FallbackStrategy`, `FeatureDispatcher`, `FlagZen`, `FlagProvider`, `ContextAccessor`, `InMemoryFlagProvider`, `FeatureMetadata`, `MethodModel`.
