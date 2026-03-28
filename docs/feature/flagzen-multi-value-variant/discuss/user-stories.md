<!-- markdownlint-disable MD024 -->

# User Stories: flagzen-multi-value-variant

## US-01: String Array Multi-Value Variant Mapping

### Problem

Kenji Nakamura is a senior Java developer maintaining a checkout service with 12 feature flags. He finds it tedious to repeat `@Variant` annotations when multiple string flag values (e.g., "CLASSIC" and "LEGACY") should route to the same implementation class. The repeated annotations clutter the code and obscure intent.

### Who

- Java developer | Maintaining production feature flags | Wants to reduce annotation boilerplate for value consolidation

### Solution

Change `@Variant` annotation's `value()` element from `String` to `String[]`. Single-value usage continues to work unchanged. The annotation processor iterates array elements and creates one `VariantModel` per element, all mapping to the same implementation class.

### Domain Examples

#### 1: Happy Path -- Kenji consolidates checkout flow variants

Kenji has `ClassicCheckout` that should activate for both "CLASSIC" and "LEGACY" flag values. He writes `@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)`. The project compiles successfully and the generated proxy maps both values to `ClassicCheckout`.

#### 2: Backward Compatibility -- Priya's existing single-value annotation

Priya Sharma has `@Variant(value = "MODERN", of = CheckoutFlow.class)` on her `ModernCheckout` class. After the annotation schema change, her code compiles without modification. The single string auto-wraps to a one-element array.

#### 3: Error -- Empty string in array

Kenji accidentally writes `@Variant(value = {"CLASSIC", ""}, of = CheckoutFlow.class)`. The processor detects the empty string and reports a compile error: empty variant values are not permitted in arrays.

### UAT Scenarios (BDD)

#### Scenario: Multiple string values map to one implementation

```gherkin
Given Kenji defines a @Feature interface "CheckoutFlow" with flag key "checkout-flow"
And Kenji annotates ClassicCheckout with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And Kenji annotates ModernCheckout with @Variant(value = "MODERN", of = CheckoutFlow.class)
When the project compiles
Then compilation succeeds
And the generated proxy maps "CLASSIC" to ClassicCheckout
And the generated proxy maps "LEGACY" to ClassicCheckout
And the generated proxy maps "MODERN" to ModernCheckout
```

#### Scenario: Single string value backward compatibility

```gherkin
Given Priya has an existing @Variant(value = "MODERN", of = CheckoutFlow.class) on ModernCheckout
When the project compiles after the annotation schema change
Then compilation succeeds without any source changes
And the generated proxy maps "MODERN" to ModernCheckout
```

#### Scenario: Runtime dispatch for multi-value string

```gherkin
Given ClassicCheckout is mapped to values "CLASSIC" and "LEGACY"
And ModernCheckout is mapped to value "MODERN"
When the flag provider returns "LEGACY" for key "checkout-flow"
Then the FeatureDispatcher dispatches to ClassicCheckout
```

#### Scenario: Empty string in array rejected

```gherkin
Given Kenji annotates ClassicCheckout with @Variant(value = {"CLASSIC", ""}, of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error indicating empty variant values are not permitted
```

### Acceptance Criteria

- [ ] `@Variant` annotation `value()` element accepts `String[]`
- [ ] Single-value `@Variant(value = "X")` compiles without changes (backward compatible)
- [ ] Each array element produces a separate entry in the generated proxy's variant map
- [ ] Empty strings in the array are rejected at compile time
- [ ] Runtime dispatch works for all values in the array

### Outcome KPIs

- **Who**: Java developers using `@Variant` with string values
- **Does what**: Use array syntax instead of repeated annotations when mapping multiple values
- **By how much**: Eliminate 100% of repeated `@Variant` annotations for same-class mappings
- **Measured by**: Existing test suite passes + new multi-value tests pass
- **Baseline**: Requires N repeated `@Variant` annotations for N values

### Technical Notes

- Changing `String value()` to `String[] value()` with `default ""` needs adjustment -- default for array must be `{}` or `{""}`. Recommend `default ""` remains valid if processor treats `{""}` as "no value specified" (matching current sentinel behavior).
- Binary incompatibility: acceptable pre-1.0. Document in changelog.
- `VariantModel` remains unchanged -- processor creates one instance per array element.

---

## US-02: Int Array Multi-Value Variant Mapping

### Problem

