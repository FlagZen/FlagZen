# Wave Decisions -- flagzen-env DISTILL

## Context

- **Feature ID**: flagzen-env
- **Date**: 2026-03-28
- **Wave**: DISTILL (acceptance-designer)
- **Prior wave**: DESIGN (solution-architect) -- complete

## Decisions

### Decision 1: Feature Scope -- Extension

flagzen-env is a modular add-on (FlagProvider implementation). Tests exercise the public API of two modules: `flagzen-key-mapping` (parsers, formatters, ConflictStrategy) and `flagzen-env` (EnvironmentVariableFlagProvider, builder).

### Decision 2: Test Framework -- Cucumber

Project already uses Cucumber with JUnit Platform. Feature files go in `tests/acceptance/flagzen-env/`. Step definitions go in `com.flagzen.acceptance.steps` (shared glue path).

### Decision 3: Integration Approach -- Real Services

Tests use the real `EnvironmentVariableFlagProvider` with injected environment maps via `Supplier<Map<String, String>>` on the builder. No mocks for the provider itself. ServiceLoader discovery tested against real classpath.

### Decision 4: Infrastructure Testing -- No

Functional acceptance tests only. No Gradle build verification, no CI pipeline tests.

### Decision 5: Walking Skeleton Scope

Thinnest E2E slice: one env var with FLAGZEN_ prefix -> create() with defaults -> getString() returns value. Two scenarios: happy path + missing key (error path). Walking skeleton is not tagged @pending -- runs immediately.

### Decision 6: Milestone Organization

Feature files organized by capability, not release:

- `walking-skeleton.feature` -- 2 scenarios (run immediately)
- `milestone-1-key-mapping.feature` -- 11 scenarios (parsers + formatters)
- `milestone-2-env-provider.feature` -- 15 scenarios (provider, eager loading, builder, ServiceLoader)
- `milestone-3-conflict-strategy.feature` -- 15 scenarios (multi-parser, multi-formatter, conflict strategy, first-access warning)

### Decision 7: Tag Strategy

- Walking skeletons: no @pending tag, run immediately via Cucumber filter `not @pending`
- All other scenarios: @pending tag, enabled one-at-a-time during DELIVER
- Story traceability: @US-ENV-01 through @US-ENV-10 on every scenario

### Decision 8: Driving Port Identification

All scenarios exercise through these driving ports only:

| Driving Port | Used In |
| --- | --- |
| `FlagKeyParsers.screamingSnakeCase(prefix)` | milestone-1 parser scenarios |
| `FlagKeyParsers.camelCase(prefix)` | milestone-1 parser scenarios |
| `FlagKeyFormats.*()` | milestone-1 formatter scenarios |
| `EnvironmentVariableFlagProvider.create()` | walking skeleton, milestone-2 default scenarios |
| `EnvironmentVariableFlagProvider.builder()` | milestone-2 custom config, milestone-3 conflict scenarios |
| `provider.getString(key)` | all scenarios exercising runtime resolution |
| `ServiceLoader.load(FlagProvider.class)` | milestone-2 ServiceLoader scenarios |

No internal classes (pipeline helpers, conflict tracking sets, builder internals) are tested directly.

### Decision 9: DEVOPS Graceful Degradation

DEVOPS artifacts not yet created. Using default environment assumptions: clean environment with controlled env var injection via `Supplier<Map<String, String>>`. No environment matrix needed -- env vars are injected programmatically, not read from real OS environment.

## Artifact Inventory

| Artifact | Path | Status |
| --- | --- | --- |
| Walking skeleton | `tests/acceptance/flagzen-env/walking-skeleton.feature` | Complete |
| Milestone 1 | `tests/acceptance/flagzen-env/milestone-1-key-mapping.feature` | Complete |
| Milestone 2 | `tests/acceptance/flagzen-env/milestone-2-env-provider.feature` | Complete |
| Milestone 3 | `tests/acceptance/flagzen-env/milestone-3-conflict-strategy.feature` | Complete |
| Scenario inventory | `docs/feature/flagzen-env/distill/test-scenarios.md` | Complete |
| Walking skeleton rationale | `docs/feature/flagzen-env/distill/walking-skeleton.md` | Complete |
| Wave decisions | `docs/feature/flagzen-env/distill/wave-decisions.md` | This file |

## Handoff to DELIVER

### Mandate Compliance Evidence

- **CM-A (Hexagonal Boundary)**: All scenarios invoke through driving ports: `FlagKeyParsers.*`, `FlagKeyFormats.*`, `EnvironmentVariableFlagProvider.create()`, `EnvironmentVariableFlagProvider.builder()`, `provider.getString()`, `ServiceLoader.load()`. Zero internal component references in Gherkin.
- **CM-B (Business Language)**: Gherkin uses domain terms only: "developer", "flag", "parser", "formatter", "conflict", "warning", "error". Zero technical terms (no HTTP, JSON, database, status codes, class names in scenario text).
- **CM-C (User Journey)**: 2 walking skeletons (user value E2E) + 31 focused scenarios (boundary tests). Walking skeletons prove "developer sets env var, reads flag value."
- **CM-D (Pure Function Extraction)**: Parsers and formatters are pure functions (input -> output, no side effects). Provider construction is the only impure operation (reads `System.getenv()`), isolated behind `Supplier<Map<String, String>>` injection.

### Implementation Sequence (One-at-a-Time)

1. Walking skeleton (2 scenarios -- run first, prove pipeline works)
2. Milestone 1: parsers, then formatters (enable one scenario at a time)
3. Milestone 2: default config, eager loading, ServiceLoader, custom config, error paths
4. Milestone 3: multiple parsers, multiple formatters, cardinality defaults, WARN/ERROR behavior, first-access warning

### Build Configuration Note

The `tests/acceptance/flagzen-env` path must be added to the Cucumber features configuration in `flagzen-acceptance-tests/build.gradle.kts` during DELIVER.
