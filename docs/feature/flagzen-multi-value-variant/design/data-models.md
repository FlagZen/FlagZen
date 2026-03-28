# Data Models -- flagzen-multi-value-variant (M13)

## 1. Annotation Schema Changes

### `@Variant` Annotation

#### `value()` -- String to `String[]`

| Property | Before | After |
| --- | --- | --- |
| Type | `String` | `String[]` |
| Default | `""` | `""` (Java auto-wraps to `{""}`) |
| "Not set" detection | `value.isEmpty()` | Single-element array where element is `""` |
| Valid values | Any non-empty string | Any non-empty string per element; empty strings in arrays are rejected |

**Default behavior note**: Java annotation defaults allow `String[] value() default ""` which produces `{""}` at runtime. The processor treats a single-element array containing only `""` as "not set" -- identical to current behavior. This preserves full source compatibility.

**Explicit empty array**: `@Variant(value = {}, ...)` is treated identically to the default (no string value specified). The processor checks for "has non-empty-string elements" rather than "array is non-empty".

#### `intValue()` -- int to `int[]`

| Property | Before | After |
| --- | --- | --- |
| Type | `int` | `int[]` |
| Default | `Integer.MIN_VALUE` | `{}` (empty array) |
| "Not set" detection | `== Integer.MIN_VALUE` | `length == 0` |
| Reserved values | `Integer.MIN_VALUE` is reserved | None -- all int values are valid |

**Unreserved value**: `Integer.MIN_VALUE` (-2147483648) becomes a valid variant value. Pre-1.0, this is an acceptable behavioral change.

#### `longValue()` -- long to `long[]`

| Property | Before | After |
| --- | --- | --- |
| Type | `long` | `long[]` |
| Default | `Long.MIN_VALUE` | `{}` (empty array) |
| "Not set" detection | `== Long.MIN_VALUE` | `length == 0` |
| Reserved values | `Long.MIN_VALUE` is reserved | None -- all long values are valid |

#### `doubleValue()` -- No Change

Already `CloseTo[]` with default `{}`. Multi-value already structurally supported. M13 adds `@CloseTo` overlap detection but does not change the element type.

#### `booleanValue()` -- No Change

Remains `String` with default `""`. Only two possible values (true/false), so multi-value mapping is meaningless.

## 2. VariantModel Record -- No Structural Change

The `VariantModel` record is unchanged. The key design decision is that array expansion happens in the processor, not in the model. Each array element produces a separate `VariantModel` instance.

### Example: Array Expansion

Given annotation: `@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)` on `ClassicCheckout`.

**Before M13** (not possible -- scalar value):

```
VariantModel("com.example.ClassicCheckout", "CLASSIC")
```

**After M13** (array expansion produces two instances):

```
VariantModel("com.example.ClassicCheckout", "CLASSIC")
VariantModel("com.example.ClassicCheckout", "LEGACY")
```

Both share the same `qualifiedClassName`. Downstream logic (duplicate detection, enum validation, coverage checks, code generation) operates on this flat list without modification.

### Example: Int Array Expansion

Given: `@Variant(intValue = {3, 5}, of = PricingTier.class)` on `BulkPricing`.

Produces:

```
VariantModel("com.example.BulkPricing", "", 3, FeatureType.INT)
VariantModel("com.example.BulkPricing", "", 5, FeatureType.INT)
```

### Example: Double Multi-Value (already supported structurally)

Given: `@Variant(doubleValue = {@CloseTo(0.1), @CloseTo(0.5)}, of = DiscountRate.class)` on `SmallDiscount`.

**Before M13**: only `closeToValues[0]` was used (first element only).

**After M13**: iterates all elements:

```
VariantModel.ofDouble("com.example.SmallDiscount", 0.1, 1e-10)
VariantModel.ofDouble("com.example.SmallDiscount", 0.5, 1e-10)
```

This is a behavioral fix -- the structural support was already there, but the processor only read index 0.

## 3. Generated Code Impact

### Proxy Variant Map

The generated proxy's constructor receives `Map<KeyType, Supplier<Feature>>`. Array expansion means more entries in the map, all pointing to the same `Supplier`.

**Before M13** (single value):

```java
Map.of("CLASSIC", (Supplier<CheckoutFlow>) ClassicCheckout::new)
```

**After M13** (multi-value):

```java
Map.of(
    "CLASSIC", (Supplier<CheckoutFlow>) ClassicCheckout::new,
    "LEGACY", (Supplier<CheckoutFlow>) ClassicCheckout::new
)
```

### Metadata `variantSuppliers()`

Same pattern -- more entries in `Map.of(...)`. Each `VariantModel` in the list produces one map entry.

**Constraint**: `Map.of()` supports up to 10 entries. If a feature has more than 10 variant values total (across all implementations), the code generator may need to use `Map.ofEntries()` or a builder pattern. This is an existing limitation, not introduced by M13, but becomes more relevant with multi-value arrays.

## 4. Validation Data Flow

### `hasTypeMismatch()` Detection Matrix

| Feature Type | Correct attribute | "Has value" check (before) | "Has value" check (after) |
| --- | --- | --- | --- |
| STRING | `value()` | `!value.isEmpty()` | Array has non-`""` element |
| INT | `intValue()` | `!= Integer.MIN_VALUE` | `length > 0` |
| LONG | `longValue()` | `!= Long.MIN_VALUE` | `length > 0` |
| DOUBLE | `doubleValue()` | `length > 0` | `length > 0` (no change) |
| BOOLEAN | `booleanValue()` | `!booleanValue.isEmpty()` | `!booleanValue.isEmpty()` (no change) |

### `@CloseTo` Overlap Detection Data

For each DOUBLE-typed feature, the processor collects all `@CloseTo` annotations (flattened from arrays across all variant classes). Each entry has:

- `value`: the center point
- `delta`: the tolerance
- `variantClassName`: the implementation class it belongs to

**Overlap formula**: Two entries `(v1, d1)` and `(v2, d2)` overlap when `Math.abs(v1 - v2) < d1 + d2`.

**Complexity**: O(N^2) pairwise comparison where N = total `@CloseTo` entries for one feature. N is typically small (< 10).