Kenji Nakamura also maintains an integer-typed feature `PricingTier` where pricing levels 3 and 5 both use the same `BulkPricing` implementation. Currently he needs two `@Variant(intValue = 3)` and `@Variant(intValue = 5)` annotations on the same class.

### Who

- Java developer | Using integer-typed features | Wants array syntax for int multi-value

### Solution

Change `@Variant` annotation's `intValue()` element from `int` to `int[]`. Processor iterates array and creates one `VariantModel` per element. Sentinel value `Integer.MIN_VALUE` becomes the empty-array default.

### Domain Examples

#### 1: Happy Path -- Kenji consolidates pricing tiers

Kenji writes `@Variant(intValue = {3, 5}, of = PricingTier.class)` on `BulkPricing`. The project compiles and the proxy maps both int 3 and int 5 to `BulkPricing`.

#### 2: Single int value backward compatibility

Existing `@Variant(intValue = 42, of = PricingTier.class)` continues to compile. The single int auto-wraps to a one-element array.

#### 3: Edge -- Integer.MIN_VALUE in array

Kenji writes `@Variant(intValue = {3, -2147483648}, of = PricingTier.class)`. The processor must handle this: `Integer.MIN_VALUE` was previously the sentinel for "not set". In array context, an empty array `{}` is the sentinel instead, so `Integer.MIN_VALUE` becomes a valid value.

### UAT Scenarios (BDD)

#### Scenario: Multiple int values map to one implementation

```gherkin
Given Kenji defines a @Feature interface "PricingTier" with flag key "pricing-tier" and type INT
And Kenji annotates BulkPricing with @Variant(intValue = {3, 5}, of = PricingTier.class)
And Kenji annotates StandardPricing with @Variant(intValue = 1, of = PricingTier.class)
When the project compiles
Then compilation succeeds
And the generated proxy maps int 3 to BulkPricing
And the generated proxy maps int 5 to BulkPricing
And the generated proxy maps int 1 to StandardPricing
```

#### Scenario: Single int value backward compatibility

```gherkin
Given Priya has an existing @Variant(intValue = 42, of = PricingTier.class) on StandardPricing
When the project compiles after the annotation schema change
Then compilation succeeds without any source changes
```

#### Scenario: Runtime dispatch for multi-value int

```gherkin
Given BulkPricing is mapped to int values 3 and 5
When the flag provider returns 5 for key "pricing-tier"
Then the FeatureDispatcher dispatches to BulkPricing
```

### Acceptance Criteria

- [ ] `@Variant` annotation `intValue()` element accepts `int[]`
- [ ] Single-value `@Variant(intValue = 42)` compiles without changes
- [ ] Each array element produces a separate entry in the generated proxy's variant map
- [ ] `Integer.MIN_VALUE` is a valid value in arrays (no longer sentinel in array context)
- [ ] Runtime dispatch works for all int values in the array

### Outcome KPIs

- **Who**: Java developers using `@Variant` with int values
- **Does what**: Use array syntax for int multi-value instead of repeated annotations
- **By how much**: Same elimination of repeated annotations as US-01
- **Measured by**: Existing int-typed tests pass + new multi-value int tests pass
- **Baseline**: Requires N repeated `@Variant` annotations for N int values

### Technical Notes

- `int intValue() default Integer.MIN_VALUE` changes to `int[] intValue() default {}`. The "not set" check changes from `== Integer.MIN_VALUE` to `length == 0`.
- This means `Integer.MIN_VALUE` is no longer reserved -- it becomes a valid variant value. Pre-1.0, acceptable.
- Processor's `hasTypeMismatch()` must handle array types.

---

## US-03: Compile-Time Duplicate Detection Across Multi-Value Arrays

### Problem

Kenji Nakamura needs confidence that if two different implementation classes accidentally claim the same flag value -- even when values are spread across arrays -- the compiler catches it immediately. Without cross-array duplicate detection, a runtime `IllegalStateException` would be the first signal, which is too late.

### Who

- Java developer | Defining multiple variant implementations | Needs compile-time safety for value uniqueness

### Solution

The existing `hasDuplicateVariantValues()` method already groups by `variantKeyLiteral()`. Since multi-value arrays produce multiple `VariantModel` instances (one per element), the existing duplicate detection logic works across arrays without modification. However, intra-array duplicates (same value twice in one array) must also be detected.

### Domain Examples

#### 1: Duplicate across classes

