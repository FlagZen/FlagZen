# Wave Decisions -- Evaluation Context DESIGN

## Context

- **Feature ID**: flagzen-eval-context
- **Date**: 2026-03-27
- **Wave**: DESIGN (solution-architect)
- **Prior wave**: DISCUSS (product-owner) -- 8 user stories (US-EC-01 through US-EC-08), 2-release split

## DESIGN Summary

|               Phase               |  Status  |                                                         Key Output                                                         |
| --------------------------------- | -------- | -------------------------------------------------------------------------------------------------------------------------- |
| Phase 1: Requirements Analysis    | COMPLETE | 8 stories analyzed, all within flagzen-core, no new modules                                                                |
| Phase 2: Existing System Analysis | COMPLETE | Codebase examined -- FeatureDispatcher, FlagProvider, DefaultFeatureDispatcher, ProxyGenerator identified for modification |
| Phase 3: Constraint Analysis      | COMPLETE | Zero reflection maintained, backward compat required, Java 17+ minimum                                                     |
| Phase 4: Architecture Design      | COMPLETE | Context resolution chain, proxy evolution, SPI additions, 2 ADRs, updated C4 L3 diagram                                    |
| Phase 5: Quality Validation       | COMPLETE | All quality gates passed                                                                                                   |
| Phase 6: Peer Review              | COMPLETE | Self-reviewed against critique dimensions                                                                                  |

## Architecture Decisions Made

### Decided in DESIGN Wave

|  #  |                            Decision                            |     ADR      |                               Rationale                                |
| --- | -------------------------------------------------------------- | ------------ | ---------------------------------------------------------------------- |
| 1   | Fixed resolution order: explicit > accessor > scoped > default | ADR-011      | Specificity principle, simplicity over configurability                 |
| 2   | ThreadLocal R1, ScopedValue R2 with runtime detection          | ADR-012      | Correctness first (R1), optimization second (R2)                       |
| 3   | Context flows through FlagContext, not proxy state             | Architecture | Proxies remain stateless and cacheable as singletons                   |
| 4   | ContextResolver as internal class in com.flagzen.internal      | Architecture | Encapsulates chain logic, not public API                               |
| 5   | FlagProvider context method as default (not abstract)          | Architecture | Backward compat -- existing providers compile unchanged                |
| 6   | FeatureDispatcher context method as abstract (not default)     | Architecture | FlagZen-owned interface, not user SPI; compile error is correct signal |
| 7   | FlagContext.current() is package-private/internal              | Architecture | Not public API; only consumed by generated proxies and dispatcher      |

### Confirmed from DISCUSS Wave

|  #  |                       Decision                       |   Source   |
| --- | ---------------------------------------------------- | ---------- |
| 1   | Resolution order is fixed, not configurable          | DISCUSS D7 |
| 2   | EvaluationContext performs no validation             | DISCUSS D8 |
| 3   | Two-release split (R1: ThreadLocal, R2: ScopedValue) | DISCUSS D5 |
| 4   | ContextAccessor SPI defined but not implemented      | DISCUSS D6 |

## Artifacts Produced

|       Artifact       |                      File                      |  Status   |
| -------------------- | ---------------------------------------------- | --------- |
| Architecture Design  | `design/architecture-design.md`                | Complete  |
| Data Models          | `design/data-models.md`                        | Complete  |
| Component Boundaries | `design/component-boundaries.md`               | Complete  |
| Wave Decisions       | `design/wave-decisions.md`                     | This file |
| ADR-011              | `adrs/ADR-011-context-resolution-order.md`     | Accepted  |
| ADR-012              | `adrs/ADR-012-flagcontext-carrier-strategy.md` | Accepted  |

## Handoff Package for DISTILL Wave (acceptance-designer)

### What the acceptance-designer receives

1. **Architecture design**: Context resolution chain, proxy evolution strategy, FlagProvider SPI evolution, FlagContext design, ContextAccessor SPI, updated C4 L3 diagram
2. **Component boundaries**: 3 new types + 1 internal type + 4 modified types, all within flagzen-core
3. **Data models**: EvaluationContext (immutable, builder), FlagContext (static utility), ContextResolver (internal), ContextAccessor (SPI)
4. **ADRs**: ADR-011 (resolution order), ADR-012 (carrier strategy)
5. **User stories**: 8 stories from DISCUSS wave (US-EC-01 through US-EC-08) with BDD scenarios and acceptance criteria

### What the acceptance-designer should produce

1. Acceptance tests for each BDD scenario in the 8 user stories
2. Resolution order exhaustive test matrix (all 5 permutations from US-EC-07)
3. FlagContext nesting and cleanup tests
4. Backward compatibility verification tests (existing resolve(Class) and FlagProvider.getString(key))
5. EvaluationContext immutability and builder contract tests
6. ContextAccessor priority ordering and empty-result fallthrough tests

### Development paradigm

**OOP (Java)**. Consistent with M0.

### External integrations annotation

No new external integrations in this milestone. Provider adapters (LaunchDarkly, OpenFeature, Togglz) will add context-aware `getString(key, context)` overrides in their respective milestones. Contract test recommendation from M0 remains applicable.

## Quality Gate Status

- [x] Requirements traced to components (Section 14 of architecture-design.md)
- [x] Component boundaries with clear responsibilities (component-boundaries.md)
- [x] Technology choices in ADRs with alternatives (ADR-011, ADR-012)
- [x] Quality attributes addressed (maintainability, testability, performance, backward compat)
- [x] Dependency-inversion compliance (ContextAccessor SPI in com.flagzen.spi, implementations external)
- [x] C4 diagrams (L3 updated with new components)
- [x] Integration patterns specified (ServiceLoader for ContextAccessor, default method for FlagProvider)
- [x] OSS preference validated (no new dependencies)
- [x] AC behavioral, not implementation-coupled
- [x] External integrations annotated (no new integrations; existing annotation unchanged)
- [x] Architectural enforcement tooling recommended (ArchUnit rules extended)
- [x] Peer review completed

## Risk Register

|                                 Risk                                 | Probability | Impact |                          Mitigation                           |
| -------------------------------------------------------------------- | ----------- | ------ | ------------------------------------------------------------- |
| ScopedValue preview status on Java 21-24 causes compatibility issues | Medium      | Low    | R2 only; ThreadLocal fallback always works; detection is safe |
| ProxyGenerator changes break existing generated proxies              | Low         | High   | Recompilation required (documented); existing tests validate  |
| FlagContext ThreadLocal not cleaned up on exception                  | Low         | Medium | Design requires finally-block cleanup; acceptance tests cover |
| ContextAccessor ServiceLoader adds startup cost                      | Low         | Low    | Discovery once at dispatcher construction, cached             |
