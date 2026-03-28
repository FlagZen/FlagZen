# Test Scenarios -- flagzen-typed-variants

## Scenario Inventory

| #  | File                                  | Scenario                                                                   | Tags                           | Category   |
| -- | ------------------------------------- | -------------------------------------------------------------------------- | ------------------------------ | ---------- |
| 1  | walking-skeleton.feature              | Developer declares an integer-typed feature with typed variants            | @walking-skeleton @US-M2-01 @US-M2-02 | Happy path |
| 2  | walking-skeleton.feature              | Developer resolves an integer-typed feature to the matching variant        | @walking-skeleton @US-M2-05    | Happy path |
| 3  | walking-skeleton.feature              | Developer dispatches a boolean feature using convenience annotations       | @walking-skeleton @US-M2-03 @US-M2-06 | Happy path |
| 4  | milestone-1-type-annotations.feature  | Feature with explicit BOOLEAN type compiles                                | @US-M2-01                      | Happy path |
| 5  | milestone-1-type-annotations.feature  | Feature with explicit LONG type compiles                                   | @US-M2-01                      | Happy path |
| 6  | milestone-1-type-annotations.feature  | Feature with explicit DOUBLE type compiles                                 | @US-M2-01                      | Happy path |
| 7  | milestone-1-type-annotations.feature  | Feature without type attribute defaults to STRING                          | @US-M2-01                      | Edge case  |
| 8  | milestone-1-type-annotations.feature  | Variant with integer value for INT feature                                 | @US-M2-02                      | Happy path |
| 9  | milestone-1-type-annotations.feature  | Variant with boolean value for BOOLEAN feature                             | @US-M2-02                      | Happy path |
| 10 | milestone-1-type-annotations.feature  | Variant with long value for LONG feature                                   | @US-M2-02                      | Happy path |
| 11 | milestone-1-type-annotations.feature  | Variant with approximate double value and default tolerance                | @US-M2-02                      | Happy path |
| 12 | milestone-1-type-annotations.feature  | Variant with approximate double value and explicit tolerance               | @US-M2-02                      | Edge case  |
| 13 | milestone-1-type-annotations.feature  | Existing string variant remains unchanged                                  | @US-M2-02                      | Boundary   |
| 14 | milestone-1-type-annotations.feature  | Convenience annotation for true is equivalent to boolean variant true      | @US-M2-03                      | Happy path |
| 15 | milestone-1-type-annotations.feature  | Convenience annotation for false is equivalent to boolean variant false    | @US-M2-03                      | Happy path |
| 16 | milestone-1-type-annotations.feature  | Convenience annotations with explicit feature target for multi-feature     | @US-M2-03                      | Edge case  |
| 17 | milestone-1-type-annotations.feature  | Convenience annotation infers target on single-feature class               | @US-M2-03                      | Edge case  |
| 18 | milestone-1-type-annotations.feature  | Convenience annotation mixed with explicit boolean variant                 | @US-M2-03                      | Edge case  |
| 19 | milestone-2-compile-validation.feature | String variant on INT feature is rejected                                 | @US-M2-04                      | Error path |
| 20 | milestone-2-compile-validation.feature | Integer variant on BOOLEAN feature is rejected                            | @US-M2-04                      | Error path |
| 21 | milestone-2-compile-validation.feature | Boolean variant on LONG feature is rejected                               | @US-M2-04                      | Error path |
| 22 | milestone-2-compile-validation.feature | Integer variant on DOUBLE feature is rejected                             | @US-M2-04                      | Error path |
| 23 | milestone-2-compile-validation.feature | Long variant on INT feature is rejected                                   | @US-M2-04                      | Error path |
| 24 | milestone-2-compile-validation.feature | Mixed variant attributes within same feature are rejected                 | @US-M2-04                      | Error path |
| 25 | milestone-2-compile-validation.feature | Duplicate integer values for the same feature are rejected                | @US-M2-04                      | Error path |
| 26 | milestone-2-compile-validation.feature | Duplicate long values for the same feature are rejected                   | @US-M2-04                      | Error path |
| 27 | milestone-2-compile-validation.feature | Duplicate boolean true values from mixed annotations are rejected         | @US-M2-04                      | Error path |
| 28 | milestone-2-compile-validation.feature | Boolean REQUIRED feature missing false variant is rejected                | @US-M2-04                      | Error path |
| 29 | milestone-2-compile-validation.feature | Boolean REQUIRED feature with both variants compiles                      | @US-M2-04                      | Happy path |
| 30 | milestone-2-compile-validation.feature | Boolean REQUIRED feature satisfied by default variant                     | @US-M2-04                      | Edge case  |
| 31 | milestone-2-compile-validation.feature | Type mismatch error message includes feature name and variant name        | @US-M2-04                      | Boundary   |
| 32 | milestone-2-compile-validation.feature | Every type mismatch produces an actionable suggested fix                  | @US-M2-04 @property            | Property   |
| 33 | milestone-3-typed-dispatch.feature    | Integer proxy dispatches to matching variant                               | @US-M2-05                      | Happy path |
| 34 | milestone-3-typed-dispatch.feature    | Integer proxy follows runtime flag changes                                 | @US-M2-05                      | Edge case  |
| 35 | milestone-3-typed-dispatch.feature    | Unmatched integer value with EXCEPTION fallback                            | @US-M2-05                      | Error path |
| 36 | milestone-3-typed-dispatch.feature    | Unmatched integer value falls back to default variant                      | @US-M2-05                      | Edge case  |
| 37 | milestone-3-typed-dispatch.feature    | Empty integer value triggers NOOP fallback                                 | @US-M2-05                      | Error path |
| 38 | milestone-3-typed-dispatch.feature    | Boolean proxy dispatches true to matching variant                          | @US-M2-06                      | Happy path |
| 39 | milestone-3-typed-dispatch.feature    | Boolean proxy dispatches false to matching variant                         | @US-M2-06                      | Happy path |
| 40 | milestone-3-typed-dispatch.feature    | Integer dispatch with explicit evaluation context                          | @US-M2-06                      | Happy path |
| 41 | milestone-3-typed-dispatch.feature    | Boolean dispatch with explicit evaluation context                          | @US-M2-06                      | Happy path |
| 42 | milestone-3-typed-dispatch.feature    | Typed dispatch with block-scoped context                                   | @US-M2-06                      | Happy path |
| 43 | milestone-3-typed-dispatch.feature    | Explicit context overrides scoped context for typed dispatch               | @US-M2-06                      | Edge case  |
| 44 | milestone-3-typed-dispatch.feature    | Typed dispatch with context accessor                                       | @US-M2-06                      | Edge case  |
| 45 | milestone-3-typed-dispatch.feature    | Typed dispatch without context falls back to default resolution            | @US-M2-06                      | Boundary   |
| 46 | milestone-3-typed-dispatch.feature    | Long proxy dispatches to matching variant                                  | @US-M2-07                      | Happy path |
| 47 | milestone-3-typed-dispatch.feature    | Double proxy dispatches via approximate matching with default tolerance     | @US-M2-07                      | Happy path |
| 48 | milestone-3-typed-dispatch.feature    | Double proxy matches variant with explicit tolerance                        | @US-M2-07                      | Happy path |
| 49 | milestone-3-typed-dispatch.feature    | Double proxy rejects value outside all tolerances                          | @US-M2-07                      | Error path |
| 50 | milestone-3-typed-dispatch.feature    | Double proxy selects first matching variant when ranges overlap            | @US-M2-07                      | Edge case  |
| 51 | milestone-3-typed-dispatch.feature    | Flag provider integer accessor parses from string value                    | @US-M2-05                      | Happy path |
| 52 | milestone-3-typed-dispatch.feature    | Flag provider boolean accessor rejects non-boolean strings                 | @US-M2-05                      | Error path |
| 53 | milestone-3-typed-dispatch.feature    | Flag provider boolean accessor parses "true" case-insensitively            | @US-M2-05                      | Boundary   |
| 54 | milestone-3-typed-dispatch.feature    | Flag provider long accessor returns no value for overflow                  | @US-M2-07                      | Error path |
| 55 | milestone-3-typed-dispatch.feature    | Flag provider double accessor parses valid double                          | @US-M2-07                      | Happy path |
| 56 | milestone-3-typed-dispatch.feature    | All typed accessors return no value for absent flag                        | @US-M2-05 @US-M2-07           | Error path |
| 57 | milestone-4-conditional-api.feature   | Boolean conditional check returns true                                     | @US-M2-08                      | Happy path |
| 58 | milestone-4-conditional-api.feature   | Boolean conditional check returns false                                    | @US-M2-08                      | Happy path |
| 59 | milestone-4-conditional-api.feature   | Boolean conditional check returns no value for non-boolean string          | @US-M2-08                      | Error path |
| 60 | milestone-4-conditional-api.feature   | Integer conditional check returns parsed value                             | @US-M2-08                      | Happy path |
| 61 | milestone-4-conditional-api.feature   | Integer conditional check returns no value for non-numeric string          | @US-M2-08                      | Error path |
| 62 | milestone-4-conditional-api.feature   | Long conditional check returns parsed value                                | @US-M2-08                      | Happy path |
| 63 | milestone-4-conditional-api.feature   | Long conditional check returns no value for non-numeric string             | @US-M2-08                      | Error path |
| 64 | milestone-4-conditional-api.feature   | Long conditional check returns no value for overflow                       | @US-M2-08                      | Error path |
| 65 | milestone-4-conditional-api.feature   | Double conditional check returns parsed value                              | @US-M2-08                      | Happy path |
| 66 | milestone-4-conditional-api.feature   | Double conditional check returns no value for non-numeric string           | @US-M2-08                      | Error path |
| 67 | milestone-4-conditional-api.feature   | All typed accessors return no value for absent flag                        | @US-M2-08                      | Error path |
| 68 | milestone-4-conditional-api.feature   | Integer conditional check with evaluation context                          | @US-M2-08                      | Happy path |
| 69 | milestone-4-conditional-api.feature   | Boolean conditional check with evaluation context                          | @US-M2-08                      | Happy path |
| 70 | milestone-4-conditional-api.feature   | Long conditional check with evaluation context                             | @US-M2-08                      | Happy path |
| 71 | milestone-4-conditional-api.feature   | Double conditional check with evaluation context                           | @US-M2-08                      | Happy path |