`ClassicCheckout` maps `{"CLASSIC", "LEGACY"}` and `RetroCheckout` maps `{"RETRO", "LEGACY"}`. The compiler reports: `Duplicate @Variant("LEGACY") for feature "checkout-flow". Found on: ClassicCheckout and RetroCheckout`.

#### 2: Duplicate within same array

`ClassicCheckout` maps `{"CLASSIC", "CLASSIC"}`. The compiler reports the duplicate within the same class.

#### 3: Duplicate between array and repeated annotation on same class

`ClassicCheckout` has `@Variant(value = {"CLASSIC", "LEGACY"})` and `@Variant(value = "LEGACY")`. The compiler detects "LEGACY" appears twice for the same class.

#### 4: Int duplicate across classes

`BulkPricing` maps `{3, 5}` and `SpecialPricing` maps `{5, 7}`. The compiler reports: `Duplicate @Variant("5") for feature "pricing-tier". Found on: BulkPricing and SpecialPricing`.

### UAT Scenarios (BDD)

#### Scenario: Duplicate string value across classes

```gherkin
Given ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And RetroCheckout is annotated with @Variant(value = {"RETRO", "LEGACY"}, of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error "Duplicate @Variant(\"LEGACY\") for feature \"checkout-flow\". Found on: ClassicCheckout and RetroCheckout"
```

#### Scenario: Duplicate string value within same array

```gherkin
Given ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "CLASSIC"}, of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error containing "Duplicate @Variant(\"CLASSIC\")"
```

#### Scenario: Duplicate between array and repeated annotation

```gherkin
Given ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ClassicCheckout is also annotated with @Variant(value = "LEGACY", of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error containing "Duplicate @Variant(\"LEGACY\")"
```

#### Scenario: Duplicate int value across classes

```gherkin
Given BulkPricing is annotated with @Variant(intValue = {3, 5}, of = PricingTier.class)
And SpecialPricing is annotated with @Variant(intValue = {5, 7}, of = PricingTier.class)
When the project compiles
Then compilation fails with error containing "Duplicate @Variant(\"5\") for feature \"pricing-tier\""
```

#### Scenario: No false positive -- different values across classes

```gherkin
Given ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ModernCheckout is annotated with @Variant(value = {"MODERN", "CURRENT"}, of = CheckoutFlow.class)
When the project compiles
Then compilation succeeds
```

### Acceptance Criteria

- [ ] Duplicate values across different classes produce compile error
- [ ] Duplicate values within the same array produce compile error
- [ ] Duplicate values between array syntax and repeated annotation produce compile error
- [ ] Error message names both classes involved in the duplicate
- [ ] Non-duplicate arrays across classes compile successfully
- [ ] Duplicate detection works for all types: string, int, long

### Outcome KPIs

- **Who**: Java developers defining variant implementations
- **Does what**: Receive compile-time error for duplicate variant values across multi-value arrays
- **By how much**: 100% of duplicates caught at compile time (zero runtime surprises)
- **Measured by**: Negative compilation tests covering all duplicate scenarios
- **Baseline**: Existing duplicate detection covers single-value only

### Technical Notes

- Existing `hasDuplicateVariantValues()` groups by `variantKeyLiteral()` and counts occurrences. With multi-value arrays producing multiple `VariantModel` entries, the grouping logic catches cross-array duplicates automatically.
- Intra-array duplicates (same value repeated in one `String[]`) need explicit detection in the array expansion loop.
- Same-class duplicates (from array + repeated annotation) are a valid error scenario.

---

## US-04: Long Array Multi-Value Variant Mapping

### Problem

Kenji Nakamura uses long-typed features for rate limits where multiple threshold values should route to the same implementation. He needs array syntax for `longValue` matching the pattern established for string and int.

### Who

- Java developer | Using long-typed features | Wants consistent multi-value syntax across all numeric types

### Solution

Change `@Variant` annotation's `longValue()` element from `long` to `long[]`. Processor iterates array and creates one `VariantModel` per element. Empty array `{}` replaces `Long.MIN_VALUE` as the sentinel for "not set".

### Domain Examples

#### 1: Happy Path -- Kenji consolidates rate limit thresholds

Kenji writes `@Variant(longValue = {1000L, 2000L}, of = RateLimit.class)` on `ThrottledRate`. The proxy maps both long 1000 and long 2000 to `ThrottledRate`.

#### 2: Single long value backward compatibility

Existing `@Variant(longValue = 999999L)` compiles without changes.

