# Wave Decisions -- Typed Variants DESIGN

## Context

- **Feature ID**: flagzen-typed-variants
- **Date**: 2026-03-27
- **Wave**: DESIGN (solution-architect)
- **Prior wave**: DISCUSS (product-owner) -- 8 user stories (US-M2-01 through US-M2-08), 2-release split

## DESIGN Summary

|               Phase               |  Status  |                                                Key Output                                                 |
| --------------------------------- | -------- | --------------------------------------------------------------------------------------------------------- |
| Phase 1: Requirements Analysis    | COMPLETE | 8 stories analyzed, all within flagzen-core, no new modules                                               |
| Phase 2: Existing System Analysis | COMPLETE | @Feature, @Variant, FlagProvider, FeatureModel, VariantModel, ProxyGenerator identified for modification  |
| Phase 3: Constraint Analysis      | COMPLETE | Zero reflection maintained, backward compat required, Java 17+, sentinel strategy for annotation defaults |
| Phase 4: Architecture Design      | COMPLETE | Typed dispatch strategy, FlagProvider SPI evolution, @CloseTo/@WhenTrue/@WhenFalse, 2 ADRs, updated C4 L3 |
| Phase 5: Quality Validation       | COMPLETE | All quality gates passed                                                                                  |
| Phase 6: Peer Review              | COMPLETE | Self-reviewed against critique dimensions                                                                 |

## Architecture Decisions Made

### Decided in DESIGN Wave

|  #  |                             Decision                              |   ADR/Doc    |                                Rationale                                |
| --- | ----------------------------------------------------------------- | ------------ | ----------------------------------------------------------------------- |
| 1   | Per-variant delta with 1e-10 default for @CloseTo                 | ADR-013      | Per-variant is strictly more expressive; default covers IEEE 754 errors |
| 2   | @WhenTrue/@WhenFalse as normalization sugar, not meta-annotations | ADR-014      | Java annotation processing lacks meta-annotation support                |
| 3   | FlagProvider typed methods as default methods parsing getString   | Architecture | Backward compat -- existing providers unchanged                         |
| 4   | DOUBLE dispatch via iteration (not map lookup)                    | Architecture | Approximate matching cannot hash; O(n) for n=2-5 is negligible          |
| 5   | Sentinel strategy for @Variant typed attributes                   | Architecture | Java annotations have no null; MIN_VALUE/empty string/NaN sentinels     |
| 6   | getBoolean strict parsing (only "true"/"false")                   | Architecture | Prevents silent misinterpretation of "1", "yes", etc.                   |
| 7   | @Variant.value default changed to empty string sentinel           | Architecture | Allows typed attributes without requiring string value                  |
| 8   | No compile-time overlap detection for DOUBLE deltas               | ADR-013      | Halting-problem-adjacent; ordering resolves ambiguity                   |

### Confirmed from DISCUSS Wave

|  #  |                         Decision                         |   Source    |
| --- | -------------------------------------------------------- | ----------- |
| 1   | FeatureType enum with STRING, INT, LONG, BOOLEAN, DOUBLE | DISCUSS D1  |
| 2   | `@Feature(type = FeatureType.X)` attribute               | DISCUSS D2  |
| 3   | Typed @Variant attributes (intValue, etc.)               | DISCUSS D3  |
| 4   | @CloseTo for approximate double matching                 | DISCUSS D4  |
| 5   | @WhenTrue/@WhenFalse with of= for multi-feature          | DISCUSS D5  |
| 6   | FlagProvider returns primitive optionals                 | DISCUSS D6  |
| 7   | Context-aware overloads for all typed methods            | DISCUSS D7  |
| 8   | Default methods parse from getString()                   | DISCUSS D8  |
| 9   | Map lookup for INT/LONG/BOOLEAN, iterate for DOUBLE      | DISCUSS D9  |
| 10  | Typed variants support order (ADR-008)                   | DISCUSS D10 |

## Artifacts Produced

