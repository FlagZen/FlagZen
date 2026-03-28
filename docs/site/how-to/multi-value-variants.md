# How to Map Multiple Values to One Variant

Activate a single variant implementation from multiple flag values.

## Goal

Route different flag values (e.g., `"CLASSIC"` and `"LEGACY"`) to the same variant without duplicating code.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`
- Understanding of variant matching

## Steps

### 1. Define multiple values in a single variant

Use array syntax in the `@Variant` value field:

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "classic"; }
}

@Variant(value = "STREAMLINED", of = CheckoutFlow.class)
public class StreamlinedCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "streamlined"; }
}
```

Now both `"CLASSIC"` and `"LEGACY"` flag values activate the `ClassicCheckout` variant.

### 2. For typed flags, use type-specific array fields

Provide arrays for numeric types:

```java
@Feature(value = "retry-level", type = FeatureType.INT)
public interface RetryStrategy {
    int getMaxRetries();
}

@Variant(intValue = {3, 5, 7}, of = RetryStrategy.class)
public class ConservativeRetry implements RetryStrategy {
    @Override
    public int getMaxRetries() { return 3; }
}
```

Values `3`, `5`, and `7` all dispatch to `ConservativeRetry`.

### 3. Compose with `@Repeatable` for cleaner syntax (optional)

If you need complex variant definitions, repeat the `@Variant` annotation:

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
@Variant(value = "LEGACY", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

Both forms are equivalent. Use arrays for simple cases; use `@Repeatable` when variant logic is more complex.

### 4. Test with multiple values

```java
provider.set("checkout-flow", "CLASSIC");
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
flow.execute(); // ClassicCheckout

provider.set("checkout-flow", "LEGACY");
flow = dispatcher.resolve(CheckoutFlow.class);
flow.execute(); // Still ClassicCheckout
```

## Result

A single variant implementation handles multiple flag values, reducing code duplication and keeping variant logic centralized.

## See Also

- [Reference: @Variant](../reference/annotations.md#variant) — all value fields
- [How-to: Typed Dispatch](typed-dispatch.md) — numeric flag values
- [How-to: Boolean Variants](../reference/annotations.md#whentrue-whenfalse) — convenience for booleans
