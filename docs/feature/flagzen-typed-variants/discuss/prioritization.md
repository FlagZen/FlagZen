# Prioritization: flagzen-typed-variants

## Release Priority

| Priority |     Release      |               Target Outcome                |                                                           Rationale                                                            |
| -------- | ---------------- | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| 1        | Walking Skeleton | End-to-end INT typed dispatch works         | Proves the annotation model extension, processor validation, proxy generation, and FlagProvider typed method all work together |
| 2        | Release 1        | Boolean dispatch with REQUIRED completeness | Adds second type with its unique validation rule (exactly 2 variants), completing typed polymorphic dispatch                   |
| 3        | Release 2        | Full conditional API                        | Adds remaining numeric accessors (getLong, getDouble) and context-aware overloads for non-polymorphic usage                    |

## Backlog Suggestions

|                          Story                           | Release | Priority |                       Outcome Link                       |    Dependencies    |
| -------------------------------------------------------- | ------- | -------- | -------------------------------------------------------- | ------------------ |
| US-M2-01: FeatureType Enum and @Feature Type Attribute   | WS      | P1       | Annotation model supports typed dispatch                 | None (M0 complete) |
| US-M2-02: @Variant intValue Attribute                    | WS      | P1       | Variants can declare integer match values                | US-M2-01           |
| US-M2-03: Compile-Time Type Consistency Validation       | WS      | P1       | Processor rejects type mismatches with actionable errors | US-M2-02           |
| US-M2-04: Integer Proxy Dispatch and FlagProvider.getInt | WS      | P1       | Proxy dispatches on int values end-to-end                | US-M2-03           |
| US-M2-05: @Variant booleanValue and Boolean Dispatch     | R1      | P2       | Boolean typed dispatch with REQUIRED completeness        | US-M2-04           |
| US-M2-06: Conditional API -- getBoolean and getInt       | R2      | P3       | Non-polymorphic typed flag access                        | US-M2-04           |
| US-M2-07: Conditional API -- getLong and getDouble       | R2      | P3       | Complete numeric accessor set                            | US-M2-06           |

> **Note**: Story IDs are placeholders assigned in Phase 4 (Requirements). Dependencies are sequenced by implementation necessity -- each story builds on the previous.

## MoSCoW Classification

|  Category   |          Stories          |                                   Rationale                                   |
| ----------- | ------------------------- | ----------------------------------------------------------------------------- |
| Must Have   | US-M2-01 through US-M2-05 | Without typed dispatch (INT + BOOLEAN), the milestone has no value            |
| Should Have | US-M2-06                  | Conditional API is part of milestone scope and completes the FlagProvider API |
| Could Have  | US-M2-07                  | getLong/getDouble are symmetric additions with low risk                       |
