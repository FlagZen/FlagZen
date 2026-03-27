# Outcome KPIs: flagzen-typed-variants

## Feature: Typed Polymorphic Dispatch and Conditional API

### Objective

Java developers using FlagZen can dispatch on integer, boolean, long, and double flag values with compile-time type safety, eliminating string-encoding workarounds and manual parsing boilerplate.

### Outcome KPIs

| # | Who | Does What | By How Much | Baseline | Measured By | Type |
| - | --- | --------- | ----------- | -------- | ----------- | ---- |
| 1 | Java developers with int/bool/long/double flags | Declare typed features and variants without string encoding | 100% of typed features use typed annotations | All features are string-typed | Annotation processor processes all 5 FeatureType values correctly | Leading |
| 2 | Java developers making type mistakes | Receive compile-time errors for type mismatches | 100% of type mismatches caught at compile time | Zero type validation (runtime failures only) | Processor emits ERROR diagnostic for every type-inconsistent @Variant | Leading |
| 3 | Java developers using conditional API | Access typed flag values without manual parsing | Zero manual getString+parse calls for boolean/int/long/double | Manual parsing for every typed flag check | Typed default methods on FlagProvider return correct values | Leading |
| 4 | Java developers with boolean features | Use concise @WhenTrue/@WhenFalse annotations | 50% reduction in boolean variant annotation verbosity | Only @Variant(booleanValue = ...) available | Processor treats @WhenTrue/@WhenFalse identically to @Variant(booleanValue = ...) | Leading |
| 5 | FlagZen library | Maintains backward compatibility for existing features | Zero breaking changes to existing @Feature/@Variant usage | Current M0/M1 annotation behavior | All existing tests pass without modification | Guardrail |
| 6 | Java developers using evaluation context | Typed dispatch respects context resolution chain | 100% parity between typed and string context resolution | Context not yet integrated with typed dispatch | All context scenarios pass for typed features | Leading |

### Metric Hierarchy

- **North Star**: Developers use typed annotations for non-string features instead of string encoding
- **Leading Indicators**: Processor validates type consistency; proxy dispatches on typed values; @WhenTrue/@WhenFalse used for boolean features; conditional API methods parse correctly; typed dispatch respects context chain
- **Guardrail Metrics**: Existing string-typed features compile and dispatch identically; no behavioral changes for STRING FeatureType; `FlagProvider.getString()` contract unchanged

### Measurement Plan

| KPI | Data Source | Collection Method | Frequency | Owner |
| --- | ----------- | ----------------- | --------- | ----- |
| Type mismatch detection | Processor diagnostics | Compile-time error count in test suite | Every build | flagzen-core |
| Typed dispatch correctness | UAT scenarios | Automated test pass rate | Every build | flagzen-core |
| Double approximate matching | UAT scenarios for @CloseTo | Delta-based matching tests | Every build | flagzen-core |
| Context integration parity | UAT scenarios for typed context | Context resolution tests for all types | Every build | flagzen-core |
| Backward compatibility | Existing test suite | All M0/M1 tests green | Every build | flagzen-core |
| Parse correctness | FlagProvider default method tests | Unit tests for all input categories | Every build | flagzen-core |

### Hypothesis

We believe that adding typed annotations (FeatureType, intValue, booleanValue, longValue, doubleValue, @CloseTo, @WhenTrue, @WhenFalse) and typed FlagProvider accessors for Java developers using FlagZen will achieve type-safe dispatch without string workarounds. We will know this is true when developers can annotate features with any of the five FeatureType values, receive compile-time errors for type mismatches, use @WhenTrue/@WhenFalse for readable boolean dispatch, have proxies dispatch on typed values (including approximate double matching), and use context-aware typed accessors -- all without breaking any existing string-typed features.
