# Annotations Reference

Complete reference for all FlagZen annotations.

## `@Feature`

Marks a Java interface as a feature flag dispatch point. The annotation processor generates a proxy class that delegates method calls to the active variant implementation.

### Attributes

| Attribute  |        Type        |   Default   |                                      Description                                      |
| ---------- | ------------------ | ----------- | ------------------------------------------------------------------------------------- |
| `value`    | `String`           | required    | The flag key used to resolve the active variant. Must be a non-empty string.          |
| `type`     | `FeatureType`      | `STRING`    | The type of flag value used to dispatch variants. Must match the variant value types. |
| `fallback` | `FallbackStrategy` | `EXCEPTION` | Strategy when no variant matches the flag value.                                      |

### Constraints

- Must be applied to an interface, not a class
- The interface must be public
- The interface package becomes the package for the generated proxy class
- All `@Variant` implementations must have `of = <this interface class>`
- All variant values must match the declared `type`

### Example

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Feature(value = "max-retries", type = FeatureType.INT)
public interface RetryStrategy {
    int getMaxRetries();
}

@Feature(value = "dark-mode", type = FeatureType.BOOLEAN, fallback = FallbackStrategy.NOOP)
public interface DarkMode {
    String theme();
}
```

## `@Variant`

Maps one or more flag values to a variant implementation class. A feature requires at least one `@Variant` unless a `@DefaultVariant` is defined.

### Attributes

|   Attribute    |    Type     |   Default    |                                           Description                                            |
| -------------- | ----------- | ------------ | ------------------------------------------------------------------------------------------------ |
| `value`        | `String[]`  | `{}`         | String variant value(s) for `STRING`-type features. Auto-wraps scalars to single-element arrays. |
| `intValue`     | `int[]`     | `{}`         | Integer variant value(s) for `INT`-type features. Auto-wraps scalars.                            |
| `longValue`    | `long[]`    | `{}`         | Long variant value(s) for `LONG`-type features. Auto-wraps scalars.                              |
| `doubleValue`  | `CloseTo[]` | `{}`         | Double variant value(s) with approximate matching for `DOUBLE`-type features.                    |
| `booleanValue` | `String`    | `""` (empty) | Boolean variant value: `"true"` or `"false"`. Empty string means not set.                        |
| `of`           | `Class<?>`  | `void.class` | The feature interface this variant belongs to. Required.                                         |

### Constraints

- Must be applied to a class (not an interface)
- The class must implement the interface specified in `of`
- At least one value array must be non-empty, or `booleanValue` must be non-empty
- All value types must match the feature's declared `type`
- Variant values must be unique across all `@Variant` annotations for the same feature
- Cannot be used on abstract classes or inner classes

### Compile-Time Validation

The annotation processor validates:

- Feature interface exists and is properly annotated
- Variant class implements the feature interface
- Value type matches feature type declaration
- No duplicate values across variants for the same feature
- No mismatch between feature type and variant value type

### Examples

**String variant (single value)**

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "classic"; }
}
```

**String variant (multiple values)**

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "classic"; }
}
```

**Integer variant (array)**

```java
@Variant(intValue = {3, 5}, of = RetryStrategy.class)
public class ConservativeRetry implements RetryStrategy {
    @Override
    public int getMaxRetries() { return 3; }
}
```

## `@DefaultVariant`

Marks a variant as the fallback when no flag value matches any `@Variant` annotation. At most one `@DefaultVariant` per feature.

### Attributes

| Attribute |    Type    |   Default    |                           Description                            |
| --------- | ---------- | ------------ | ---------------------------------------------------------------- |
| `of`      | `Class<?>` | `void.class` | The feature interface this default variant belongs to. Required. |

### Constraints

- Must be applied to a class implementing the feature interface
- At most one per feature
- Cannot be used with `@Variant` on the same class
- Only meaningful when the feature's `fallback = FallbackStrategy.EXCEPTION` (default)

### Example

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@DefaultVariant(of = CheckoutFlow.class)
public class SafeCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "safe"; }
}
```

When `checkout-flow` flag is absent or unrecognized, `SafeCheckout` handles the request.

## `@WhenTrue` / `@WhenFalse`

Convenience annotations for `BOOLEAN`-type features. Equivalent to `@Variant(booleanValue = "true")` and `@Variant(booleanValue = "false")` respectively.

### Attributes

| Attribute |    Type    |   Default    |                           Description                            |
| --------- | ---------- | ------------ | ---------------------------------------------------------------- |
| `of`      | `Class<?>` | `void.class` | The boolean feature interface this variant belongs to. Required. |

### Constraints

- Can only be used on variants of `BOOLEAN`-type features
- Feature's `type` attribute must be `FeatureType.BOOLEAN`
- Each boolean feature needs exactly one `@WhenTrue` and optionally one `@WhenFalse`

### Example

```java
@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
public interface DarkMode {
    String theme();
}

@WhenTrue(of = DarkMode.class)
public class DarkTheme implements DarkMode {
    @Override
    public String theme() { return "dark"; }
}

@WhenFalse(of = DarkMode.class)
public class LightTheme implements DarkMode {
    @Override
    public String theme() { return "light"; }
}
```

## `@CloseTo`

Specifies a double variant value with approximate matching. Used within `@Variant(doubleValue = {...})`.

### Attributes

