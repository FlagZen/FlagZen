# Wave Decisions: Evaluation Context DISTILL

## Context

- **Feature ID**: flagzen-eval-context
- **Date**: 2026-03-27
- **Wave**: DISTILL (acceptance-designer)
- **Prior waves**: DISCUSS (8 user stories), DESIGN (architecture, component boundaries, 2 ADRs)

## Decisions

### D1: Three Walking Skeletons

**Decision**: Three walking skeletons covering the three major vertical slices: explicit context resolution (US-EC-01/02/03), block-scoped context (US-EC-05), and resolution order (US-EC-07/06).

**Rationale**: Each skeleton delivers independently observable developer value. The three slices map cleanly to the three usage patterns: explicit pass, block scope, automatic resolution. This matches the story map's backbone structure.

### D2: Immutability Not Tested in Gherkin

**Decision**: EvaluationContext immutability is not an acceptance scenario. Per user instruction, this is an architectural constraint verified by unit test (ArchUnit or direct assertion on no-setter/unmodifiable-collections).

**Rationale**: Immutability is not an observable behavior from the user's perspective -- it is a design constraint. Testing it at the acceptance level would violate Mandate 2 (business language) and Mandate 3 (user journey completeness).

### D3: US-EC-08 Acceptance Tested as Behavioral Equivalence

**Decision**: US-EC-08 (ScopedValue/ThreadLocal carrier) has one acceptance scenario that verifies identical behavior regardless of carrier mechanism. Carrier detection internals are not tested at the acceptance level.

**Rationale**: The carrier is an internal optimization. The user-observable behavior is identical between ThreadLocal and ScopedValue. Testing which carrier is active would violate Mandate 1 (hexagonal boundary enforcement) by testing internals.

### D4: Real Integration, No Mocks

**Decision**: All acceptance tests use real EvaluationContext, real FeatureDispatcher, real FlagProvider implementations. Test doubles are custom FlagProvider implementations (e.g., context-aware providers that check targeting key), not mock frameworks.

**Rationale**: Per user specification. FlagProvider implementations used in tests are simple in-memory adapters that implement the SPI contract. This validates the full resolution chain without external dependencies.

### D5: Default Environment Matrix (DEVOPS Missing)

**Decision**: DEVOPS artifacts not available. Using default environment matrix: clean, with-pre-commit, with-stale-config.

**Warning**: Environment-specific Given clauses in walking skeletons are not applicable for a library project. The walking skeletons test API behavior, not environment-specific deployment scenarios.

### D6: Error Path Strategy

**Decision**: Error paths focus on: null handling (null context, null in scoped block), cleanup after exceptions (scoped block), empty accessor fallthrough, missing accessor graceful handling, and backward compatibility edge cases. Total non-happy-path ratio: 44%.

**Rationale**: This is a strongly-typed Java library. Many error paths are prevented at compile time (type mismatches, missing method implementations). Runtime error scenarios focus on null safety, resource cleanup, and fallback behavior.

### D7: Property-Shaped Scenarios

**Decision**: Three scenarios tagged `@property`: thread isolation for scoped context, resolution order determinism, and thread-safety of EvaluationContext reads. These signal the DELIVER wave crafter to implement as property-based tests.

**Rationale**: Thread safety and resolution order determinism are universal invariants ("for any context, for any thread") that benefit from generative testing rather than single-example assertions.

## Mandate Compliance Evidence

### CM-A: Hexagonal Boundary Enforcement

All scenarios invoke through driving ports only:

- `EvaluationContext.builder()` -- public API
- `FeatureDispatcher.resolve(Class)` and `resolve(Class, EvaluationContext)` -- driving port
- `FlagContext.run(EvaluationContext, Runnable/Supplier)` -- public API
- `FlagZen.configure()` -- public API factory

No scenarios import or test: `ContextResolver` (internal), `DefaultFeatureDispatcher` (internal), `ProxyGenerator` (compile-time).

### CM-B: Business Language Purity

Gherkin uses domain terms only: evaluation context, targeting key, attributes, feature, variant, flag provider, resolve, scoped context, context accessor, resolution order. Zero technical terms (no HTTP, JSON, ThreadLocal, ScopedValue, ServiceLoader, etc. in Gherkin).

### CM-C: Walking Skeleton + Focused Scenario Counts

- Walking skeletons: 3 (user-centric, demo-able)
- Focused scenarios: 33 (boundary tests through driving ports)
- Total: 36

### CM-D: Pure Function Extraction

Not applicable at acceptance test design phase. The DELIVER wave crafter will extract pure functions from resolution chain logic during implementation. The acceptance tests invoke through driving ports only, so pure function extraction is transparent to the acceptance layer.

## Artifacts Produced

|            Artifact            |                                       File                                       |  Status   |
| ------------------------------ | -------------------------------------------------------------------------------- | --------- |
| Walking skeleton scenarios     | `tests/acceptance/flagzen-eval-context/walking-skeleton.feature`                 | Complete  |
| Context model scenarios        | `tests/acceptance/flagzen-eval-context/milestone-1-context-model.feature`        | Complete  |
| Block-scoped context scenarios | `tests/acceptance/flagzen-eval-context/milestone-2-block-scoped-context.feature` | Complete  |
| Context resolution scenarios   | `tests/acceptance/flagzen-eval-context/milestone-3-context-resolution.feature`   | Complete  |
| Test scenarios inventory       | `docs/feature/flagzen-eval-context/distill/test-scenarios.md`                    | Complete  |
| Walking skeleton documentation | `docs/feature/flagzen-eval-context/distill/walking-skeleton.md`                  | Complete  |
| Wave decisions                 | `docs/feature/flagzen-eval-context/distill/wave-decisions.md`                    | This file |

## Handoff to DELIVER Wave (software-crafter)

### Implementation Sequence

1. **Enable walking-skeleton.feature Scenario 1** -- implement EvaluationContext builder, FeatureDispatcher.resolve(Class, EvaluationContext), FlagProvider.getString(key, context) default method
2. **Enable walking-skeleton.feature Scenario 2** -- implement FlagContext.run()
3. **Enable walking-skeleton.feature Scenario 3** -- implement ContextAccessor SPI, ContextResolver, resolution order
4. **Enable milestone-1 scenarios one at a time** -- complete US-EC-01/02/03/04 edge cases
5. **Enable milestone-2 scenarios one at a time** -- complete US-EC-05/08 edge cases
6. **Enable milestone-3 scenarios one at a time** -- complete US-EC-06/07 edge cases and resolution order matrix

### One-at-a-Time Protocol

All scenarios except the first walking skeleton are marked for skip/ignore. Enable one, implement until green, commit, enable next.
