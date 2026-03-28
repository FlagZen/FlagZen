# Journey: Multi-Value Variant Mapping

## Persona

**Kenji Nakamura** -- Senior Java developer at an e-commerce company. Maintains a checkout service with 12 feature flags. Several flags have "legacy" values that should map to the same implementation as their modern equivalents (e.g., "CLASSIC" and "LEGACY" both route to the same `ClassicCheckout`).

## Goal

Kenji wants to map multiple flag values to one variant implementation without duplicating annotation boilerplate, so that flag value consolidation happens declaratively at compile time.

## Emotional Arc

- **Start**: Mildly annoyed -- has 4 repeated `@Variant` annotations on the same class, feels like unnecessary noise
- **Middle**: Focused -- learning the array syntax, expects it to "just work" like standard Java annotation arrays
- **End**: Satisfied -- annotation is cleaner, compiler still catches duplicates, no runtime behavior change

## Journey Flow

```
[Trigger]              [Step 1]                [Step 2]                [Step 3]
 Kenji has 4           Changes @Variant to     Builds project          Verifies runtime
 repeated @Variant     array syntax            (compilation)           dispatch works
 on same class                                                         for all values
   |                      |                       |                       |
   v                      v                       v                       v
 Feels: annoyed        Feels: confident         Feels: expectant       Feels: satisfied
 "This is noisy"       "Standard Java syntax"   "Should just work"     "Same behavior,
                                                                        less code"
```

## Step Details

### Step 1: Annotation Change

Kenji replaces repeated annotations with array syntax.

**Before:**

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
@Variant(value = "LEGACY", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

**After:**

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

For int-typed features:

**Before:**

```java
@Variant(intValue = 3, of = PricingTier.class)
@Variant(intValue = 5, of = PricingTier.class)
public class BulkPricing implements PricingTier { ... }
```

**After:**

```java
@Variant(intValue = {3, 5}, of = PricingTier.class)
public class BulkPricing implements PricingTier { ... }
```

### Step 2: Compilation

Annotation processor:

1. Reads array values from `@Variant`
2. Registers implementation class under each value
3. Detects duplicates across all arrays and repeated annotations
4. Generates proxy with all values mapped to the same supplier

**Error case -- duplicate across classes:**

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }

@Variant(value = {"MODERN", "LEGACY"}, of = CheckoutFlow.class)  // ERROR: "LEGACY" duplicated
public class ModernCheckout implements CheckoutFlow { ... }
```

**Compiler output:**

```
error: Duplicate @Variant("LEGACY") for feature "checkout-flow". Found on: ClassicCheckout and ModernCheckout
```

### Step 3: Runtime Verification

No runtime change. Generated proxy has a `Map<String, Supplier<CheckoutFlow>>` with entries for "CLASSIC", "LEGACY", "MODERN" -- each pointing to the correct implementation. Dispatch is identical to before.

## Composability: Array + Repeated Annotations

Both syntaxes work together:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
@Variant(value = "RETRO", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

Registers under "CLASSIC", "LEGACY", and "RETRO". Duplicate detection spans all values from all annotations on the class.

## @CloseTo Overlapping Range Detection

### Step 4: Compile-Time @CloseTo Range Validation

When Kenji defines DOUBLE-typed features with `@CloseTo` variants, the processor computes the effective range `[value - delta, value + delta]` for each entry and checks for overlaps.

**Overlap detected (inter-variant):**

```java
@Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.05), of = DiscountRate.class)
public class SmallDiscount implements DiscountRate { ... }
// range: [0.05, 0.15]

@Variant(doubleValue = @CloseTo(value = 0.12, delta = 0.05), of = DiscountRate.class)
public class MediumDiscount implements DiscountRate { ... }
// range: [0.07, 0.17]
```

**Compiler output:**

```
error: Overlapping @CloseTo ranges for feature "discount-rate".
  SmallDiscount: @CloseTo(value=0.1, delta=0.05) -> range [0.05, 0.15]
  MediumDiscount: @CloseTo(value=0.12, delta=0.05) -> range [0.07, 0.17]
  Suggestion: reduce delta to increase precision, or merge into a single variant.
```

**Overlap detected (intra-variant, M13 multi-value array):**

```java
@Variant(doubleValue = {
    @CloseTo(value = 0.1, delta = 0.05),
    @CloseTo(value = 0.12, delta = 0.05)  // overlaps with first entry!
}, of = DiscountRate.class)
public class SmallDiscount implements DiscountRate { ... }
```

**Compiler output:**

```
error: Overlapping @CloseTo ranges within variant SmallDiscount for feature "discount-rate".
  @CloseTo(value=0.1, delta=0.05) -> range [0.05, 0.15]
  @CloseTo(value=0.12, delta=0.05) -> range [0.07, 0.17]
  Suggestion: reduce delta to increase precision, or remove the redundant entry.
```

**No overlap (well-separated ranges):**

```java
@Variant(doubleValue = {@CloseTo(0.1), @CloseTo(0.5)}, of = DiscountRate.class)
public class SmallDiscount implements DiscountRate { ... }
// ranges are far apart -- compilation succeeds
```

### Emotional Arc (Step 4)

```
[Trigger]              [Step 4]
 Kenji defines          Processor computes
 @CloseTo variants      ranges, detects overlap
 with close values
   |                      |
   v                      v
 Feels: unaware        Feels: relieved
 "These look fine"     "Compiler caught
                        ambiguous dispatch"
```