| Attribute |   Type   | Default  |                                        Description                                        |
| --------- | -------- | -------- | ----------------------------------------------------------------------------------------- |
| `value`   | `double` | required | The double value to match against.                                                        |
| `delta`   | `double` | `1e-10`  | The maximum absolute difference for a match. Accepts values like `0.01` for 1% tolerance. |

### Constraints

- Can only be used in `@Variant(doubleValue = {...})`
- Feature type must be `FeatureType.DOUBLE`
- `delta` must be positive

### Example

```java
@Feature(value = "compression-ratio", type = FeatureType.DOUBLE)
public interface CompressionStrategy {
    double getRatio();
}

@Variant(doubleValue = {@CloseTo(value = 0.5, delta = 0.01)}, of = CompressionStrategy.class)
public class MediumCompression implements CompressionStrategy {
    @Override
    public double getRatio() { return 0.5; }
}

@Variant(doubleValue = {@CloseTo(value = 0.9, delta = 0.05)}, of = CompressionStrategy.class)
public class HighCompression implements CompressionStrategy {
    @Override
    public double getRatio() { return 0.9; }
}
```

If the `compression-ratio` flag is `0.49` or `0.51`, it matches the first variant (0.5 ± 0.01). If it is `0.88`, `0.89`, `0.90`, `0.91`, or `0.92`, it matches the second variant (0.9 ± 0.05).

## `FeatureType` Enum

Defines the type of flag value used to dispatch variants.

### Values

|   Value   |                         Description                          |                          Range                          |                  Example Variant                  |
| --------- | ------------------------------------------------------------ | ------------------------------------------------------- | ------------------------------------------------- |
| `STRING`  | Text-based flag values (default).                            | any string                                              | `@Variant(value = "PREMIUM")`                     |
| `INT`     | 32-bit signed integer flag values.                           | -2,147,483,648 to 2,147,483,647                         | `@Variant(intValue = 42)`                         |
| `LONG`    | 64-bit signed integer flag values.                           | -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807 | `@Variant(longValue = 1000000000000L)`            |
| `BOOLEAN` | Boolean flag values (`true` or `false`).                     | `true`, `false`                                         | `@WhenTrue(of = Feature.class)`                   |
| `DOUBLE`  | 64-bit floating-point flag values with approximate matching. | IEEE 754 double range                                   | `@Variant(doubleValue = {@CloseTo(value = 0.5)})` |

## `FallbackStrategy` Enum

Defines the runtime behavior when no variant matches the flag value.

### Values

|         Value         |                Description                 |                                                                             Behavior                                                                              |
| --------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `EXCEPTION` (default) | Throw an exception if no variant matches.  | Calls to the proxy throw `UnmatchedVariantException` if the flag value has no corresponding variant and no `@DefaultVariant` is defined. Fails loudly at runtime. |
| `NOOP`                | Silently do nothing if no variant matches. | Calls to the proxy return default values (null for objects, 0 for primitives, false for booleans). No exception. Used for optional features.                      |

### Usage

```java
// Strict: must have a matching variant or default
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

// Optional: missing variants are silently ignored
@Feature(value = "experimental-ui", fallback = FallbackStrategy.NOOP)
public interface ExperimentalUI {
    void render();
}
```

## Compile-Time Validation Summary

The annotation processor performs these validations at compile time:

|                        Check                         |    Scope    |                       Failure                       |
| ---------------------------------------------------- | ----------- | --------------------------------------------------- |
| `@Feature` applied only to interfaces                | per feature | Error: cannot apply to class                        |
| `@Variant` applied only to classes                   | per variant | Error: cannot apply to interface                    |
| Variant implements feature interface                 | per variant | Error: class does not implement feature             |
| All variant values match feature type                | per variant | Error: type mismatch                                |
| No duplicate variant values                          | per feature | Error: duplicate value                              |
| Feature has at least one variant or default          | per feature | Error: no variants found (unless `@DefaultVariant`) |
| Boolean feature has `@WhenTrue` or `@DefaultVariant` | per feature | Error: boolean feature with no true variant         |
| `CloseTo` only in DOUBLE features                    | per variant | Error: type mismatch                                |

## Generated Code

For each `@Feature` interface, the annotation processor generates:

1. **Proxy class**: `{Feature}_FlagZenProxy` in the same package as the interface. This class is public with a package-private constructor and implements the feature interface.

2. **Metadata class**: `{Feature}_FlagZenMetadata` in the same package. This implements the `FeatureMetadata` SPI and is registered via `META-INF/services/com.flagzen.spi.FeatureMetadata`.

The proxy is discovered and instantiated by `FeatureDispatcher` via the metadata class.

## Best Practices

- **Use short, descriptive flag keys**: `checkout-flow`, `dark-mode`, not `cf`, `dm`, or `experimental_ui_v2_for_testing_only`
- **One feature per interface**: Do not combine multiple concerns in a single `@Feature` interface
- **Consistent variant naming**: Use SCREAMING_SNAKE_CASE for string variant values (e.g., `CLASSIC`, `PREMIUM`, `EXPERIMENTAL`)
- **Prefer `@DefaultVariant` over exceptions**: For optional features, define a sensible default rather than throwing on mismatch
- **Group variant implementations by package**: Keep all variants for a feature in the same package as the feature interface
- **Document variant semantics**: Add Javadoc explaining what each variant value means and when it is used
