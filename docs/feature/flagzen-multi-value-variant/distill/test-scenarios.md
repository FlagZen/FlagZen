# Test Scenario Inventory: flagzen-multi-value-variant

## Summary

| Category | Count |
| --- | --- |
| Walking skeleton | 2 |
| Happy path (focused) | 14 |
| Error path (focused) | 12 |
| Edge case (focused) | 3 |
| **Total** | **31** |
| **Error path ratio** | **39% (12/31)** |

Note: Error ratio is 39%, approaching the 40% target. The feature is primarily compile-time validation where error paths are well-defined. The 12 error scenarios cover all identified failure modes from the user stories including duplicate detection across all scopes and @CloseTo overlap in both inter-variant and intra-variant contexts.

## Scenario Inventory by Feature File

### walking-skeleton.feature (2 scenarios, NOT @pending)

| # | Scenario | Story | Category |
| --- | --- | --- | --- |
| 1 | Developer maps multiple string values to one variant implementation | US-MV-01 | Walking skeleton |
| 2 | Developer resolves a multi-value string feature to the matching variant at runtime | US-MV-01 | Walking skeleton |

### milestone-1-annotation-schema.feature (14 scenarios, @pending)

| # | Scenario | Story | Category |
| --- | --- | --- | --- |
| 3 | Single string value still compiles without changes | US-MV-01 | Happy path |
| 4 | Empty string in array is rejected at compile time | US-MV-01 | Error path |
| 5 | Multiple int values map to one implementation | US-MV-02 | Happy path |
| 6 | Single int value still compiles without changes | US-MV-02 | Happy path |
| 7 | Integer.MIN_VALUE is a valid int array value | US-MV-02 | Edge case |
| 8 | Multiple long values map to one implementation | US-MV-04 | Happy path |
| 9 | Single long value still compiles without changes | US-MV-04 | Happy path |
| 10 | Multiple CloseTo values map to one implementation | US-MV-01 | Happy path |
| 11 | Array values compose with a repeated single-value annotation | US-MV-05 | Happy path |
| 12 | Two arrays on the same class compose all values | US-MV-05 | Happy path |
| 13 | Multi-value satisfies REQUIRED fallback coverage | US-MV-06 | Happy path |
| 14 | Invalid enum value in array is rejected | US-MV-06 | Error path |
| 15 | Incomplete REQUIRED coverage despite multi-value reports missing variant | US-MV-06 | Error path |

### milestone-2-duplicate-detection.feature (8 scenarios, @pending)

| # | Scenario | Story | Category |
| --- | --- | --- | --- |
| 16 | Duplicate string value across different classes is rejected | US-MV-03 | Error path |
| 17 | Duplicate string value within the same array is rejected | US-MV-03 | Error path |
| 18 | Duplicate between array and repeated annotation is rejected | US-MV-03, US-MV-05 | Error path |
| 19 | Different string values across classes compile successfully | US-MV-03 | Happy path |
| 20 | Duplicate int value across different classes is rejected | US-MV-03 | Error path |
| 21 | Duplicate long value across different classes is rejected | US-MV-03 | Error path |
| 22 | Duplicate int value within the same array is rejected | US-MV-03 | Error path |

### milestone-3-closeto-overlap.feature (5 scenarios, @pending)

| # | Scenario | Story | Category |
| --- | --- | --- | --- |
| 23 | Overlapping @CloseTo ranges across variants is rejected | US-MV-07 | Error path |
| 24 | Non-overlapping @CloseTo ranges across variants compiles successfully | US-MV-07 | Happy path |
| 25 | Overlapping @CloseTo ranges with default delta is detected | US-MV-07 | Edge case |
| 26 | Overlapping @CloseTo ranges within the same variant array is rejected | US-MV-07 | Error path |
| 27 | Non-overlapping @CloseTo ranges within the same variant array compiles successfully | US-MV-07 | Happy path |

### milestone-4-runtime-dispatch.feature (6 scenarios, @pending)

| # | Scenario | Story | Category |
| --- | --- | --- | --- |
| 28 | Flag value matching first array element dispatches correctly | US-MV-01 | Happy path |
| 29 | Flag value matching second array element dispatches correctly | US-MV-01 | Happy path |
| 30 | Flag value matching single-value variant dispatches correctly | US-MV-01 | Happy path |
| 31 | Int flag value matching any array element dispatches correctly | US-MV-02 | Happy path (typed) |
| 32 | Long flag value matching any array element dispatches correctly | US-MV-04 | Happy path (typed) |
| 33 | Unmatched flag value with multi-value variants triggers fallback | US-MV-01 | Error path |

## Story-to-Scenario Traceability (Dim 8 Check A)

| Story | Scenario Count | Scenarios |
| --- | --- | --- |
| US-MV-01 | 10 | 1, 2, 3, 4, 10, 27, 28, 29, 32 + walking skeleton 1 |
| US-MV-02 | 4 | 5, 6, 7, 30 |
| US-MV-03 | 6 | 16, 17, 18, 19, 20, 21 |
| US-MV-04 | 4 | 8, 9, 31, + long duplicate (21 covers long type) |
| US-MV-05 | 3 | 11, 12, 18 |
| US-MV-06 | 3 | 13, 14, 15 |
| US-MV-07 | 5 | 22, 23, 24, 25, 26 |

All 7 stories have at least one scenario. PASS.

## Implementation Sequence (one-at-a-time)

1. Walking Skeleton 1 (compile-time string array) -- enables first
2. Walking Skeleton 2 (runtime dispatch) -- enables second
3. Milestone 1 scenarios in order: backward compat, empty string, int array, long array, double, composability, enum validation
4. Milestone 2 scenarios: cross-class dup, intra-array dup, cross-syntax dup, no-false-positive, int dup, long dup
5. Milestone 3 scenarios: inter-variant overlap, non-overlap, default delta, intra-variant overlap, intra non-overlap
6. Milestone 4 scenarios: string dispatch variations, int dispatch, long dispatch, unmatched fallback
