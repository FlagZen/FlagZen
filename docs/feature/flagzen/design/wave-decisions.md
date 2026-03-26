# Wave Decisions -- FlagZen DESIGN

## Context

- **Feature ID**: flagzen
- **Date**: 2026-03-26
- **Wave**: DESIGN (solution-architect)
- **Prior wave**: DISCUSS (product-owner) -- PROCEED decision, all phases complete, 9 user stories passed DoR

## DESIGN Summary

|               Phase               |  Status  |                                           Key Output                                            |
| --------------------------------- | -------- | ----------------------------------------------------------------------------------------------- |
| Phase 1: Requirements Analysis    | COMPLETE | All 9 user stories analyzed, quality attributes prioritized (maintainability, testability)      |
| Phase 2: Existing System Analysis | COMPLETE | Greenfield project -- no existing code or infrastructure                                        |
| Phase 3: Constraint Analysis      | COMPLETE | Library (not service), zero runtime reflection, Java 17+, compile-time code gen                 |
| Phase 4: Architecture Design      | COMPLETE | Module architecture, SPI contracts, annotation processor design, 7 ADRs, C4 diagrams (L1+L2+L3) |
| Phase 5: Quality Validation       | COMPLETE | All quality gates passed                                                                        |
| Phase 6: Peer Review              | APPROVED | 0 critical, 0 high, 2 medium (resolved in iteration 1)                                          |

## Architecture Decisions Made

### Pre-Confirmed (from user input)

|  #  |                           Decision                            |                             Rationale                             |
| --- | ------------------------------------------------------------- | ----------------------------------------------------------------- |
| 1   | One proxy per @Feature interface                              | Self-contained, same-package access, incremental compilation      |
| 2   | FlagProvider: `Optional<String> getString(String key)` for R1 | Polymorphic dispatch only needs strings; typed accessors deferred |
| 3   | FeatureDispatcher is interface + factory                      | Testable, injectable, internal evolution freedom                  |
| 4   | Generated proxy: public class, package-private constructor    | DI-visible, user-construction-prevented                           |

### Decided in DESIGN Wave

|  #  |                              Decision                              |       ADR        |                               Rationale                                |
| --- | ------------------------------------------------------------------ | ---------------- | ---------------------------------------------------------------------- |
| 5   | Compile-time proxy generation (not dynamic proxy, not ByteBuddy)   | ADR-001          | Zero reflection, debuggable, IDE-visible generated source              |
| 6   | JavaPoet for code generation                                       | ADR-002          | Type-safe Java source generation, import management, formatting        |
| 7   | Gradle monorepo with 9 submodules                                  | ADR-005          | One concern per module, no unnecessary transitive deps                 |
| 8   | CLASS retention for core annotations, RUNTIME for test annotations | ADR-006          | Zero runtime reflection in core; test annotations need runtime access  |
| 9   | ContextAccessor SPI for reactive context propagation               | Architecture     | Pluggable context source, ServiceLoader discovery                      |
| 10  | ArchUnit for architectural enforcement                             | Technology Stack | Automated enforcement of zero-reflection, package structure, no cycles |

## Artifacts Produced

|                 Artifact                  |                        File                        |  Status   |
| ----------------------------------------- | -------------------------------------------------- | --------- |
| Architecture Design                       | `design/architecture-design.md`                    | Complete  |
| Technology Stack                          | `design/technology-stack.md`                       | Complete  |
| Component Boundaries                      | `design/component-boundaries.md`                   | Complete  |
| Data Models                               | `design/data-models.md`                            | Complete  |
| Wave Decisions                            | `design/wave-decisions.md`                         | This file |
| ADR-001: Proxy Generation Strategy        | `adrs/ADR-001-proxy-generation-strategy.md`        | Accepted  |
| ADR-002: Code Generation Tooling          | `adrs/ADR-002-code-generation-tooling.md`          | Accepted  |
| ADR-003: FlagProvider SPI Contract        | `adrs/ADR-003-flagprovider-contract.md`            | Accepted  |
| ADR-004: FeatureDispatcher Design         | `adrs/ADR-004-feature-dispatcher-design.md`        | Accepted  |
| ADR-005: Module Structure                 | `adrs/ADR-005-module-structure.md`                 | Accepted  |
| ADR-006: Annotation Retention and Targets | `adrs/ADR-006-annotation-retention-and-targets.md` | Accepted  |
| ADR-007: Generated Proxy Visibility       | `adrs/ADR-007-generated-proxy-visibility.md`       | Accepted  |
| CLAUDE.md                                 | `CLAUDE.md` (project root)                         | Complete  |

