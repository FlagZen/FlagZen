# Outcome KPIs: Condition Predicates (flagzen-conditions)

## Objective

Java developers using FlagZen can declaratively select variants based on runtime context predicates with the same compile-time safety and ergonomic DX they have for value-based dispatch.

## Outcome KPIs

| # | Who | Does What | By How Much | Baseline | Measured By | Type |
|---|-----|-----------|-------------|----------|-------------|------|
| 1 | Java developers defining conditions | Express a variant selection rule in a single annotation | 100% -- one @Condition annotation per variant | Manual if/else chains outside FlagZen | API usage: @Variant(when = @Condition(...)) line count vs. manual dispatch | Leading |
| 2 | Java developers compiling | Get compile-time error for invalid predicate configuration | 100% of invalid predicates caught at compile time | Runtime ClassCastException or silent failure | Annotation processor error test coverage | Leading |
| 3 | Java developers at runtime | Dispatch to correct variant based on EvaluationContext predicates | Deterministic -- same context always selects same variant | No predicate dispatch exists | Unit tests with varied EvaluationContext inputs | Leading |
| 4 | Java developers handling edge cases | Handle "no match" consistently with existing FallbackStrategy | Zero new concepts -- same 3 strategies | FallbackStrategy exists for value-based only | Fallback behavior tests mirror value-based tests | Leading |
| 5 | Spring developers | Use constructor injection in predicates | Zero workarounds (no service locator patterns) | No-arg constructor only | Spring integration tests with injected predicates | Leading |

## Metric Hierarchy

- **North Star**: Developers can implement the Strategy pattern declaratively via @Condition with compile-time safety (KPI 1 + KPI 2)
- **Leading Indicators**: Compile-time validation coverage (KPI 2), deterministic dispatch (KPI 3)
- **Guardrail Metrics**: Existing value-based dispatch behavior must NOT degrade. All M0 tests must pass unchanged. Compile time must not increase by more than 10% for projects not using conditions.

## Measurement Plan

| KPI | Data Source | Collection Method | Frequency | Owner |
|-----|------------|-------------------|-----------|-------|
| 1 | API design review | Count annotation attributes vs. manual code | Once at design | Product owner |
| 2 | Processor error tests | Automated test suite | Per build (CI) | Developer |
| 3 | Dispatch unit tests | Automated test suite with varied contexts | Per build (CI) | Developer |
| 4 | Fallback tests | Automated test suite mirroring M0 tests | Per build (CI) | Developer |
| 5 | Spring integration tests | Automated test suite with @Component predicates | Per build (CI) | Developer |

## Hypothesis

We believe that adding @Condition predicate-based dispatch for Java developers using FlagZen will enable declarative Strategy pattern selection with compile-time safety. We will know this is true when developers can express variant selection rules in a single @Condition annotation, all invalid configurations are caught at compile time, and runtime dispatch is deterministic against the EvaluationContext.
