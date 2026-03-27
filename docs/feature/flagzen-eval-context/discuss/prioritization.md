# Prioritization: Evaluation Context (flagzen-eval-context)

## Release Priority

| Priority |             Release              |                        Target Outcome                        |                          KPI                          |                                                        Rationale                                                        |
| -------- | -------------------------------- | ------------------------------------------------------------ | ----------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| 1        | Release 1: Core Context Support  | End-to-end context passing works across all resolution paths | Developer can resolve flags per-user in < 5 min setup | Validates the core API design and resolution order -- the riskiest assumption is that the resolution order is intuitive |
| 2        | Release 2: Java 21+ Optimization | ScopedValue carrier for virtual thread compatibility         | FlagContext.run() works correctly on virtual threads  | Enhancement, not required for correctness -- ThreadLocal works on all versions                                          |

## Backlog Suggestions

|  Story   | Release | Priority |      Outcome Link       |         Dependencies         |
| -------- | ------- | -------- | ----------------------- | ---------------------------- |
| US-EC-01 | R1      | P1       | KPI-1 (API ergonomics)  | None                         |
| US-EC-02 | R1      | P1       | KPI-1 (API ergonomics)  | US-EC-01                     |
| US-EC-03 | R1      | P1       | KPI-2 (backward compat) | US-EC-01                     |
| US-EC-04 | R1      | P1       | KPI-1 (API ergonomics)  | US-EC-02, US-EC-03           |
| US-EC-05 | R1      | P2       | KPI-3 (DX convenience)  | US-EC-01                     |
| US-EC-06 | R1      | P2       | KPI-4 (extensibility)   | US-EC-01                     |
| US-EC-07 | R1      | P1       | KPI-1 (API ergonomics)  | US-EC-02, US-EC-05, US-EC-06 |
| US-EC-08 | R2      | P3       | KPI-5 (thread safety)   | US-EC-05                     |

## Dependency Graph

```
US-EC-01 (EvaluationContext model)
  |
  +--- US-EC-02 (explicit resolve overload)
  |                                                                        |
  | +--- US-EC-04 (proxy passes context) --- depends on US-EC-03 too       |
  |                                                                        |
  | +--- US-EC-07 (resolution order) --- depends on US-EC-05, US-EC-06 too |
  |                                                                        |
  +--- US-EC-03 (FlagProvider overload)
  |
  +--- US-EC-05 (FlagContext.run block scope)
  |                                          |
  | +--- US-EC-08 (ScopedValue optimization) |
  |                                          |
  +--- US-EC-06 (ContextAccessor SPI)
```

## MoSCoW Classification

|  Category   |                                     Stories                                      |                                        Rationale                                        |
| ----------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Must Have   | US-EC-01, US-EC-02, US-EC-03, US-EC-04, US-EC-07                                 | Without these, context-aware resolution does not work at all                            |
| Should Have | US-EC-05, US-EC-06                                                               | Block scope and ContextAccessor complete the DX but explicit context works without them |
| Could Have  | US-EC-08                                                                         | ScopedValue optimization for Java 21+ -- ThreadLocal fallback covers correctness        |
| Won't Have  | Reactive ContextAccessor implementations (M6), provider-specific context mapping | Explicitly out of scope for this milestone                                              |

## Value/Effort Analysis

|  Story   | Value | Urgency | Effort | Score (V*U/E) |                      Notes                      |
| -------- | ----- | ------- | ------ | ------------- | ----------------------------------------------- |
| US-EC-01 | 5     | 5       | 2      | 12.5          | Foundation -- everything depends on it          |
| US-EC-02 | 5     | 5       | 2      | 12.5          | Core API surface                                |
| US-EC-03 | 4     | 5       | 1      | 20.0          | Small but critical for backward compat          |
| US-EC-04 | 5     | 5       | 3      | 8.3           | Annotation processor change -- moderate effort  |
| US-EC-05 | 4     | 3       | 3      | 4.0           | Nice DX but not strictly required               |
| US-EC-06 | 3     | 2       | 2      | 3.0           | Foundation for M6, not immediately used         |
| US-EC-07 | 5     | 5       | 2      | 12.5          | Resolution order logic ties everything together |
| US-EC-08 | 2     | 1       | 3      | 0.7           | Optimization, can defer                         |

## Recommended Build Order

1. US-EC-01 (EvaluationContext model) -- foundation
2. US-EC-03 (FlagProvider overload) -- backward compat first
3. US-EC-02 (FeatureDispatcher overload) -- explicit path
4. US-EC-04 (proxy passes context) -- wires everything together
5. US-EC-05 (FlagContext.run) -- block scope convenience
6. US-EC-06 (ContextAccessor SPI) -- extension point
7. US-EC-07 (resolution order) -- integration of all paths
8. US-EC-08 (ScopedValue) -- R2 optimization
