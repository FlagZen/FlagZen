# Wave Decisions -- flagzen-typed-variants DISTILL

## Context

- **Feature ID**: flagzen-typed-variants
- **Date**: 2026-03-27
- **Wave**: DISTILL (acceptance-designer)
- **Prior wave**: DESIGN (solution-architect) -- APPROVED, 2 ADRs (013, 014), all quality gates passed

## DISTILL Summary

|               Phase               |  Status  |                                   Key Output                                   |
| --------------------------------- | -------- | ------------------------------------------------------------------------------ |
| Phase 1: Context Understanding    | COMPLETE | 8 user stories analyzed, 4 driving ports identified, domain language extracted |
| Phase 2: Scenario Design          | COMPLETE | 71 scenarios across 5 feature files, 55% error/edge/boundary coverage          |
| Phase 3: Test Infrastructure      | COMPLETE | Feature files created, @pending tags on all except walking skeleton scenarios  |
| Phase 4: Peer Review + Validation | COMPLETE | Review pass completed, all 8 dimensions passed                                 |

## Decisions Made

|  #  |                                     Decision                                     |                                       Rationale                                        |
| --- | -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| 1   | Test framework: Cucumber-JVM with cucumber-java + cucumber-junit-platform-engine | User-specified. Consistent with M0 and M1 acceptance tests.                            |
| 2   | Integration approach: Real compilation, real generated code, real proxies        | User-specified. Annotation processor and generated code ARE the SUT.                   |
| 3   | 5 feature files organized by delivery milestone                                  | Aligns with story map: annotations, validation, dispatch, conditional API.             |
| 4   | Walking skeleton covers US-M2-01/02, US-M2-05, US-M2-03/06 (3 scenarios)         | Thinnest E2E slices: compile typed feature, dispatch typed value, boolean convenience. |
| 5   | @pending tag on all non-skeleton scenarios                                       | One-at-a-time enablement. Walking skeleton scenarios enabled first.                    |
| 6   | 1 @property-tagged scenario for property-based testing                           | Identified universal invariant: every type mismatch has actionable fix.                |
| 7   | Compile-time tests: javax.tools.JavaCompiler API or google/compile-testing       | For scenarios asserting compilation success/failure and compiler error messages.       |
| 8   | Runtime tests: real InMemoryFlagProvider, real FeatureDispatcher, real proxies   | No mocks. Tests exercise the actual library API through driving ports.                 |
| 9   | Business language: "tolerance" instead of "delta" in Gherkin                     | "tolerance" is domain-accessible; "delta" is mathematical jargon.                      |
| 10  | No infrastructure testing                                                        | User-specified. Library feature, no infrastructure concerns.                           |

## DEVOPS Warning

DEVOPS artifacts not found at `docs/feature/flagzen-typed-variants/devops/environments.yaml`. Using default environment assumptions: clean project setup. Environment matrix testing (Dim 8, Check B) deferred.

## Artifacts Produced

|         Artifact         |                                       File                                       |  Status   |
| ------------------------ | -------------------------------------------------------------------------------- | --------- |
| Walking skeleton feature | `tests/acceptance/flagzen-typed-variants/walking-skeleton.feature`               | Complete  |
| Milestone 1 feature      | `tests/acceptance/flagzen-typed-variants/milestone-1-type-annotations.feature`   | Complete  |
| Milestone 2 feature      | `tests/acceptance/flagzen-typed-variants/milestone-2-compile-validation.feature` | Complete  |
| Milestone 3 feature      | `tests/acceptance/flagzen-typed-variants/milestone-3-typed-dispatch.feature`     | Complete  |
| Milestone 4 feature      | `tests/acceptance/flagzen-typed-variants/milestone-4-conditional-api.feature`    | Complete  |
| Test scenarios doc       | `docs/feature/flagzen-typed-variants/distill/test-scenarios.md`                  | Complete  |
| Walking skeleton doc     | `docs/feature/flagzen-typed-variants/distill/walking-skeleton.md`                | Complete  |
| Wave decisions doc       | `docs/feature/flagzen-typed-variants/distill/wave-decisions.md`                  | This file |

## Driving Ports (Mandate 1 Compliance)

Tests invoke through these driving ports only:

|     Driving Port     |                                 Test Usage                                  |
| -------------------- | --------------------------------------------------------------------------- |
| Annotation Processor | Invoked via compile-testing (javac API). Validates compile success/failure. |
| FeatureDispatcher    | Runtime resolution of typed features to proxy instances.                    |
| FlagProvider SPI     | Typed accessor methods (`getInt`, `getBoolean`, `getLong`, `getDouble`).    |
| FlagContext          | Block-scoped context for typed dispatch scenarios.                          |

Internal components (FeatureModel, VariantModel, ProxyGenerator) are exercised indirectly through these ports.

## Mandate Compliance Evidence

- **CM-A**: All scenarios invoke through annotation processor (compile), FeatureDispatcher (resolve), or FlagProvider (read). Zero direct internal component access.
- **CM-B**: Gherkin uses business terms only: "feature", "variant", "type", "tolerance", "flag value", "dispatch", "resolve". Zero technical terms (no HTTP, JSON, database, API, Map, Supplier references).
- **CM-C**: 3 walking skeletons (user-centric E2E) + 68 focused scenarios (boundary tests). Walking skeletons pass litmus test (user goals, observable outcomes, stakeholder-confirmable).
- **CM-D**: Pure function extraction not applicable -- this is a library with compile-time annotation processing. Business logic is in the annotation processor (compile-time) and generated proxies (runtime). No fixture parametrization needed.

## Handoff Package for DELIVER Wave (software-crafter)

### What the software-crafter receives

1. **Walking skeleton**: 3 scenarios identifying the thinnest typed dispatch slices
2. **Implementation sequence**: skeleton 1 (compile) -> skeleton 2 (INT dispatch) -> skeleton 3 (BOOLEAN dispatch) -> milestone 1 -> 2 -> 3 -> 4
3. **71 acceptance scenarios** organized by milestone with @pending tags
4. **1 @property scenario** for property-based testing (type mismatch fix suggestions)
5. **Mandate compliance evidence** (CM-A/B/C/D)

### Implementation guidance

- Enable one @pending scenario at a time
- Walking skeleton scenarios are enabled (no @pending tag) -- start here
- Compile-time scenarios use javax.tools.JavaCompiler API or google/compile-testing
- Runtime scenarios use real InMemoryFlagProvider and FeatureDispatcher
- Context scenarios reuse M1 context infrastructure (EvaluationContext, FlagContext, ContextAccessor)

## Quality Gate Status

- [x] All 8 user stories have scenario coverage (traceability verified)
- [x] Error + edge + boundary ratio: 55% (exceeds 40% target)
- [x] Walking skeleton litmus test passed (3 skeletons, user-centric framing)
- [x] Business language purity verified (zero technical terms in Gherkin)
- [x] Hexagonal boundary enforcement verified (driving ports only)
- [x] One-at-a-time enablement configured (@pending on all non-skeleton scenarios)
- [x] Property-shaped criteria tagged (@property on universal invariant)
- [x] Peer review completed (8 dimensions evaluated)
