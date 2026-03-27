# Prioritization: Condition Predicates (flagzen-conditions)

## Release Priority

| Priority |         Release          |                  Target Outcome                  |                                                    Rationale                                                    |
| -------- | ------------------------ | ------------------------------------------------ | --------------------------------------------------------------------------------------------------------------- |
| 1        | Walking Skeleton (R1)    | End-to-end predicate dispatch works              | Validates the core assumption: annotation-driven predicate dispatch is viable with compile-time code generation |
| 2        | Compile-Time Safety (R2) | All invalid configurations caught before runtime | Completes the safety net -- users trust the library because wrong things do not compile                         |
| 3        | Fallback Behavior (R3)   | Graceful handling when no predicate matches      | Handles the "what if nothing matches" case consistently with existing FallbackStrategy                          |
| 4        | Spring DI (R4)           | Predicates use constructor injection             | DX improvement for Spring users; no-arg constructor is sufficient for non-Spring                                |

## Backlog Suggestions

|  Story   | Release | Priority | MoSCoW | Value (1-5) | Urgency (1-5) | Effort (1-5) | Score |         Dependencies          |
| -------- | ------- | -------- | ------ | ----------- | ------------- | ------------ | ----- | ----------------------------- |
| US-CP-01 | R1 (WS) | P1       | Must   | 5           | 5             | 1            | 25.0  | None (uses JDK interfaces)    |
| US-CP-02 | R1 (WS) | P1       | Must   | 5           | 5             | 1            | 25.0  | None                          |
| US-CP-04 | R1 (WS) | P1       | Must   | 5           | 5             | 2            | 12.5  | US-CP-01, US-CP-02            |
| US-CP-06 | R1 (WS) | P1       | Must   | 5           | 5             | 3            | 8.3   | US-CP-01, US-CP-02, US-CP-04  |
| US-CP-03 | R2      | P2       | Must   | 4           | 4             | 1            | 16.0  | US-CP-02                      |
| US-CP-05 | R2      | P2       | Must   | 4           | 4             | 2            | 8.0   | US-CP-02, US-CP-04            |
| US-CP-07 | R3      | P3       | Should | 4           | 3             | 2            | 6.0   | US-CP-06                      |
| US-CP-08 | R4      | P4       | Could  | 3           | 2             | 2            | 3.0   | US-CP-06, M4 (flagzen-spring) |

> **Note**: Story IDs are assigned in Phase 4 (Requirements). This table is finalized there.

## Delivery Sequence

```
US-CP-01 (JDK predicates)  ----+
| --> US-CP-04 (type validation) --> US-CP-06 (proxy dispatch) |
| US-CP-02 (@Condition)    -----+                              |
                                                                       v
US-CP-03 (@Condition model)  --> US-CP-05 (order validation)          US-CP-07 (fallback)
                                                                       |
                                                                       v
                                                                  US-CP-08 (Spring DI)
```
