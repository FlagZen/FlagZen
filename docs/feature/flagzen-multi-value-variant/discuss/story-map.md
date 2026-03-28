# Story Map: flagzen-multi-value-variant

## User: Kenji Nakamura (senior Java developer)

## Goal: Map multiple flag values to one variant implementation declaratively at compile time

## Backbone

|             Define Annotation Schema              |          Process Multi-Value Arrays           |             Detect Duplicates             |             Generate Proxy Code             |
| ------------------------------------------------- | --------------------------------------------- | ----------------------------------------- | ------------------------------------------- |
| Change `String value()` to `String[] value()`     | Expand each array element into a VariantModel | Cross-array duplicate detection           | Map entries for all values to same Supplier |
| Change `int intValue()` to `int[] intValue()`     | Handle mixed array + repeated annotations     | Cross-class duplicate detection           | No runtime change to dispatch               |
| Change `long longValue()` to `long[] longValue()` | Validate array elements against inner enum    | Intra-array duplicate detection           | Metadata lists all values                   |
| Document `doubleValue` already supports arrays    | REQUIRED fallback counts multi-value coverage | Clear error messages with all class names |                                             |
| Skip `booleanValue` multi-value                   | Validate @CloseTo ranges for overlap          | @CloseTo inter-variant overlap detection  |                                             |
|                                                    |                                               | @CloseTo intra-variant overlap detection  |                                             |

---

### Walking Skeleton

Thinnest end-to-end slice: `String[] value()` on `@Variant` + processor expands array into multiple VariantModels + duplicate detection across arrays + proxy maps all values.

- Define: `String value()` -> `String[] value()` with `default ""` -> `default ""`  (source compatible)
- Process: `collectVariants()` iterates array, creates one VariantModel per element
- Detect: `hasDuplicateVariantValues()` already works (groups by variantKeyLiteral)
- Generate: No proxy change needed -- VariantModel list is already the input

### Release 1: String + Int Multi-Value (Walking Skeleton + typed extension)

**Stories:**

- US-01: String array multi-value (`String[] value()`)
- US-02: Int array multi-value (`int[] intValue()`)
- US-03: Compile-time duplicate detection across multi-value arrays

**Outcome:** Developer can use array syntax for the two most common types (string and int), with full compile-time safety.

### Release 2: Long Multi-Value + Composability

**Stories:**

- US-04: Long array multi-value (`long[] longValue()`)
- US-05: Array values compose with repeated `@Variant` annotations
- US-06: Enum validation and REQUIRED fallback with multi-value coverage
- US-07: Compile-time detection of overlapping `@CloseTo` ranges

**Outcome:** Full type coverage (string, int, long -- double already works, boolean skipped), composability with existing `@Repeatable` mechanism, and compile-time safety for ambiguous `@CloseTo` range dispatch.

## Scope Assessment: PASS -- 7 stories, 1 bounded context (flagzen-core), estimated 5-6 days
