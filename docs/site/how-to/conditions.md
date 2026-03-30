# How to Use Condition Predicates

Dispatch variants based on flag value patterns, ranges, or thresholds instead of exact matches.

## Goal

Use `@Condition` predicates to activate variants when the flag value meets a condition (e.g., "retry count above 7") rather than matching a specific value (e.g., "retry count equals 3").

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- Java 17+ (JDK functional interfaces: `Predicate<String>`, `IntPredicate`, etc.)

## When to use conditions vs. exact matches

| Approach | Use when | Example |
| --- | --- | --- |
| Exact match (`@Variant(value = "X")`) | You know all possible flag values upfront | `"CLASSIC"`, `"PREMIUM"`, `"STREAMLINED"` |
| Condition (`@Condition(matches = ...)`) | You need ranges, patterns, or open-ended matching | "retries >= 7", "plan starts with enterprise-" |
| Both (unified dispatch) | Some values are known, others need predicates | Exact `"FREE"` + predicate for enterprise tiers |

## Steps

### 1. Define a feature with a flag type

```java
@Feature(value = "max-retries", type = FeatureType.INT)
public interface RetryStrategy {
    void execute(Request req);
}
```

### 2. Write a predicate

Predicates are plain classes implementing JDK functional interfaces. No FlagZen imports needed:

```java
public class HighRetryRange implements IntPredicate {
    @Override
    public boolean test(int value) {
        return value >= 7;
    }
}
```

The predicate type must match the feature type:

| `@Feature(type = ...)` | Predicate interface |
| --- | --- |
| `STRING` (default) | `java.util.function.Predicate<String>` |
| `INT` | `java.util.function.IntPredicate` |
| `LONG` | `java.util.function.LongPredicate` |
| `DOUBLE` | `java.util.function.DoublePredicate` |

### 3. Bind the predicate to a variant

Use `@Variant(when = @Condition(matches = ...), order = N)`:

```java
@Variant(intValue = 3, of = RetryStrategy.class, order = 1)
public class ConservativeRetry implements RetryStrategy {
    @Override
    public void execute(Request req) { /* retry up to 3 times */ }
}

@Variant(when = @Condition(matches = HighRetryRange.class), of = RetryStrategy.class, order = 2)
public class AggressiveRetry implements RetryStrategy {
    @Override
    public void execute(Request req) { /* retry aggressively */ }
}

@DefaultVariant(of = RetryStrategy.class)
public class StandardRetry implements RetryStrategy {
    @Override
    public void execute(Request req) { /* standard retry logic */ }
}
```

### 4. Understand dispatch order

When `order` is present, variants are evaluated in ascending order. First match wins:

1. Flag value `3` → exact match → `ConservativeRetry` (order 1)
2. Flag value `10` → `HighRetryRange.test(10)` returns true → `AggressiveRetry` (order 2)
3. Flag value `5` → no exact match, `HighRetryRange.test(5)` returns false → `StandardRetry` (default)

### 5. Use negation with `notMatches` (optional)

Activate a variant when a predicate does NOT match:

```java
public class LowValue implements IntPredicate {
    @Override
    public boolean test(int value) {
        return value < 10;
    }
}

@Variant(when = @Condition(notMatches = LowValue.class), of = PricingStrategy.class, order = 1)
public class PremiumPricing implements PricingStrategy { ... }
```

`PremiumPricing` activates when the flag value is NOT low (i.e., >= 10).

`matches` and `notMatches` are mutually exclusive on the same `@Condition`.

### 6. Mix exact matches and conditions (optional)

Exact-match and condition-based variants coexist on the same feature. Use `order` to control evaluation sequence:

```java
@Feature(value = "pricing-tier", type = FeatureType.INT)
public interface PricingStrategy { double calculate(Order order); }

@Variant(intValue = 0, of = PricingStrategy.class, order = 1)
public class FreeTier implements PricingStrategy { ... }

@Variant(when = @Condition(matches = EnterpriseTier.class), of = PricingStrategy.class, order = 2)
public class EnterprisePricing implements PricingStrategy { ... }

@DefaultVariant(of = PricingStrategy.class)
public class StandardPricing implements PricingStrategy { ... }
```

Flag value `0` → exact match (order 1). Flag value `1000` → `EnterpriseTier.test(1000)` (order 2). Anything else → `StandardPricing`.

### 7. Resolve at runtime

No changes to runtime code. Condition-based features resolve the same way as value-based features:

```java
FlagProvider provider = new InMemoryFlagProvider();
provider.set("max-retries", "10");

FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);
strategy.execute(request); // Dispatches to AggressiveRetry
```

## String predicate example

```java
@Feature("plan-id")
public interface PlanFeature {
    String dashboard();
}

public class EnterprisePrefix implements Predicate<String> {
    @Override
    public boolean test(String value) {
        return value != null && value.startsWith("enterprise-");
    }
}

@Variant(value = "free", of = PlanFeature.class, order = 1)
public class FreePlan implements PlanFeature { ... }

@Variant(when = @Condition(matches = EnterprisePrefix.class), of = PlanFeature.class, order = 2)
public class EnterprisePlan implements PlanFeature { ... }

@DefaultVariant(of = PlanFeature.class)
public class StandardPlan implements PlanFeature { ... }
```

Flag value `"free"` → `FreePlan`. Flag value `"enterprise-gold"` → `EnterprisePlan`. Flag value `"basic"` → `StandardPlan`.

## Predicate requirements

- Must have a public no-arg constructor
- Must not be abstract
- Must be thread-safe if used in multi-threaded applications
- Instantiated once at proxy construction time and reused for every dispatch call

## Performance

Features without `order` use O(1) map-based lookup (no regression from M0). Features with `order` use linear evaluation (typically 2-5 entries). Predicates are instantiated once and reused.

## Result

Condition predicates give you flexible, type-safe dispatch beyond exact value matching. The generated proxy evaluates predicates at runtime with zero reflection.

## See Also

- [Reference: Annotations](../reference/annotations.md) -- `@Condition` attribute details
- [How-to: Typed Dispatch](typed-dispatch.md) -- INT, LONG, BOOLEAN, DOUBLE features
- [Explanation: Architecture](../explanation/architecture.md) -- how ordered dispatch works internally
