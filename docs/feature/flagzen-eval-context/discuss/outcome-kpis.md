# Outcome KPIs: Evaluation Context (flagzen-eval-context)

## Feature: Evaluation Context for Targeted Flag Resolution

### Objective

FlagZen's evaluation context API is ergonomic, type-safe, and backward-compatible, enabling per-user flag resolution without compromising the library's zero-reflection and compile-time safety guarantees.

### Outcome KPIs

|  #  |                    Who                     |                               Does What                               |                                   By How Much                                    |               Baseline               |                               Measured By                               |   Type    |
| --- | ------------------------------------------ | --------------------------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------ | ----------------------------------------------------------------------- | --------- |
| 1   | Java developer integrating eval context    | Build and pass EvaluationContext to resolve() in a single fluent call | 100% of context use cases require zero boilerplate beyond builder + resolve call | No context API exists                | API ergonomics review, example code line count                          | Leading   |
| 2   | Existing FlagProvider implementor          | Upgrade to M1 without modifying provider code                         | Zero code changes for existing providers (default method handles compat)         | FlagProvider has getString(key) only | Compilation test: InMemoryFlagProvider compiles unchanged against M1    | Leading   |
| 3   | Developer with request-handling code       | Scope context to a block without parameter drilling                   | Eliminates N parameter-passing modifications (N = call depth)                    | No block-scoping exists              | FlagContext.run() usage in integration examples                         | Leading   |
| 4   | Framework adapter author                   | Implement ContextAccessor SPI for custom context sources              | One interface + one ServiceLoader file                                           | No context extension point exists    | ContextAccessor implementations in M6 (reactor, mutiny)                 | Leading   |
| 5   | Developer on Java 21+ with virtual threads | Use FlagContext.run() without virtual thread pinning                  | Zero ThreadLocal pinning events                                                  | ThreadLocal-only implementation      | JFR analysis of FlagContext code path                                   | Leading   |
| 6   | All FlagZen users                          | Maintain zero-reflection guarantee in flagzen-core                    | Zero java.lang.reflect imports in runtime code                                   | Zero (M0 baseline)                   | ArchUnit test: noClasses in com.flagzen should access java.lang.reflect | Guardrail |
| 7   | All FlagZen users                          | Maintain PITest mutation kill rate                                    | >= 80% kill rate on new code                                                     | 84% (M0 baseline)                    | PITest CI gate                                                          | Guardrail |

### Metric Hierarchy

- **North Star**: API ergonomics -- developers can add per-user flag resolution to existing FlagZen code with minimal changes (1-3 lines for explicit, 0 lines for accessor)
- **Leading Indicators**: Builder fluency, resolve() overload discoverability, FlagContext.run() adoption
- **Guardrail Metrics**: Zero-reflection compliance (must NOT degrade), mutation kill rate >= 80%, backward compatibility (existing code compiles unchanged)

### Measurement Plan

|          KPI          |          Data Source          |                        Collection Method                        |  Frequency  |   Owner    |
| --------------------- | ----------------------------- | --------------------------------------------------------------- | ----------- | ---------- |
| API ergonomics        | Code examples, Javadoc review | Manual review of example code                                   | Per release | Maintainer |
| Backward compat       | CI build                      | Compile InMemoryFlagProvider against new FlagProvider interface | Per commit  | CI/CD      |
| Zero reflection       | ArchUnit test                 | Automated test in flagzen-core                                  | Per commit  | CI/CD      |
| Mutation kill rate    | PITest                        | CI job after build                                              | Per commit  | CI/CD      |
| Virtual thread safety | JFR                           | Manual test on Java 21+                                         | Per release | Maintainer |

### Hypothesis

We believe that adding evaluation context support with a resolution order (explicit > accessor > scoped > default) for Java developers using FlagZen will achieve ergonomic per-user flag resolution. We will know this is true when a developer can add A/B testing to an existing FlagZen integration by adding 1-3 lines of context-building code and zero changes to existing resolve() call sites (when using block-scoped or accessor context).
