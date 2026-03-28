# Outcome KPIs -- flagzen-openfeature

## Feature: OpenFeature SDK Adapter

### Objective

FlagZen seamlessly bridges to the OpenFeature ecosystem, proving the FlagProvider SPI works for vendor-neutral flag providers.

### Outcome KPIs

|  #  |                Who                |                      Does What                      |                                   By How Much                                    |                  Baseline                  |                         Measured By                          |  Type   |
| --- | --------------------------------- | --------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------ | ------- |
| 1   | Java developers using OpenFeature | Adopt FlagZen without replacing their flag provider | 100% feature parity with FlagProvider SPI contract (all 10 methods delegating)   | No OpenFeature adapter exists              | Automated test suite: all FlagProvider methods covered       | Leading |
| 2   | Java developers using OpenFeature | Resolve typed flags without string round-tripping   | Native typed resolution for boolean, int, long, double                           | FlagProvider defaults parse from getString | Unit tests asserting native OpenFeature typed methods called | Leading |
| 3   | Java developers using OpenFeature | Pass targeting context through the adapter          | 100% of FlagZen EvaluationContext fields mapped to OpenFeature EvaluationContext | No context mapping exists                  | Integration tests with targeting key + attributes            | Leading |

### Metric Hierarchy

- **North Star**: OpenFeature adapter passes full FlagProvider contract test suite (all typed methods + context overloads)
- **Leading Indicators**: Individual method delegation tests green; context mapping tests green
- **Guardrail Metrics**: No regression in flagzen-core test suite; no new runtime dependencies on flagzen-core

### Measurement Plan

|              KPI               |                      Data Source                      | Collection Method |  Frequency   |   Owner   |
| ------------------------------ | ----------------------------------------------------- | ----------------- | ------------ | --------- |
| FlagProvider contract coverage | JUnit test suite                                      | CI pipeline       | Every commit | Developer |
| Native typed delegation        | Unit tests asserting OpenFeature client method called | CI pipeline       | Every commit | Developer |
| Context mapping completeness   | Integration tests with mock OpenFeature client        | CI pipeline       | Every commit | Developer |

### Hypothesis

We believe that implementing `OpenFeatureFlagProvider` with native typed delegation and context mapping will allow Java developers on OpenFeature to adopt FlagZen's polymorphic dispatch with zero changes to their existing flag infrastructure. We will know this is true when the adapter passes all FlagProvider SPI contract tests and context-aware resolution tests.