#### 3: Duplicate long across classes

`ThrottledRate` maps `{1000L, 2000L}` and `LimitedRate` maps `{2000L, 3000L}`. Compiler reports duplicate for value 2000.

### UAT Scenarios (BDD)

#### Scenario: Multiple long values map to one implementation

```gherkin
Given Kenji defines a @Feature interface "RateLimit" with flag key "rate-limit" and type LONG
And Kenji annotates ThrottledRate with @Variant(longValue = {1000L, 2000L}, of = RateLimit.class)
And Kenji annotates UnlimitedRate with @Variant(longValue = 999999L, of = RateLimit.class)
When the project compiles
Then compilation succeeds
And the generated proxy maps long 1000 to ThrottledRate
And the generated proxy maps long 2000 to ThrottledRate
```

#### Scenario: Single long value backward compatibility

```gherkin
Given Priya has an existing @Variant(longValue = 999999L, of = RateLimit.class) on UnlimitedRate
When the project compiles after the annotation schema change
Then compilation succeeds without any source changes
```

#### Scenario: Duplicate long value across classes

```gherkin
Given ThrottledRate is annotated with @Variant(longValue = {1000L, 2000L}, of = RateLimit.class)
And LimitedRate is annotated with @Variant(longValue = {2000L, 3000L}, of = RateLimit.class)
When the project compiles
Then compilation fails with error containing "Duplicate @Variant(\"2000\") for feature \"rate-limit\""
```

### Acceptance Criteria

- [ ] `@Variant` annotation `longValue()` element accepts `long[]`
- [ ] Single-value `@Variant(longValue = 999999L)` compiles without changes
- [ ] Each array element produces a separate entry in the generated proxy's variant map
- [ ] `Long.MIN_VALUE` is a valid value in arrays (no longer sentinel)
- [ ] Duplicate detection works for long arrays

### Outcome KPIs

- **Who**: Java developers using `@Variant` with long values
- **Does what**: Use array syntax for long multi-value
- **By how much**: Consistent syntax across all numeric types
- **Measured by**: Long multi-value tests pass
- **Baseline**: Requires repeated annotations for multiple long values

### Technical Notes

- `long longValue() default Long.MIN_VALUE` changes to `long[] longValue() default {}`.
- Same pattern as US-02 (int). Implementation is mechanical once int pattern is established.

---

## US-05: Array Values Compose with Repeated @Variant Annotations

### Problem

Kenji Nakamura wants to use both array syntax and repeated `@Variant` annotations on the same class. This is the natural expectation: if `@Variant` is `@Repeatable`, and `value()` accepts arrays, both should work together without surprises.

### Who

- Java developer | Using both multi-value arrays and repeated annotations | Expects composability

### Solution

The processor already handles repeated annotations via the `@Variants` container. When expanding array values, the processor must iterate all `@Variant` annotations on a class (from both direct and container), expand all arrays, and aggregate all values. Duplicate detection spans the full aggregated set.

### Domain Examples

#### 1: Happy Path -- Array plus single repeated annotation

