# DESIGN Wave Handoff: Condition Predicates (flagzen-conditions)

## Handoff Summary

Feature flagzen-conditions (M6) is ready for DESIGN wave. All 8 stories pass the 9-item Definition of Ready. Peer review approved after 2 iterations.

## Artifacts Produced

| Artifact | Path | Purpose |
|----------|------|---------|
| Journey Visual | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates-visual.md` | ASCII flow + emotional arc + error paths |
| Journey YAML | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates.yaml` | Structured journey schema |
| Journey Gherkin | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates.feature` | Testable acceptance scenarios (22 scenarios) |
| Story Map | `docs/feature/flagzen-conditions/discuss/story-map.md` | Backbone + walking skeleton + release slices |
| Prioritization | `docs/feature/flagzen-conditions/discuss/prioritization.md` | Release priority + backlog suggestions |
| User Stories | `docs/feature/flagzen-conditions/discuss/user-stories.md` | 8 LeanUX stories with BDD scenarios |
| Shared Artifacts | `docs/feature/flagzen-conditions/discuss/shared-artifacts-registry.md` | Integration points + consistency validation |
| Outcome KPIs | `docs/feature/flagzen-conditions/discuss/outcome-kpis.md` | Measurable outcomes per story |
| DoR Validation | `docs/feature/flagzen-conditions/discuss/dor-validation.md` | All 8 stories PASSED |
| Peer Review | `docs/feature/flagzen-conditions/discuss/peer-review.md` | Approved (iteration 2) |

## Key Design Decisions for Solution Architect

### 1. FeaturePredicate is not generic

`FeaturePredicate` has a fixed `boolean test(EvaluationContext ctx)` signature. Not `Predicate<T>`. This is deliberate: the evaluation input is always EvaluationContext, keeping the contract simple and the annotation processor validation straightforward.

### 2. @Condition is nested inside @Variant

`@Condition` is not a standalone annotation. It lives inside `@Variant(when = @Condition(...))`. This keeps the variant declaration self-contained and avoids the need for cross-annotation matching.

### 3. Value-based and condition-based dispatch are mutually exclusive per feature

A @Feature cannot mix `@Variant("value")` and `@Variant(when = @Condition(...))`. The processor rejects this. Each feature uses one dispatch mode.

### 4. REQUIRED strategy on conditions demands @DefaultVariant

Since predicate completeness cannot be verified statically (unlike enum coverage), `FallbackStrategy.REQUIRED` on condition-based features requires `@DefaultVariant` at compile time.

### 5. Predicate instantiation: no-arg constructor (Spring DI as extension)

Core module instantiates predicates via no-arg constructor. Spring module (US-CP-08, Could Have) resolves @Component predicates from ApplicationContext. This keeps core reflection-free.

### 6. Predicates evaluated per method call

The generated proxy re-evaluates predicates on each method call, consistent with how value-based dispatch re-queries FlagProvider on each call. EvaluationContext comes from the resolution chain (explicit > accessor > scoped > default).

### 7. Exception propagation

Predicates are user code. FlagZen does not catch or wrap exceptions from `test()`. They propagate directly to the caller.

## Dependencies

| Dependency | Status | Impact |
|-----------|--------|--------|
| M1: EvaluationContext (US-EC-01) | NOT STARTED | BLOCKING -- FeaturePredicate.test() takes EvaluationContext |
| M0: FallbackStrategy | DONE | NONE -- reused unchanged |
| M0: Annotation Processor | DONE | EXTEND -- add @Condition processing |
| M0: ProxyGenerator | DONE | EXTEND -- add predicate dispatch path |
| M4: flagzen-spring | NOT STARTED | US-CP-08 only -- deferrable |

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| M1 EvaluationContext API changes during M1 development | Medium | High | FeaturePredicate depends on EvaluationContext; coordinate M1 completion first |
| Annotation processor complexity | Low | Medium | Condition processing is additive, not modifying existing validation |
| Proxy generation complexity | Medium | Medium | Two dispatch modes in one proxy class; consider dispatching mode flag at construction time |
| Performance of predicate evaluation | Low | Low | Predicates are user responsibility; document best practice |

## Delivery Recommendation

1. Complete M1 (EvaluationContext) before starting M6
2. Implement R1 (Walking Skeleton: US-CP-01, 02, 04, 06) first for end-to-end validation
3. R2 (US-CP-03, 05) adds remaining compile-time safety
4. R3 (US-CP-07) handles fallback edge cases
5. R4 (US-CP-08) only after M4 (flagzen-spring) is available
