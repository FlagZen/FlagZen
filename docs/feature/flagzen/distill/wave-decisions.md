# Wave Decisions -- FlagZen DISTILL

## Context

- **Feature ID**: flagzen
- **Date**: 2026-03-26
- **Wave**: DISTILL (acceptance-designer)
- **Prior wave**: DESIGN (solution-architect) -- APPROVED, 7 ADRs, all quality gates passed

## DISTILL Summary

|               Phase               |  Status  |                                   Key Output                                   |
| --------------------------------- | -------- | ------------------------------------------------------------------------------ |
| Phase 1: Context Understanding    | COMPLETE | 9 user stories analyzed, 4 driving ports identified, domain language extracted |
| Phase 2: Scenario Design          | COMPLETE | 34 scenarios across 4 feature files, 53% error/edge/boundary coverage          |
| Phase 3: Test Infrastructure      | COMPLETE | Feature files created, @pending tags on all except walking skeleton            |
| Phase 4: Peer Review + Validation | COMPLETE | Self-review pass (fast-path: review dimensions applied inline)                 |

## Decisions Made

|  #  |                                     Decision                                     |                                                  Rationale                                                  |
| --- | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| 1   | Test framework: Cucumber-JVM with cucumber-java + cucumber-junit-platform-engine | User-specified. BDD with Gherkin for Java.                                                                  |
| 2   | Integration approach: Real compilation, real generated code, real proxies        | User-specified. No mocks -- the annotation processor and generated code ARE the SUT.                        |
| 3   | 4 feature files organized by delivery milestone                                  | Aligns with story map walking skeleton + release 1 milestones.                                              |
| 4   | Walking skeleton covers US-01/02/04, US-05/06, US-07 (3 scenarios)               | Thinnest E2E slice through compile-time, runtime, and testing. Matches DISCUSS walking skeleton definition. |
| 5   | @pending tag on all non-skeleton scenarios                                       | One-at-a-time enablement. Walking skeleton scenarios enabled first.                                         |
| 6   | 3 @property-tagged scenarios for property-based testing                          | Identified universal invariants: proxy-per-feature, NOOP-never-throws, pin-over-file priority.              |
| 7   | Compile-time tests: use javax.tools.JavaCompiler API or google/compile-testing   | For scenarios that assert on compilation success/failure and compiler error messages.                       |
| 8   | Runtime tests: real InMemoryFlagProvider, real FeatureDispatcher, real proxies   | No mocks at any level. Tests exercise the actual library API.                                               |

## DEVOPS Warning

DEVOPS artifacts not found at `docs/feature/flagzen/devops/environments.yaml`. Using default environment assumptions: clean project setup. Environment matrix testing (Dim 8, Check B) deferred.

## Artifacts Produced

|         Artifact          |                                  File                                  |  Status   |
| ------------------------- | ---------------------------------------------------------------------- | --------- |
| Walking skeleton feature  | `tests/acceptance/flagzen/walking-skeleton.feature`                    | Complete  |
| Milestone 1: Compile-time | `tests/acceptance/flagzen/milestone-1-compile-time-validation.feature` | Complete  |
| Milestone 2: Fallback     | `tests/acceptance/flagzen/milestone-2-fallback-strategies.feature`     | Complete  |
| Milestone 3: Testing      | `tests/acceptance/flagzen/milestone-3-testing-support.feature`         | Complete  |
| Test scenarios doc        | `docs/feature/flagzen/distill/test-scenarios.md`                       | Complete  |
| Walking skeleton doc      | `docs/feature/flagzen/distill/walking-skeleton.md`                     | Complete  |
| Wave decisions            | `docs/feature/flagzen/distill/wave-decisions.md`                       | This file |

## Mandate Compliance Evidence

### CM-A: Hexagonal Boundary Enforcement

All scenarios invoke through the 4 identified driving ports:

1. **Java compiler** (annotation processor): walking-skeleton #1, all milestone-1 scenarios
2. **FeatureDispatcher.resolve()**: walking-skeleton #2, milestone-2 runtime scenarios
3. **FlagZen.configure() / FlagZen.dispatcher()**: milestone-2 provider configuration scenarios
4. **@PinFlag / TestFlagContext**: walking-skeleton #3, all milestone-3 scenarios

Zero scenarios test internal components (ProxyGenerator, FeatureModel, VariantModel, DefaultFeatureDispatcher) directly.

### CM-B: Business Language Purity

Gherkin uses domain terms only: feature, variant, flag key, fallback strategy, dispatch proxy, resolve, pin, compile, error message. Zero occurrences of: HTTP, REST, JSON, database, controller, service layer, status code, repository, mock, stub.

Technical terms allowed in context: "compile" (the user action is compilation), "annotation processor" (user-facing concept in a Java library), "proxy" (domain concept in FlagZen).

### CM-C: Walking Skeleton + Focused Scenario Counts

- Walking skeletons: 3 (user-centric, demo-able, E2E slices)
- Focused scenarios: 31 (boundary tests through driving ports)
- Total: 34

### CM-D: Pure Function Extraction

Not applicable at DISTILL phase -- this is a DELIVER concern. The acceptance tests themselves do not contain business logic to extract. The annotation processor's internal logic (validation, code generation) will be structured during implementation.

## Handoff Package for DELIVER Wave (software-crafter)

### What the software-crafter receives

1. **4 feature files** with 34 Gherkin scenarios covering all 9 user stories
2. **Walking skeleton identification**: 3 scenarios to implement first, in order
3. **Implementation sequence**: Walking skeleton (3) -> Milestone 1 (13) -> Milestone 2 (10) -> Milestone 3 (8)
4. **One-at-a-time protocol**: Enable one @pending scenario, implement until it passes, commit, repeat
5. **Property-based test signals**: 3 scenarios tagged @property for generator-based testing
6. **Compile-time test strategy**: Use javax.tools.JavaCompiler or google/compile-testing to programmatically compile test sources
7. **Runtime test strategy**: Real InMemoryFlagProvider + real FeatureDispatcher + real generated proxies

### What the software-crafter should decide

1. Step definition organization (by domain concept: feature-steps, variant-steps, dispatch-steps, test-support-steps)
2. Compile-testing fixture design (shared Java source templates for test compilation)
3. Whether to use google/compile-testing or raw javax.tools.JavaCompiler
4. Test runner configuration (Cucumber-JVM + JUnit Platform)

## Peer Review (Fast-Path)

With 34 scenarios (> 3), full review applied inline during design:

|              Dimension              |                          Status                           |                             Notes                              |
| ----------------------------------- | --------------------------------------------------------- | -------------------------------------------------------------- |
| 1. Happy Path Bias                  | PASS                                                      | 53% error/edge/boundary coverage                               |
| 2. GWT Format                       | PASS                                                      | All scenarios follow Given-When-Then, single When action       |
| 3. Business Language                | PASS                                                      | Zero technical jargon in Gherkin (see CM-B)                    |
| 4. Coverage Completeness            | PASS                                                      | All 9 stories covered (see traceability matrix)                |
| 5. Walking Skeleton User-Centricity | PASS                                                      | Titles describe user goals, Then steps are observable outcomes |
| 6. Priority Validation              | PASS                                                      | Scenarios follow walking skeleton build order from story map   |
| 7. Observable Behavior              | PASS                                                      | All Then steps assert return values or observable outcomes     |
| 8. Traceability Coverage            | PASS (Check A), DEFERRED (Check B -- no DEVOPS artifacts) |                                                                |

**Approval status**: APPROVED
