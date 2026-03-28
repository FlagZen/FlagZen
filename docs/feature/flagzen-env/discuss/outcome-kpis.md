# Outcome KPIs -- flagzen-env

## Feature: Environment Variable Flag Provider

### Objective

Developers can source feature flags from environment variables with sensible defaults and clean parse/format separation, enabling 12-factor app deployments without external flag management services, fitting any team's naming convention, and handling multi-convention environments with explicit conflict control.

### Outcome KPIs

|  #  |               Who               |                           Does What                           |                         By How Much                         |                            Baseline                            |                      Measured By                       |  Type   |
| --- | ------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------ | ------- |
| 1   | Backend developers              | Resolve flags from env vars without custom code               | 0 lines of configuration for default convention             | Must implement custom FlagProvider or use InMemoryFlagProvider | Integration test using create() with standard env vars | Leading |
| 2   | Backend developers              | Predict flag key from env var name without docs               | 100% predictability for FLAGZEN_SCREAMING_SNAKE convention  | No convention (each project invents its own)                   | Test coverage of default parse-format pipeline         | Leading |
| 3   | Developers adding the module    | Activate provider without registration boilerplate            | 0 lines of configuration                                    | InMemoryFlagProvider requires explicit construction            | ServiceLoader integration test                         | Leading |
| 4   | Backend developers              | Get O(1) flag resolution with no runtime I/O                  | Zero System.getenv() calls after construction               | Naive implementation calls System.getenv() per read            | Unit test verifying no env access post-construction    | Leading |
| 5   | Platform engineers              | Use team-specific env var prefixes                            | Any prefix string supported via parser configuration        | FLAGZEN_ hardcoded, no customization                           | Parameterized tests with diverse parser configurations | Leading |
| 6   | Developers with mixed codebases | Parse env vars in any convention (SCREAMING_SNAKE, camelCase) | 2 built-in parser types with optional prefix                | Only SCREAMING_SNAKE_CASE supported                            | Parameterized tests for each parser variant            | Leading |
| 7   | Developers with mixed codebases | Format flag keys in any convention                            | 6 built-in formatters + lambda covers all conventions       | Only one format supported                                      | Parameterized tests for each formatter                 | Leading |
| 8   | Platform engineers (multi-team) | Resolve flags from multiple env var conventions               | One provider instance instead of multiple                   | Must create multiple providers                                 | Integration test with multiple parsers                 | Leading |
| 9   | Platform engineers              | Detect and handle flag key conflicts                          | Conflicts caught at construction time, not silently ignored | No conflict detection -- last mapping wins silently            | Tests for both WARN and ERROR strategies               | Leading |
| 10  | Developers during migration     | Get conflict visibility at point of use                       | Warning surfaced on first access of conflicted key          | Conflict warning only at startup, easily missed                | Test verifying first-access warning and no-repeat      | Leading |

### Metric Hierarchy

- **North Star**: Developers can toggle flags via env vars with zero boilerplate for defaults and minimal code for customization
- **Leading Indicators**: Parse-format pipeline predictability, ServiceLoader discovery success rate, FlagProvider contract compliance, parser/formatter coverage of common conventions, conflict detection accuracy
- **Guardrail Metrics**: No regressions in flagzen-core tests, no reflection at runtime, thread safety (immutable map), FlagKeyParser/FlagKeyFormat SAM interface compatibility with lambdas

### Measurement Plan

|                KPI                |   Data Source    |                      Collection Method                       |  Frequency  |       Owner        |
| --------------------------------- | ---------------- | ------------------------------------------------------------ | ----------- | ------------------ |
| Contract compliance               | Unit tests       | All FlagProvider methods tested                              | Every build | flagzen-env module |
| Parse-format pipeline correctness | Unit tests       | Parameterized tests covering env var -> segments -> flag key | Every build | flagzen-env module |
| ServiceLoader discovery           | Integration test | Load provider via ServiceLoader in test                      | Every build | flagzen-env module |
| Eager loading verification        | Unit test        | Verify no System.getenv() after construction                 | Every build | flagzen-env module |
| Built-in parser correctness       | Unit tests       | Parameterized tests for screamingSnakeCase and camelCase     | Every build | flagzen-env module |
| Built-in formatter correctness    | Unit tests       | Parameterized tests for all 6 formatters                     | Every build | flagzen-env module |
| Multiple parser contribution      | Integration test | Tests with mixed env var conventions                         | Every build | flagzen-env module |
| Conflict strategy correctness     | Unit tests       | Tests for WARN and ERROR with controlled conflicts           | Every build | flagzen-env module |
| First-access warning              | Unit test        | Verify warning on first access, no repeat                    | Every build | flagzen-env module |

### Hypothesis

We believe that providing an `EnvironmentVariableFlagProvider` with clean parse/format separation (`FlagKeyParser` for input, `FlagKeyFormat` for output), sensible defaults (FLAGZEN_ prefix + kebab formatter), eager loading into an immutable map, and configurable `ConflictStrategy` will achieve zero-config adoption for standard use cases and minimal-code adoption for custom conventions. We will know this is true when developers can add the dependency and resolve flags from env vars with any naming convention using at most 3 lines of builder configuration.