|       Artifact       |                    File                    |  Status   |
| -------------------- | ------------------------------------------ | --------- |
| Architecture Design  | `design/architecture-design.md`            | Complete  |
| Data Models          | `design/data-models.md`                    | Complete  |
| Component Boundaries | `design/component-boundaries.md`           | Complete  |
| Wave Decisions       | `design/wave-decisions.md`                 | This file |
| ADR-013              | `adrs/ADR-013-closeto-delta-strategy.md`   | Accepted  |
| ADR-014              | `adrs/ADR-014-whentrue-whenfalse-sugar.md` | Accepted  |

## Handoff Package for DISTILL Wave (acceptance-designer)

### What the acceptance-designer receives

1. **Architecture design**: Typed dispatch strategy per FeatureType, FlagProvider SPI evolution, proxy generation changes, updated C4 L3 diagram
2. **Component boundaries**: 6 new types + 6+ modified types, all within flagzen-core
3. **Data models**: FeatureType enum, @CloseTo annotation, @WhenTrue/@WhenFalse, modified @Feature/@Variant, modified FeatureModel/VariantModel, FlagProvider typed methods
4. **ADRs**: ADR-013 (@CloseTo delta strategy), ADR-014 (@WhenTrue/@WhenFalse sugar)
5. **User stories**: 8 stories from DISCUSS wave (US-M2-01 through US-M2-08) with BDD scenarios and acceptance criteria

### What the acceptance-designer should produce

1. Acceptance tests for each BDD scenario in the 8 user stories
2. Type mismatch validation tests (all FeatureType x wrong attribute combinations)
3. BOOLEAN REQUIRED completeness tests (true-only, false-only, both, with/without default)
4. Duplicate typed value detection tests
5. @CloseTo delta edge cases (boundary, very small delta, very large delta, negative delta rejection)
6. @WhenTrue/@WhenFalse normalization verification (identical behavior to @Variant(booleanValue=...))
7. FlagProvider typed method parse tests (valid, invalid, absent, overflow)
8. Backward compatibility tests (existing STRING features unchanged)
9. Typed dispatch with evaluation context tests (parity with string dispatch)
10. DOUBLE dispatch ordering tests (first match wins, order attribute)

### Development paradigm

**OOP (Java)**. Consistent with M0 and M1.

### External integrations annotation

No new external integrations in this milestone. Provider adapters (LaunchDarkly, OpenFeature, Togglz) will add typed method overrides in their respective milestones. Contract test recommendation from M0 remains applicable -- should be extended to cover typed `FlagProvider` methods when adapters implement them.

## Quality Gate Status

- [x] Requirements traced to components (Section 12 of architecture-design.md)
- [x] Component boundaries with clear responsibilities (component-boundaries.md)
- [x] Technology choices in ADRs with alternatives (ADR-013, ADR-014)
- [x] Quality attributes addressed (maintainability, testability, performance, backward compat)
- [x] Dependency-inversion compliance (FlagProvider SPI evolution via default methods)
- [x] C4 diagrams (L3 updated with typed dispatch components)
- [x] Integration patterns specified (default methods on FlagProvider, processor normalization)
- [x] OSS preference validated (no new dependencies)
- [x] AC behavioral, not implementation-coupled
- [x] External integrations annotated (no new integrations; existing annotation unchanged)
- [x] Architectural enforcement tooling recommended (ArchUnit rules extended)
- [x] Peer review completed

## Risk Register

|                              Risk                               | Probability | Impact |                                     Mitigation                                     |
| --------------------------------------------------------------- | ----------- | ------ | ---------------------------------------------------------------------------------- |
| Sentinel values (MIN_VALUE) collide with real flag values       | Low         | Medium | Documented limitation; string dispatch available as workaround                     |
| @Variant attribute proliferation (5 mutually exclusive attrs)   | Medium      | Low    | Compile-time validation catches misuse; @WhenTrue/@WhenFalse simplify boolean case |
| DOUBLE delta overlap causes unexpected dispatch                 | Low         | Medium | Ordering resolves ambiguity; documented behavior                                   |
| Existing @Variant(value) users confused by empty default change | Low         | Low    | STRING features still require value; processor error message guides                |
| ProxyGenerator complexity growth (5 dispatch paths)             | Medium      | Medium | Clean separation per FeatureType; crafter structures internally                    |
