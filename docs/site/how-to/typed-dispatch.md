# How to Use Typed Dispatch

Dispatch variants based on non-string flag values: integers, longs, booleans, and doubles.

## Goal

Route method calls to variant implementations selected by numeric or boolean flag values instead of strings.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- Supported types: `INT`, `LONG`, `BOOLEAN`, `DOUBLE`

## Steps

### 1. Define the feature with a type

Specify the flag value type in the `@Feature` annotation:

```java
@Feature(value = "max-retries", type = FeatureType.INT)
public interface RetryStrategy {
    int getMaxRetries();
}
```

### 2. Create variants with typed values

Use the corresponding value field on `@Variant`:

```java
@Variant(intValue = 3, of = RetryStrategy.class)
public class ConservativeRetry implements RetryStrategy {
    @Override
    public int getMaxRetries() { return 3; }
}

@Variant(intValue = 10, of = RetryStrategy.class)
public class AggressiveRetry implements RetryStrategy {
    @Override
    public int getMaxRetries() { return 10; }
}
```

### 3. Provide the flag value as a number

Set the flag in your provider as a string representation of the type:

```java
provider.set("max-retries", "10");  // String is parsed to int
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);
strategy.getMaxRetries(); // 10 -> AggressiveRetry variant
```

### 4. For DOUBLE flags, use `@CloseTo` for approximate matching

Define tolerance when numeric precision may vary:

```java
@Feature(value = "discount-rate", type = FeatureType.DOUBLE)
public interface Discount {
    double rate();
}

@Variant(doubleValue = @CloseTo(value = 0.15, tolerance = 0.01), of = Discount.class)
public class PremiumDiscount implements Discount {
    @Override
    public double rate() { return 0.15; }
}
```

Flags within the tolerance range match the variant:

```java
provider.set("discount-rate", "0.14");  // Within 0.01 of 0.15
Discount d = dispatcher.resolve(Discount.class);
d.rate(); // PremiumDiscount variant (0.14 is close to 0.15)
```

## Result

The proxy dispatches to the correct variant based on numeric comparison, with no string matching logic needed.

## See Also

- [Reference: FeatureType](../reference/annotations.md#featuretype-enum) — all supported types
- [Reference: @CloseTo](../reference/annotations.md#closeto) — double approximate matching
- [How-to: Multi-Value Variants](multi-value-variants.md) — map multiple values to one variant
