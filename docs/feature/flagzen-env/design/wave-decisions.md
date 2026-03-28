# Wave Decisions -- flagzen-env DESIGN

## Context

- **Feature ID**: flagzen-env
- **Date**: 2026-03-28
- **Wave**: DESIGN (solution-architect)
- **Prior wave**: DISCUSS (product-owner) -- complete
- **Architect**: Morgan

## DESIGN Summary

| Phase | Status | Key Output |
| --- | --- | --- |
| Phase 1: Requirements Analysis | COMPLETE | Quality attributes identified: testability, performance, extensibility (primary) |
| Phase 2: Existing System Analysis | COMPLETE | Analyzed FlagProvider SPI, InMemoryFlagProvider, EvaluationContext, existing module structure |
| Phase 3: Constraint Analysis | COMPLETE | Java 17+, zero-reflection spirit, Gradle monorepo, SPI discovery |
| Phase 4: Architecture Design | COMPLETE | C4 L2+L3 diagrams, component boundaries, data models, 3 ADRs |
| Phase 5: Quality Validation | COMPLETE | All quality gates passed |
| Phase 6: Peer Review | COMPLETE | Self-review completed |

## Architecture Decisions

### Decision 1: flagzen-key-mapping is independent of flagzen-core

**Context**: The DISCUSS wave proposed `flagzen-key-mapping` as a separate module. The question was whether it should depend on `flagzen-core`.

**Decision**: No dependency on flagzen-core. The key-mapping module is entirely self-contained with zero external dependencies.

**Rationale**: `FlagKeyParser`, `FlagKeyFormat`, and `ConflictStrategy` are generic abstractions with no FlagZen-specific coupling. Making the module independent maximizes reusability -- any Java project can use key mapping without adopting FlagZen.

### Decision 2: `api` scope for flagzen-key-mapping dependency in flagzen-env

**Context**: Should flagzen-env declare flagzen-key-mapping as `api` or `implementation`?

**Decision**: `api` scope.

**Rationale**: The builder exposes `FlagKeyParser` and `FlagKeyFormat` types in its public method signatures. Consumers calling `.parser()` or `.formatter()` need these types on their compile classpath. `api` ensures transitive availability.

### Decision 3: `Supplier<Map<String, String>>` for testability

**Context**: How to make `System.getenv()` testable without reflection or mocking frameworks.

**Decision**: Builder accepts `.environmentSource(Supplier<Map<String, String>>)` defaulting to `System::getenv`.

**Rationale**: Clean injection, no reflection, no PowerMock, no static mocking. Tests pass a controlled map. The `Supplier` wrapper (vs raw `Map`) allows the builder to defer the read to `build()` time.

### Decision 4: snakeCase parser added to built-in set

**Context**: DISCUSS wave specified `screamingSnakeCase` and `camelCase` built-in parsers. The architecture adds `snakeCase` for completeness.

**Decision**: Include `snakeCase(String prefix)` and `snakeCase()` as built-in parsers.

**Rationale**: `snakeCase` (lowercase underscored, e.g., `checkout_flow`) is a common naming convention in config files and some environments. It is trivially implemented alongside the other parsers and completes the common-convention coverage. The cost is minimal; the benefit is covering a real convention without forcing users to write a custom lambda.

### Decision 5: No-arg constructor for ServiceLoader

**Context**: ServiceLoader requires a public no-arg constructor. The provider needs default configuration.

**Decision**: Public no-arg constructor delegates to `create()` (which uses builder with all defaults).

**Rationale**: ServiceLoader compatibility without exposing mutable construction. The no-arg constructor is the only way to get a default-configured instance via ServiceLoader. Explicit configuration uses the builder.

## Quality Gates

- [x] Requirements traced to components (see architecture-design.md Section 16)
- [x] Component boundaries with clear responsibilities (see component-boundaries.md)
- [x] Technology choices in ADRs with alternatives (ADR-015, ADR-016, ADR-017)
- [x] Quality attributes addressed: performance (eager loading), testability (supplier injection), reliability (immutable map), maintainability (module split)
- [x] Dependency-inversion compliance: SAM interfaces are ports; built-in implementations are adapters
- [x] C4 diagrams: L2 (container) + L3 (component for both modules)
- [x] Integration patterns specified (ServiceLoader, builder, transitive deps)
- [x] OSS preference validated: zero external dependencies, all JDK APIs
- [x] AC behavioral, not implementation-coupled (all scenarios describe WHAT behavior, not HOW)
- [x] No external integrations requiring contract tests (System.getenv() is JDK)
- [x] Architectural enforcement tooling recommended (ArchUnit, Gradle constraints)
- [x] Peer review completed

## Artifacts Produced

| Artifact | File | Status |
| --- | --- | --- |
| Architecture Design | `architecture-design.md` | Complete |
| Component Boundaries | `component-boundaries.md` | Complete |
| Data Models | `data-models.md` | Complete |
| Wave Decisions | `wave-decisions.md` | This file |
| ADR-015 | `docs/adrs/ADR-015-key-mapping-module-split.md` | Accepted |
| ADR-016 | `docs/adrs/ADR-016-eager-loading-strategy.md` | Accepted |
| ADR-017 | `docs/adrs/ADR-017-conflict-strategy-design.md` | Accepted |

## Handoff Package for DISTILL Wave (acceptance-designer)

### What acceptance-designer receives

1. Architecture document with C4 L2 + L3 diagrams, pipeline description, conflict algorithm
2. Component boundaries with public API surface and package structure
3. Data models with interface contracts, builder API, and map construction examples
4. 3 ADRs documenting key architectural decisions
5. Story-to-component mapping for test planning

### What acceptance-designer should produce

- Acceptance tests for all 10 user stories
- Test fixtures for environment variable injection (using `Supplier<Map<String, String>>`)
- Parameterized tests for all built-in parsers and formatters
- Conflict strategy tests for all cardinality combinations
- First-access warning tests (warn once, no repeat)
- ServiceLoader discovery integration test

### Development paradigm

OOP (Java 17+). No change from project default.
