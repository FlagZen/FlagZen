# Wave Decisions: DISTILL -- flagzen-multi-value-variant (M13)

## Decision 1: Feature Scope

**Core feature** -- extends annotation processor in flagzen-core. All acceptance tests exercise compile-time and runtime behavior of the annotation processor and generated proxies.

## Decision 2: Test Framework

**Cucumber -- Java BDD** (project standard). Feature files in `tests/acceptance/flagzen-multi-value-variant/`. Step definitions in `com.flagzen.acceptance.steps`. The feature path must be added to `cucumber.features` in `build.gradle.kts` during DELIVER.

## Decision 3: Integration Approach

**Real services** -- compile-testing library for annotation processor scenarios (same pattern as `flagzen-typed-variants`). InMemoryFlagProvider + DefaultFeatureDispatcher for runtime dispatch scenarios.

## Decision 4: Infrastructure Testing

**No** -- functional acceptance tests only. No infrastructure or environment-specific tests.

## Decision 5: Step Definition Strategy

**New step class `MultiValueVariantSteps`** for M13-specific steps. This avoids pattern collisions with existing `CompileTimeSteps` and `TypeAnnotationSteps` (per feedback in memory about DuplicateStepDefinitionException risk). Multi-value-specific step patterns like "for string values X and Y" and "for int values X and Y" are distinct from existing patterns.

Reuse from existing steps:

- `CompileTimeSteps`: "the project compiles", "compilation succeeds", "compilation fails"
- `TypeAnnotationSteps`: feature setup with type (INT, LONG, DOUBLE)

New steps needed:

- Compile-time: multi-value variant definitions (string array, int array, long array, @CloseTo array/overlap)
- Runtime: multi-value dispatcher setup, multi-value fixture resolution

## Decision 6: Walking Skeleton Count

**2 skeletons** -- one compile-time (annotation compiles, proxy generated), one runtime (dispatch works). See `walking-skeleton.md` for rationale.

## Decision 7: @pending Tag Strategy

Walking skeleton scenarios are NOT tagged @pending (run immediately). All other scenarios ARE tagged @pending (enabled one-at-a-time during DELIVER). This matches the project's `cucumber.filter.tags=not @pending` configuration.

## Decision 8: DEVOPS Graceful Degradation

DEVOPS artifacts not created for this feature. Default environments assumed. Dim 8 Check B skipped (no environment-specific preconditions apply to a compile-time annotation processor feature). Logged as warning.

## Artifacts Produced

| Artifact | Path |
| --- | --- |
| Walking skeleton feature | `tests/acceptance/flagzen-multi-value-variant/walking-skeleton.feature` |
| Milestone 1: annotation schema | `tests/acceptance/flagzen-multi-value-variant/milestone-1-annotation-schema.feature` |
| Milestone 2: duplicate detection | `tests/acceptance/flagzen-multi-value-variant/milestone-2-duplicate-detection.feature` |
| Milestone 3: @CloseTo overlap | `tests/acceptance/flagzen-multi-value-variant/milestone-3-closeto-overlap.feature` |
| Milestone 4: runtime dispatch | `tests/acceptance/flagzen-multi-value-variant/milestone-4-runtime-dispatch.feature` |
| Test scenario inventory | `docs/feature/flagzen-multi-value-variant/distill/test-scenarios.md` |
| Walking skeleton rationale | `docs/feature/flagzen-multi-value-variant/distill/walking-skeleton.md` |
| Wave decisions | `docs/feature/flagzen-multi-value-variant/distill/wave-decisions.md` (this file) |
