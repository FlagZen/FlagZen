# DESIGN Wave Handoff: Condition Predicates (flagzen-conditions)

## Handoff Summary

Feature flagzen-conditions (M6) is ready for DESIGN wave. All 8 stories pass the 9-item Definition of Ready. Peer review approved after 2 iterations.

## Artifacts Produced

|     Artifact     |                                       Path                                       |                   Purpose                    |
| ---------------- | -------------------------------------------------------------------------------- | -------------------------------------------- |
| Journey Visual   | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates-visual.md` | ASCII flow + emotional arc + error paths     |
| Journey YAML     | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates.yaml`      | Structured journey schema                    |
| Journey Gherkin  | `docs/feature/flagzen-conditions/discuss/journey-condition-predicates.feature`   | Testable acceptance scenarios (25 scenarios) |
| Story Map        | `docs/feature/flagzen-conditions/discuss/story-map.md`                           | Backbone + walking skeleton + release slices |
| Prioritization   | `docs/feature/flagzen-conditions/discuss/prioritization.md`                      | Release priority + backlog suggestions       |
| User Stories     | `docs/feature/flagzen-conditions/discuss/user-stories.md`                        | 8 LeanUX stories with BDD scenarios          |
| Shared Artifacts | `docs/feature/flagzen-conditions/discuss/shared-artifacts-registry.md`           | Integration points + consistency validation  |
| Outcome KPIs     | `docs/feature/flagzen-conditions/discuss/outcome-kpis.md`                        | Measurable outcomes per story                |
| DoR Validation   | `docs/feature/flagzen-conditions/discuss/dor-validation.md`                      | All 8 stories PASSED                         |
| Peer Review      | `docs/feature/flagzen-conditions/discuss/peer-review.md`                         | Approved (iteration 2)                       |

## Key Design Decisions for Solution Architect

### 1. JDK predicate interfaces instead of custom FeaturePredicate

Predicates use standard JDK interfaces (`Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`). Predicates test the flag value, not EvaluationContext. This eliminates a custom type and leverages familiar JDK contracts.

### 2. @Condition is nested inside @Variant

`@Condition` is not a standalone annotation. It lives inside `@Variant(when = @Condition(...))`. This keeps the variant declaration self-contained and avoids the need for cross-annotation matching.

### 3. matches/notMatches instead of on

`@Condition(matches = Enterprise.class)` provides the verb. `notMatches` is the negation option (mutually exclusive with `matches`). The "Is" prefix on predicates is dropped since `matches` already reads as a verb.

### 4. order lives on @Variant, not @Condition

`@Variant(when = @Condition(matches = Enterprise.class), order = 1)` -- the evaluation sequence is a property of the variant, not the condition. `order` is optional when unambiguous.

### 5. Unified ordered dispatch (no mutually exclusive modes)

Exact matches and conditions can coexist on the same @Feature. Exact matches are checked first, then conditions by @Variant order. No need to choose between "value-based" and "condition-based" dispatch modes.

### 6. REQUIRED strategy on conditions demands @DefaultVariant

Since predicate completeness cannot be verified statically (unlike enum coverage), `FallbackStrategy.REQUIRED` on condition-based features requires `@DefaultVariant` at compile time.

### 7. Predicate instantiation: no-arg constructor (Spring DI as extension)

Core module instantiates predicates via no-arg constructor. Spring module (US-CP-08, Could Have) resolves @Component predicates from ApplicationContext. This keeps core reflection-free.

### 8. Predicates evaluated per method call

The generated proxy re-evaluates predicates on each method call, consistent with how value-based dispatch re-queries FlagProvider on each call. Flag value comes from FlagProvider.

### 9. Exception propagation

Predicates are user code. FlagZen does not catch or wrap exceptions from `test()`. They propagate directly to the caller.

## Dependencies

|            Dependency            |   Status    |                       Impact                        |
| -------------------------------- | ----------- | --------------------------------------------------- |
| M0: FallbackStrategy             | DONE        | NONE -- reused unchanged                            |
| M0: Annotation Processor         | DONE        | EXTEND -- add @Condition processing                 |
| M0: ProxyGenerator               | DONE        | EXTEND -- add predicate dispatch path               |
| M4: flagzen-spring               | NOT STARTED | US-CP-08 only -- deferrable                         |

## Risk Assessment

|                          Risk                          | Probability | Impact |                                         Mitigation                                         |
| ------------------------------------------------------ | ----------- | ------ | ------------------------------------------------------------------------------------------ |
| Annotation processor complexity                        | Low         | Medium | Condition processing is additive, not modifying existing validation                        |
| Proxy generation complexity                            | Medium      | Medium | Unified dispatch in one proxy class; dispatch order flag at construction time              |
| Performance of predicate evaluation                    | Low         | Low    | Predicates are user responsibility; document best practice                                 |

## Delivery Recommendation

1. Implement R1 (Walking Skeleton: US-CP-01, 02, 04, 06) first for end-to-end validation
2. R2 (US-CP-03, 05) adds remaining compile-time safety
3. R3 (US-CP-07) handles fallback edge cases
4. R4 (US-CP-08) only after M4 (flagzen-spring) is available