## Handoff Package for DISTILL Wave (acceptance-designer)

### What the acceptance-designer receives

1. **Architecture design**: Module boundaries, dependency graph, SPI contracts, annotation processor flow, C4 diagrams (L1+L2+L3)
2. **Component boundaries**: 9 modules with clear responsibilities, public API surface (<20 types, <50 methods)
3. **Data models**: Compile-time models (FeatureModel, VariantModel), runtime models (EvaluationContext), annotation definitions, SPI contracts
4. **Technology stack**: All technology choices with rationale, license, and alternatives considered
5. **ADRs**: 7 architectural decision records covering proxy strategy, code gen, SPI contract, dispatcher design, module structure, annotation retention, proxy visibility
6. **User stories**: 9 stories from DISCUSS wave (US-01 through US-09) with BDD scenarios and acceptance criteria

### What the acceptance-designer should produce

1. Acceptance tests derived from the BDD scenarios in user stories
2. Test strategy for annotation processor (compile-testing library)
3. Test strategy for generated proxy behavior
4. Integration test scenarios for FlagProvider SPI
5. Test isolation strategy for parallel test execution (@PinFlag)

### Development paradigm

**OOP (Java)**. Written to project CLAUDE.md.

### External integrations annotation

Contract tests recommended for LaunchDarkly, Togglz, and OpenFeature APIs -- consumer-driven contracts (e.g., Pact or Spring Cloud Contract) to detect breaking changes in provider SDKs before production. These are Release 3+ concerns.

## Handoff Package for DEVOPS Wave (platform-architect)

### What the platform-architect receives

1. **Build system**: Gradle monorepo with 9 submodules, Kotlin DSL, Java 17+
2. **Publishing**: Maven Central via Sonatype, BOM/platform dependency recommended
3. **CI quality gates**: JaCoCo (>90% core), Checkstyle, SpotBugs, ArchUnit, Spotless
4. **Annotation processor testing**: Google compile-testing library
5. **External integrations**: LaunchDarkly SDK, Togglz, OpenFeature SDK -- contract tests recommended
6. **Module dependency graph**: All deps flow inward to flagzen-core, no cross-extension deps

## Quality Gate Status

- [x] Requirements traced to components
- [x] Component boundaries with clear responsibilities
- [x] Technology choices in ADRs with alternatives
- [x] Quality attributes addressed (maintainability, testability, performance, reliability, portability)
- [x] Dependency-inversion compliance (core defines SPIs, extensions implement)
- [x] C4 diagrams (L1 + L2 + L3 for flagzen-core)
- [x] Integration patterns specified (SPI, ServiceLoader, Spring auto-config, JUnit extension)
- [x] OSS preference validated (all dependencies are OSS, Apache 2.0/EPL 2.0/LGPL 2.1)
- [x] AC behavioral, not implementation-coupled
- [x] External integrations annotated with contract test recommendation
- [x] Architectural enforcement tooling recommended (ArchUnit)
- [x] Peer review completed and approved

## Risk Register

|                                      Risk                                       | Probability | Impact |                                 Mitigation                                 |
| ------------------------------------------------------------------------------- | ----------- | ------ | -------------------------------------------------------------------------- |
| Annotation processor complexity exceeds expectations                            | Medium      | High   | Walking skeleton (US-01/02/04) derisk early; JavaPoet reduces risk         |
| JavaPoet generates incorrect code for edge cases (generics, checked exceptions) | Low         | Medium | Google compile-testing library validates generated code compiles correctly |
| Cross-module variant discovery at compile time                                  | Low         | Medium | Explicitly deferred to runtime startup check (Release 2)                   |
| Spring auto-configuration conflicts with user configuration                     | Low         | Medium | Conditional annotations (@ConditionalOnMissingBean)                        |
| Package-private constructor prevents dispatcher access across packages          | Medium      | Medium | Generated FeatureMetadata interface or ServiceLoader-based discovery       |
