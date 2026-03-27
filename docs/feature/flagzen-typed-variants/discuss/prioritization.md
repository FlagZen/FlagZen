# Prioritization: flagzen-typed-variants

## Release Priority

| Priority | Release | Target Outcome | Rationale |
| -------- | ------- | -------------- | --------- |
| 1 | Release 1 | End-to-end INT and BOOLEAN typed dispatch with @WhenTrue/@WhenFalse | Proves annotation model extension, processor validation, proxy generation, and FlagProvider typed methods all work together. Covers the two most common typed flag patterns (boolean toggles and integer counts). |
| 2 | Release 2 | LONG and DOUBLE typed dispatch with @CloseTo + full conditional API | Extends the established pattern to remaining numeric types. DOUBLE dispatch with approximate matching is the most novel behavior. |

## Backlog Suggestions

| Story | Release | Priority | Outcome Link | Dependencies |
| ----- | ------- | -------- | ------------ | ------------ |
| US-M2-01: FeatureType Enum and @Feature Type Attribute | R1 | P1 | Annotation model supports typed dispatch | None (M0 complete) |
| US-M2-02: Typed @Variant Attributes (intValue, booleanValue) | R1 | P1 | Variants declare typed match values | US-M2-01 |
| US-M2-03: @WhenTrue / @WhenFalse Convenience Annotations | R1 | P1 | Readable boolean dispatch syntax | US-M2-02 |
| US-M2-04: Compile-Time Type Validation (INT, BOOLEAN) | R1 | P1 | Processor rejects type mismatches with actionable errors | US-M2-02, US-M2-03 |
| US-M2-05: INT and BOOLEAN Proxy Dispatch + getInt/getBoolean | R1 | P1 | Proxy dispatches on typed values end-to-end | US-M2-04 |
| US-M2-06: Typed Dispatch with Evaluation Context | R1 | P1 | Typed dispatch respects M1 context chain | US-M2-05 |
| US-M2-07: LONG/DOUBLE Dispatch, @CloseTo, and getLong/getDouble | R2 | P2 | Complete typed dispatch and conditional API | US-M2-06 |
| US-M2-08: Conditional API (Non-Polymorphic Typed Accessors) | R2 | P2 | Direct typed flag access without polymorphism | US-M2-07 |

> **Note**: Story IDs assigned here. Dependencies are sequenced by implementation necessity -- each story builds on the previous. US-M2-06 covers the intersection of typed dispatch and evaluation context as a dedicated story.

## MoSCoW Classification

| Category | Stories | Rationale |
| --------- | ------- | --------- |
| Must Have | US-M2-01 through US-M2-06 | Without typed dispatch (INT + BOOLEAN) and context integration, the milestone has no value |
| Should Have | US-M2-07 | LONG/DOUBLE dispatch completes the type system; @CloseTo solves real floating-point imprecision from JS backends |
| Should Have | US-M2-08 | Conditional API completes the FlagProvider surface and is a documented public API |

## Value/Effort Assessment

| Story | Value | Urgency | Effort | Score | Notes |
| ----- | ----- | ------- | ------ | ----- | ----- |
| US-M2-01 | 4 | 5 | 1 | 20.0 | Foundation for everything; tiny effort |
| US-M2-02 | 5 | 5 | 2 | 12.5 | Core typed attributes |
| US-M2-03 | 4 | 4 | 1 | 16.0 | Small but high readability impact |
| US-M2-04 | 5 | 5 | 3 | 8.3 | Critical safety net; moderate complexity |
| US-M2-05 | 5 | 5 | 3 | 8.3 | Core runtime behavior |
| US-M2-06 | 4 | 4 | 2 | 8.0 | Context integration reuses M1 patterns |
| US-M2-07 | 4 | 3 | 3 | 4.0 | Pattern established; mechanical extension |
| US-M2-08 | 3 | 3 | 1 | 9.0 | Low effort, completes public API |
