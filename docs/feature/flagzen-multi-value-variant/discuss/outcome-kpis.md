# Outcome KPIs: flagzen-multi-value-variant

## Feature: Multi-Value Variant Mapping

### Objective

Java developers can declaratively map multiple flag values to one variant implementation using array syntax, with full compile-time safety.

### Outcome KPIs

|  #  |                         Who                         |                           Does What                           |                      By How Much                       |                                 Baseline                                  |             Measured By              |        Type         |
| --- | --------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------- | ------------------------------------ | ------------------- |
| 1   | Java developers with multi-value variants           | Use array syntax instead of repeated annotations              | 100% of repeated-for-same-class annotations eliminated | Requires N repeated annotations for N values                              | Source code comparison + test suite  | Leading             |
| 2   | Java developers defining variants                   | Receive compile-time error for duplicate values across arrays | 100% of duplicates caught at compile time              | Duplicates in single-value annotations caught; cross-array not applicable | Negative compilation tests           | Leading             |
| 3   | Java developers with existing single-value @Variant | Compile without source changes after annotation schema change | 0 source changes required for existing code            | N/A (new feature)                                                         | Existing test suite passes unchanged | Leading (guardrail) |
| 4   | Java developers using DOUBLE-typed @CloseTo variants | Receive compile-time error for overlapping @CloseTo ranges    | 100% of overlapping ranges caught at compile time      | No overlap detection; ambiguous dispatch discovered at runtime            | Negative compilation tests           | Leading             |

### Metric Hierarchy

- **North Star**: Existing test suite passes unchanged after annotation element type changes (backward compatibility)
- **Leading Indicators**: Multi-value tests pass for all types (string, int, long); composability tests pass
- **Guardrail Metrics**: PITest mutation kill rate stays >= 80%; no existing tests broken

### Measurement Plan

|             KPI              |        Data Source         | Collection Method |  Frequency   | Owner |
| ---------------------------- | -------------------------- | ----------------- | ------------ | ----- |
| Backward compatibility       | Existing test suite        | Gradle build      | Every commit | CI    |
| Multi-value correctness      | New BDD scenarios          | Gradle build      | Every commit | CI    |
| Duplicate detection coverage | Negative compilation tests | Gradle build      | Every commit | CI    |
| @CloseTo overlap detection   | Negative compilation tests | Gradle build      | Every commit | CI    |
| Mutation kill rate           | PITest report              | CI pipeline       | Every PR     | CI    |

### Hypothesis

We believe that changing `@Variant` annotation elements from scalar to array types for Java developers with multi-value variant mappings will achieve declarative multi-value mapping with zero boilerplate duplication. We will know this is true when existing single-value annotations compile unchanged AND multi-value arrays dispatch correctly for all supported types AND overlapping `@CloseTo` ranges are rejected at compile time.
