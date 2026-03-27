# Wave Decisions: Evaluation Context (flagzen-eval-context)

## Context

FlagZen is a personal/research project optimizing for technical excellence. This milestone (M1) adds evaluation context for targeted flag resolution -- a backend library feature with no UI. Decisions below were made during DISCUSS wave discovery.

## Decisions

### D1: No Walking Skeleton Required

**Decision**: Skip walking skeleton identification in story mapping.

**Rationale**: This is a library API extension, not an application with user-facing flows. All stories are in a single bounded context (flagzen-core). The dependency graph is clear and linear. A walking skeleton concept applies to systems with multiple integrating components -- here, the "skeleton" is simply the dependency chain from US-EC-01 through US-EC-07.

### D2: Lightweight UX Research

**Decision**: Lightweight developer experience (DX) analysis instead of full UX research.

**Rationale**: The "users" are Java developers interacting with a library API, not end users interacting with a UI. DX concerns are: API discoverability (Javadoc, IDE autocomplete), builder ergonomics, backward compatibility, and predictable resolution behavior. These are assessed through API design review and code examples, not UX research methodologies.

### D3: No JTBD Analysis

**Decision**: Skip Jobs-to-be-Done framework.

**Rationale**: The job is well-understood from the architecture design document and project brief: "resolve flags differently per user for A/B testing and segmentation." There are no competing jobs, no unclear motivations, and no switching decisions to analyze. JTBD adds overhead without new insight for this clearly-scoped API extension.

### D4: Backend Feature -- No TUI/CLI Mockups

**Decision**: API surface documentation (code examples) instead of TUI/CLI mockups.

**Rationale**: FlagZen is a library, not a CLI tool. The "interface" is Java API surface: method signatures, builder patterns, SPI contracts. Journey visualization uses code examples and API documentation rather than ASCII screen mockups.

### D5: Two-Release Split

**Decision**: Split into Release 1 (7 stories, core context support) and Release 2 (1 story, Java 21+ optimization).

**Rationale**: Release 1 delivers complete, correct context support on Java 17+ using ThreadLocal. Release 2 optimizes for Java 21+ virtual threads using ScopedValue. ThreadLocal correctness is sufficient for all use cases -- ScopedValue is a performance/compatibility optimization, not a correctness requirement.

### D6: ContextAccessor SPI Defined But Not Implemented

**Decision**: Define the ContextAccessor SPI in this milestone but defer implementations to M6 (reactive modules).

**Rationale**: The SPI must exist before the resolution order can be fully specified and tested. However, actual implementations (ReactorContextAccessor, MutinyContextAccessor) depend on reactive framework dependencies that belong in their own modules. Defining the SPI now ensures the contract is stable before implementors depend on it.

### D7: Resolution Order Is Fixed, Not Configurable

**Decision**: Hardcode the resolution order (explicit > accessor > scoped > default).

**Rationale**: Configurable resolution order adds complexity with minimal benefit. The hardcoded order follows the principle of specificity (most specific context wins). This matches OpenFeature's targeting key precedence model and is intuitive to Java developers familiar with configuration precedence patterns (flags > env vars > config > defaults).

### D8: EvaluationContext Performs No Validation

**Decision**: EvaluationContext accepts any targeting key (including null) and any attribute values without validation.

**Rationale**: FlagZen is a dispatch library, not a rules engine. Flag targeting rules are the responsibility of the FlagProvider (LaunchDarkly, OpenFeature, etc.). Validation in EvaluationContext would couple the context model to provider-specific constraints.

## Scope Boundaries

### In Scope (This Milestone)

- EvaluationContext model (immutable, builder)
- FeatureDispatcher.resolve(Class, EvaluationContext) overload
- FlagProvider.getString(key, context) default method
- Generated proxy context forwarding
- FlagContext.run() block-scoped context (ThreadLocal)
- ContextAccessor SPI definition
- Resolution order implementation
- ScopedValue optimization (R2)

### Out of Scope (Deferred)

- Reactive ContextAccessor implementations (M6: flagzen-reactor, flagzen-mutiny)
- Provider-specific context mapping (each provider module)
- Flag targeting rules engine (non-goal per project brief)
- EvaluationContext serialization/deserialization
- Context propagation across async boundaries (beyond ScopedValue)
- TestFlagContext integration with EvaluationContext (future enhancement)