Kenji writes:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
@Variant(value = "RETRO", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

All three values ("CLASSIC", "LEGACY", "RETRO") map to `ClassicCheckout`.

#### 2: Two arrays on same class

Kenji writes:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
@Variant(value = {"RETRO", "VINTAGE"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

All four values map to `ClassicCheckout`.

#### 3: Error -- Duplicate between array and repeated annotation

Kenji accidentally writes:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
@Variant(value = "LEGACY", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

Compiler catches "LEGACY" as duplicate.

### UAT Scenarios (BDD)

#### Scenario: Array values compose with single repeated annotation

```gherkin
Given ClassicCheckout has @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ClassicCheckout has @Variant(value = "RETRO", of = CheckoutFlow.class)
When the project compiles
Then compilation succeeds
And the generated proxy maps "CLASSIC" to ClassicCheckout
And the generated proxy maps "LEGACY" to ClassicCheckout
And the generated proxy maps "RETRO" to ClassicCheckout
```

#### Scenario: Two arrays on same class compose

```gherkin
Given ClassicCheckout has @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ClassicCheckout has @Variant(value = {"RETRO", "VINTAGE"}, of = CheckoutFlow.class)
When the project compiles
Then compilation succeeds
And the generated proxy maps all four values to ClassicCheckout
```

#### Scenario: Duplicate between array and repeated annotation detected

```gherkin
Given ClassicCheckout has @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ClassicCheckout has @Variant(value = "LEGACY", of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error containing "Duplicate @Variant(\"LEGACY\")"
```

### Acceptance Criteria

- [ ] Array values from multiple `@Variant` annotations on same class are all registered
- [ ] Duplicate detection spans values from all annotations on a class
- [ ] Both array and single-value annotations on same class work together
- [ ] Works for all types (string, int, long)

### Outcome KPIs

- **Who**: Java developers using @Repeatable @Variant
- **Does what**: Combine array syntax with repeated annotations without surprises
- **By how much**: Zero unexpected behavior when mixing syntaxes
- **Measured by**: Composability tests covering mixed syntax combinations
- **Baseline**: Array syntax and repeated annotations not yet tested in combination

### Technical Notes

- The `@Variants` container (for `@Repeatable`) already aggregates multiple `@Variant` annotations. Array expansion happens inside `processVariantAnnotation()`, which is called for each annotation in the container. No new aggregation logic needed -- just array iteration within the existing loop.

---

## US-06: Enum Validation and REQUIRED Fallback with Multi-Value Coverage

### Problem

Kenji Nakamura uses inner `Variant` enums and `FallbackStrategy.REQUIRED` on some features. With multi-value arrays, the processor must count array elements toward enum coverage. If `@Variant(value = {"CLASSIC", "LEGACY"})` covers both enum values, the REQUIRED check should pass.

### Who

- Java developer | Using REQUIRED fallback with inner Variant enums | Expects multi-value to count toward coverage

### Solution

When validating array values against the inner `Variant` enum, each array element is validated individually. When checking REQUIRED coverage, each array element counts as covering its corresponding enum value. The existing coverage check works because `VariantModel` instances (one per array element) already carry the variant value.

### Domain Examples

#### 1: Happy Path -- Multi-value covers all enum values

```java
@Feature(value = "checkout-flow", fallback = FallbackStrategy.REQUIRED)
interface CheckoutFlow {
    enum Variant { CLASSIC, LEGACY, MODERN }
    // ...
}

@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
class ClassicCheckout implements CheckoutFlow { ... }

@Variant(value = "MODERN", of = CheckoutFlow.class)
class ModernCheckout implements CheckoutFlow { ... }
```

All three enum values covered. Compiles successfully.

#### 2: Invalid enum value in array

`@Variant(value = {"CLASSIC", "INVALID"})` -- "INVALID" not in enum. Compiler reports error for the invalid element.

#### 3: Incomplete coverage despite multi-value

`@Variant(value = {"CLASSIC", "LEGACY"})` covers two of three enum values. No implementation for "MODERN". REQUIRED fallback reports missing coverage.

### UAT Scenarios (BDD)

#### Scenario: Multi-value satisfies REQUIRED coverage

```gherkin
Given CheckoutFlow has fallback REQUIRED and inner enum Variant(CLASSIC, LEGACY, MODERN)
And ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
And ModernCheckout is annotated with @Variant(value = "MODERN", of = CheckoutFlow.class)
When the project compiles
Then compilation succeeds
```

#### Scenario: Invalid enum value in array rejected

```gherkin
Given CheckoutFlow has inner enum Variant(CLASSIC, MODERN)
And ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "INVALID"}, of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error containing "does not match any value in CheckoutFlow.Variant"
```

#### Scenario: Incomplete coverage with multi-value

```gherkin
Given CheckoutFlow has fallback REQUIRED and inner enum Variant(CLASSIC, LEGACY, MODERN)
And ClassicCheckout is annotated with @Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
When the project compiles
Then compilation fails with error containing "variant MODERN has no implementation"
```

### Acceptance Criteria

- [ ] Each array element validated against inner `Variant` enum individually
- [ ] Multi-value array elements count toward REQUIRED coverage
- [ ] Invalid enum values in arrays produce compile error per invalid element
- [ ] Incomplete coverage with multi-value still reports missing variants

### Outcome KPIs

- **Who**: Java developers using REQUIRED fallback with inner Variant enums
- **Does what**: Multi-value arrays participate correctly in coverage checks
- **By how much**: Zero false positives or false negatives in REQUIRED validation
- **Measured by**: Coverage validation tests with multi-value arrays
- **Baseline**: Coverage checks only count single-value annotations

### Technical Notes

- `validateVariantValuesAgainstEnum()` iterates `VariantModel` list. Since multi-value arrays produce multiple `VariantModel` entries, each element is validated individually -- existing logic works.
- `hasIncompleteVariantCoverage()` collects covered values from `VariantModel.variantValue()`. Multi-value array expansion means all elements are in the coverage set -- existing logic works.
- Key insight: most of the existing validation logic works unchanged because the multi-value expansion happens upstream (in `collectVariants()`), and downstream validation operates on the flat `List<VariantModel>`.

---

## US-07: Compile-Time Detection of Overlapping @CloseTo Ranges

### Problem

Kenji Nakamura defines a DOUBLE-typed feature `DiscountRate` where variants are dispatched by proximity (`@CloseTo`). He accidentally creates two variants with ranges that overlap -- `@CloseTo(value = 0.1, delta = 0.05)` on `SmallDiscount` and `@CloseTo(value = 0.12, delta = 0.05)` on `MediumDiscount`. At runtime, a flag value of 0.11 falls within both ranges, causing ambiguous dispatch. Kenji only discovers this when QA reports inconsistent behavior in production. He needs the compiler to catch overlapping `@CloseTo` ranges before the code ever runs.

### Who

- Java developer | Defining DOUBLE-typed features with `@CloseTo` variants | Needs compile-time safety against ambiguous range overlap

### Solution

The annotation processor computes the effective range `[value - delta, value + delta]` for each `@CloseTo` annotation on variants of the same DOUBLE-typed feature. Two ranges overlap when `|value1 - value2| < delta1 + delta2`. The processor emits a compile error identifying the overlapping variants and their ranges, with a suggestion to reduce delta or merge variants. Two distinct checks apply:

1. **Inter-variant overlap**: two different implementation classes have `@CloseTo` ranges that overlap for the same feature.
2. **Intra-variant overlap (M13)**: a single variant class has multiple `@CloseTo` entries in its `doubleValue` array whose ranges overlap, which is nonsensical.

### Domain Examples

#### 1: Inter-variant overlap -- Kenji's discount rate ranges collide

Kenji defines `SmallDiscount` with `@Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.05), of = DiscountRate.class)` (range [0.05, 0.15]) and `MediumDiscount` with `@Variant(doubleValue = @CloseTo(value = 0.12, delta = 0.05), of = DiscountRate.class)` (range [0.07, 0.17]). The ranges overlap at [0.07, 0.15]. The compiler reports:

```
error: Overlapping @CloseTo ranges for feature "discount-rate".
  SmallDiscount: @CloseTo(value=0.1, delta=0.05) -> range [0.05, 0.15]
  MediumDiscount: @CloseTo(value=0.12, delta=0.05) -> range [0.07, 0.17]
  Suggestion: reduce delta to increase precision, or merge into a single variant.
```

#### 2: Non-overlapping ranges -- Kenji's well-separated discount tiers

Kenji defines `SmallDiscount` with `@CloseTo(value = 0.1, delta = 0.01)` (range [0.09, 0.11]) and `LargeDiscount` with `@CloseTo(value = 0.5, delta = 0.01)` (range [0.49, 0.51]). The ranges are well separated. Compilation succeeds.

#### 3: Intra-variant overlap -- Priya's nonsensical array entries

Priya Sharma writes `@Variant(doubleValue = {@CloseTo(value = 0.1, delta = 0.05), @CloseTo(value = 0.12, delta = 0.05)}, of = DiscountRate.class)` on `SmallDiscount`. Both `@CloseTo` entries are on the same variant class in the same array. Their ranges [0.05, 0.15] and [0.07, 0.17] overlap. The compiler reports:

```
error: Overlapping @CloseTo ranges within variant SmallDiscount for feature "discount-rate".
  @CloseTo(value=0.1, delta=0.05) -> range [0.05, 0.15]
  @CloseTo(value=0.12, delta=0.05) -> range [0.07, 0.17]
  Suggestion: reduce delta to increase precision, or remove the redundant entry.
```

#### 4: Intra-variant non-overlap -- valid multi-value double

Kenji writes `@Variant(doubleValue = {@CloseTo(0.1), @CloseTo(0.5)}, of = DiscountRate.class)` on `SmallDiscount`. With the default delta, the ranges do not overlap. Compilation succeeds.

#### 5: Inter-variant overlap with default delta

Kenji writes `@CloseTo(0.1)` on `SmallDiscount` and `@CloseTo(0.100001)` on `MediumDiscount`, both using the default delta. If the default delta is large enough that the ranges overlap, the compiler reports the overlap. This validates that the check uses actual delta values, not just exact value equality.

### UAT Scenarios (BDD)

#### Scenario: Overlapping @CloseTo ranges across variants rejected

```gherkin
Given Kenji defines a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
And SmallDiscount is annotated with @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.05), of = DiscountRate.class)
And MediumDiscount is annotated with @Variant(doubleValue = @CloseTo(value = 0.12, delta = 0.05), of = DiscountRate.class)
When the project compiles
Then compilation fails with error containing "Overlapping @CloseTo ranges for feature \"discount-rate\""
And the error message names SmallDiscount and MediumDiscount
And the error message shows range [0.05, 0.15] and range [0.07, 0.17]
And the error message suggests reducing delta or merging variants
```

#### Scenario: Non-overlapping @CloseTo ranges across variants accepted

```gherkin
Given Kenji defines a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
And SmallDiscount is annotated with @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.01), of = DiscountRate.class)
And LargeDiscount is annotated with @Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01), of = DiscountRate.class)
When the project compiles
Then compilation succeeds
```

#### Scenario: Overlapping @CloseTo ranges within same variant array rejected

```gherkin
Given Priya defines a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
And SmallDiscount is annotated with @Variant(doubleValue = {@CloseTo(value = 0.1, delta = 0.05), @CloseTo(value = 0.12, delta = 0.05)}, of = DiscountRate.class)
When the project compiles
Then compilation fails with error containing "Overlapping @CloseTo ranges within variant SmallDiscount"
And the error message shows both ranges
And the error message suggests reducing delta or removing the redundant entry
```

#### Scenario: Non-overlapping @CloseTo ranges within same variant array accepted

```gherkin
Given Kenji defines a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
And SmallDiscount is annotated with @Variant(doubleValue = {@CloseTo(0.1), @CloseTo(0.5)}, of = DiscountRate.class)
When the project compiles
Then compilation succeeds
```

#### Scenario: Overlapping ranges with default delta detected

```gherkin
Given Kenji defines a @Feature interface "DiscountRate" with flag key "discount-rate" and type DOUBLE
And SmallDiscount is annotated with @Variant(doubleValue = @CloseTo(0.1), of = DiscountRate.class)
And MediumDiscount is annotated with @Variant(doubleValue = @CloseTo(0.100001), of = DiscountRate.class)
When the project compiles
Then compilation fails with error containing "Overlapping @CloseTo ranges"
```

### Acceptance Criteria

- [ ] Overlapping `@CloseTo` ranges across different variant classes produce a compile error
- [ ] Overlapping `@CloseTo` ranges within a single variant's `doubleValue` array produce a compile error
- [ ] Non-overlapping `@CloseTo` ranges across variant classes compile successfully
- [ ] Non-overlapping `@CloseTo` ranges within a single variant's array compile successfully
- [ ] Error message identifies both overlapping variants by class name
- [ ] Error message displays the computed ranges `[value - delta, value + delta]`
- [ ] Error message suggests reducing delta or merging variants

### Outcome KPIs

- **Who**: Java developers using DOUBLE-typed features with `@CloseTo` variant dispatch
- **Does what**: Receive compile-time error when `@CloseTo` ranges overlap (ambiguous dispatch)
- **By how much**: 100% of overlapping ranges caught at compile time (zero ambiguous runtime dispatch)
- **Measured by**: Negative compilation tests covering inter-variant and intra-variant overlap scenarios
- **Baseline**: No overlap detection exists; ambiguous dispatch discovered at runtime

### Technical Notes

- Overlap condition: two `@CloseTo` annotations overlap when `|value1 - value2| < delta1 + delta2`. This is mathematically equivalent to checking if the intervals `[v1 - d1, v1 + d1]` and `[v2 - d2, v2 + d2]` intersect.
- Use `Double.compare()` or `BigDecimal` for range arithmetic to avoid floating-point comparison pitfalls.
- Inter-variant overlap detection belongs in M2 (`flagzen-typed-variants`) since it applies even without multi-value arrays. However, intra-variant overlap (within a `@CloseTo[]` array) is M13-specific.
- The processor should perform pairwise comparison of all `@CloseTo` annotations for the same feature. For N annotations, this is O(N^2) but N is typically small (< 10 variants per feature).
- Dependency: requires `@CloseTo` range parameters (`value`, `delta`) to be accessible at annotation processing time (they already are).
