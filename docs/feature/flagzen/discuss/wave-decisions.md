# Wave Decisions -- FlagZen DISCUSS

## Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Wave**: DISCUSS (product-owner)
- **Prior wave**: DISCOVER (product-discoverer) -- GO decision, all gates passed

## DISCUSS Summary

|             Phase              |  Status  |                                         Key Output                                         |
| ------------------------------ | -------- | ------------------------------------------------------------------------------------------ |
| Phase 2: Journey Visualization | COMPLETE | DX journey (7 steps), emotional arc (curious to evangelical), 3 artifact formats           |
| Phase 2.5: Story Mapping       | COMPLETE | Story map with backbone (7 activities), walking skeleton (5 stories), 4 release slices     |
| Phase 2.7: Scope Assessment    | PASS     | 9 stories in R1, 1 primary bounded context, estimated 2 weeks                              |
| Phase 3: Coherence Validation  | PASS     | Shared artifacts registry (11 artifacts), 5 integration checkpoints, consistent vocabulary |
| Phase 4: Requirements Crafting | COMPLETE | 9 user stories, all DoR passed, outcome KPIs defined                                       |
| Phase 5: Peer Review           | APPROVED | 0 critical, 0 high, 2 medium (accepted)                                                    |

## Decision: PROCEED TO DESIGN WAVE

## Artifacts Produced

|         Artifact          |                   File                    |          Status          |
| ------------------------- | ----------------------------------------- | ------------------------ |
| Journey Visual            | `journey-developer-integration-visual.md` | Complete                 |
| Journey Schema            | `journey-developer-integration.yaml`      | Complete                 |
| Journey Gherkin           | `journey-developer-integration.feature`   | Complete                 |
| Shared Artifacts Registry | `shared-artifacts-registry.md`            | Complete                 |
| Story Map                 | `story-map.md`                            | Complete                 |
| Prioritization            | `prioritization.md`                       | Complete                 |
| User Stories (9)          | `user-stories.md`                         | Complete, all DoR passed |
| Outcome KPIs              | `outcome-kpis.md`                         | Complete                 |
| DoR Validation            | `dor-validation.md`                       | All 9 PASSED             |
| Peer Review               | `peer-review.md`                          | APPROVED                 |
| Wave Decisions            | `wave-decisions.md`                       | This file                |

## Handoff Package for DESIGN Wave (solution-architect)

### What the solution-architect receives

1. **Journey artifacts**: Complete DX journey from "discover library" to "Spring integration" with emotional arc, error paths, and integration points
2. **Story map**: 9 stories in Release 1 (walking skeleton + core), 5 stories in Release 2 (production readiness), clear dependency chain
3. **User stories**: 9 fully specified stories with BDD scenarios, acceptance criteria, real domain examples, and technical notes
4. **Shared artifacts registry**: 11 tracked artifacts with sources, consumers, and integration risks
5. **Outcome KPIs**: Eminence-focused metrics (stars, blog posts, conference talks) + technical excellence metrics (zero reflection, annotation processor correctness)

### What the solution-architect should decide

1. **Annotation processor implementation**: JavaPoet vs. string templates vs. alternative code generation approach
2. **Proxy pattern**: Java Proxy, code-generated concrete class, or ByteBuddy
3. **Module structure**: Confirm flagzen-core, flagzen-test, flagzen-env, flagzen-spring as separate Gradle modules
4. **FlagProvider SPI design**: Exact interface contract, typed vs. string-only methods
5. **Generated code packaging**: Same package as @Feature interface, separate generated-sources directory
6. **Build tool integration**: Annotation processor auto-discovery mechanism
7. **Thread safety strategy**: For proxy, dispatcher, and flag provider

### What is explicitly NOT decided (solution-neutral)

- Technology choices (these belong in DESIGN wave)
- Architecture patterns beyond the SPI contract concept
- Build tool configuration details
- Internal data structures for the annotation processor
- Specific Java version features to use (records, sealed classes, etc.)

## Release Plan

|          Release          |              Stories              | Estimated Effort |                       Target Outcome                        |
| ------------------------- | --------------------------------- | ---------------- | ----------------------------------------------------------- |
| Walking Skeleton          | US-01, US-02, US-04, US-05, US-07 | 1 week           | End-to-end dispatch + testing works                         |
| Release 1 (Core)          | + US-03, US-06, US-08, US-09      | +1 week          | Complete type-safe dispatch with all validation and testing |
| Release 2 (Production)    | US-10 through US-14               | 1-2 weeks        | Spring integration, env var provider, evaluation context    |
| Release 3 (Ecosystem)     | TBD                               | TBD              | Provider adapters, CDI/Quarkus                              |
| Release 4 (Observability) | TBD                               | TBD              | Usage stats, dead flag detection                            |

## Risk Register

|                             Risk                             | Probability |    Impact     |                            Mitigation                             |
| ------------------------------------------------------------ | ----------- | ------------- | ----------------------------------------------------------------- |
| Annotation processor complexity exceeds estimate             | Medium      | High          | US-04 is in walking skeleton -- derisked early; spike if needed   |
| Cross-module @Variant discovery not feasible at compile time | Low         | Medium        | Accepted: runtime startup validation for cross-module (Release 2) |
| Proxy runtime switching confuses developers                  | Medium      | Medium        | Clear documentation, optional static resolution mode              |
| Java 17+ floor limits adoption                               | Low         | Low           | Brief specifies Java 17+; modern Java is the target audience      |
| Solo maintainer sustainability                               | Medium      | Low (for MVP) | Modular design enables community ownership of extensions          |

## Open Questions (for solution-architect)

1. Should the annotation processor generate one proxy per @Feature or one registry class containing all dispatch logic?
2. Should the FlagProvider SPI include a `subscribe(String key, Consumer<String> listener)` method for reactive flag changes, or should that be an extension SPI?
3. Should generated proxies be package-private or public? (Affects testability of generated code)
4. Should `FeatureDispatcher` be an interface (for testability) or a final class (for simplicity)?
