# Prioritization: FlagZen

## Release Priority

| Priority |              Release               |                                 Target Outcome                                 |                     KPI                      |                                             Rationale                                             |
| -------- | ---------------------------------- | ------------------------------------------------------------------------------ | -------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| 1        | Walking Skeleton                   | End-to-end dispatch works: define, implement, resolve, test                    | Core concept proven in working code          | Validates A2 (polymorphic dispatch) and A8 (compile-time feasibility) -- highest-risk assumptions |
| 2        | Release 1: Core Type-Safe Dispatch | Developer can use FlagZen for all flag scenarios with full compile-time safety | First external user can integrate and test   | Delivers the complete core value proposition; enables blog post / conference demo                 |
| 3        | Release 2: Production Readiness    | Developer can use FlagZen in a real Spring Boot production app                 | First production deployment by external user | Removes the "toy library" objection; Spring integration is table stakes                           |
| 4        | Release 3: Ecosystem Integration   | Developer can use FlagZen with existing flag providers                         | External contributions to provider adapters  | Reduces migration friction; opens community contribution path                                     |
| 5        | Release 4: Observability           | Developer can manage flag lifecycle with data                                  | Community-driven extension development       | Nice-to-have; builds on established adoption                                                      |

## Riskiest Assumptions Addressed by Release

|     Release      |                                                 Assumptions Validated                                                  |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Walking Skeleton | A8 (compile-time processing feasible), A2 (polymorphic dispatch is compelling), A9 (proxy-based resolution acceptable) |
| Release 1        | A4 (devs will learn annotation API), A6 (testing support drives adoption), A3 (compile-time safety matters)            |
| Release 2        | A5 (Spring integration is table stakes), A1 (multi-provider pain justifies new library)                                |
| Release 3        | A7 (OpenFeature leaves DX gaps), A10 (OSS can achieve critical mass)                                                   |

## Backlog Suggestions

|                        Story                        | Release | Priority |             Outcome Link             | Dependencies |
| --------------------------------------------------- | ------- | -------- | ------------------------------------ | ------------ |
| US-01: Define @Feature interface                    | WS / R1 | P1       | Core concept                         | None         |
| US-02: Implement @Variant classes                   | WS / R1 | P1       | Core concept                         | US-01        |
| US-04: Generate dispatch proxy at compile time      | WS / R1 | P1       | Core concept                         | US-01, US-02 |
| US-05: Resolve active variant via FeatureDispatcher | WS / R1 | P1       | Core concept                         | US-04        |
| US-07: Pin flag values with @PinFlag                | WS / R1 | P1       | Testing DX (adoption wedge)          | US-04, US-05 |
| US-06: FlagProvider SPI + in-memory provider        | R1      | P1       | Core concept                         | US-05        |
| US-03: Compile-time variant enum validation         | R1      | P2       | Compile-time safety (differentiator) | US-01, US-02 |
| US-09: FallbackStrategy (REQUIRED/EXCEPTION/NOOP)   | R1      | P2       | Error handling completeness          | US-04, US-05 |
| US-08: @FlagSource file-based test config           | R1      | P2       | Testing DX                           | US-07        |
| US-10: Environment variable flag provider           | R2      | P3       | Production readiness                 | US-06        |
| US-11: Spring Boot auto-configuration               | R2      | P3       | Production readiness                 | US-04, US-05 |
| US-12: @Autowired @Feature injection                | R2      | P3       | Spring DX                            | US-11        |
| US-13: Explicit evaluation context                  | R2      | P3       | A/B testing support                  | US-05        |
| US-14: Block-scoped evaluation context              | R2      | P3       | Multi-tenancy support                | US-13        |

> **Note**: Story IDs assigned here match Phase 4 user stories. Walking Skeleton stories (US-01, US-02, US-04, US-05, US-07) form the minimum buildable slice.

## Value/Effort Assessment

| Story | Value | Urgency | Effort | Score (V*U/E) |                             Notes                              |
| ----- | ----- | ------- | ------ | ------------- | -------------------------------------------------------------- |
| US-01 | 5     | 5       | 2      | 12.5          | Foundation -- everything depends on this                       |
| US-02 | 5     | 5       | 2      | 12.5          | Foundation -- co-depends with US-01                            |
| US-04 | 5     | 5       | 4      | 6.25          | Highest technical complexity (annotation processor + code gen) |
| US-05 | 5     | 5       | 3      | 8.3           | Core runtime path                                              |
| US-07 | 5     | 4       | 2      | 10.0          | Adoption wedge -- highest DX impact                            |
| US-06 | 4     | 4       | 2      | 8.0           | Needed for runtime, simple SPI                                 |
| US-03 | 4     | 3       | 3      | 4.0           | Differentiator but not on critical path                        |
| US-09 | 4     | 3       | 3      | 4.0           | Error handling completeness                                    |
| US-08 | 3     | 2       | 2      | 3.0           | Convenience for test-heavy projects                            |
| US-11 | 4     | 3       | 3      | 4.0           | Table stakes for Spring adoption                               |
| US-10 | 3     | 3       | 1      | 9.0           | Simple but needed for real use                                 |
| US-12 | 4     | 3       | 2      | 6.0           | Depends on US-11                                               |
| US-13 | 3     | 2       | 2      | 3.0           | Advanced use case                                              |
| US-14 | 3     | 2       | 3      | 2.0           | Advanced use case, ScopedValue complexity                      |

## Walking Skeleton Build Order

The walking skeleton should be built in this dependency order:

```
US-01 (Define @Feature)
  |
  v
US-02 (Implement @Variant)
  |
  v
US-04 (Annotation Processor + Proxy Generation)  <-- highest risk, tackle early
  |
  v
US-06 (FlagProvider SPI + In-Memory Provider)
  |
  v
US-05 (FeatureDispatcher.resolve())
  |
  v
US-07 (@PinFlag Testing)
```

This order derisks the annotation processor (hardest part) in the middle, with well-defined inputs from US-01/02 and clear output consumed by US-05/06/07.