## Coverage Analysis

### Story-to-Scenario Traceability

| Story    | Scenarios                                        | Count | Error/Edge/Boundary |
| -------- | ------------------------------------------------ | ----- | ------------------- |
| US-M2-01 | 1, 4, 5, 6, 7                                   | 5     | 1 edge              |
| US-M2-02 | 1, 8, 9, 10, 11, 12, 13                         | 7     | 2 edge, 1 boundary  |
| US-M2-03 | 3, 14, 15, 16, 17, 18                           | 6     | 3 edge              |
| US-M2-04 | 19-32                                            | 14    | 10 error, 1 edge, 1 boundary, 1 property |
| US-M2-05 | 2, 33, 34, 35, 36, 37, 51, 52, 53, 56           | 10    | 3 error, 2 edge, 1 boundary |
| US-M2-06 | 3, 38, 39, 40, 41, 42, 43, 44, 45               | 9     | 2 edge, 1 boundary  |
| US-M2-07 | 46, 47, 48, 49, 50, 54, 55, 56                  | 8     | 2 error, 1 edge     |
| US-M2-08 | 57-71                                            | 15    | 6 error             |

All 8 user stories have scenario coverage.

### Scenario Category Distribution

| Category   | Count | Percentage |
| ---------- | ----- | ---------- |
| Happy path | 30    | 42%        |
| Error path | 21    | 30%        |
| Edge case  | 12    | 17%        |
| Boundary   | 5     | 7%         |
| Property   | 1     | 1%         |
| **Total**  | **71**| **100%**   |

**Error + edge + boundary + property**: 39 of 71 = **55%**. Exceeds 40% target.

### Walking Skeleton vs Focused Scenario Ratio

- Walking skeletons: 3
- Focused scenarios: 68
- Ratio: 3 skeletons + 68 focused (within 2-5 skeleton target)

## Implementation Sequence

Scenarios should be enabled in this order (one at a time):

1. Walking skeleton 1: Compile-time typed feature declaration
2. Walking skeleton 2: Runtime integer dispatch
3. Walking skeleton 3: Boolean convenience dispatch
4. Milestone 1 scenarios (15): type annotations, in @pending order
5. Milestone 2 scenarios (14): compile-time validation, in @pending order
6. Milestone 3 scenarios (24): typed dispatch + FlagProvider methods, in @pending order
7. Milestone 4 scenarios (15): conditional API, in @pending order

## Property-Tagged Scenarios

| # | Scenario | Signal | Implementation Guidance |
| - | -------- | ------ | ----------------------- |
| 32 | Every type mismatch produces an actionable suggested fix | "every" / universal invariant | Property-based test: for any `(FeatureType, wrongAttribute)` pair, error message contains suggested fix syntax |
