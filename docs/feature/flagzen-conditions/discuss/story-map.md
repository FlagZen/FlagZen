# Story Map: Condition Predicates (flagzen-conditions)

## User: Kenji Tanaka -- Senior Java developer, SaaS company

## Goal: Select variants based on flag value predicates without external flag provider

## Backbone

|       Define Predicate API        |       Declare Conditions        |           Compile-Time Validation           |           Runtime Dispatch            |           DI Integration           |
| --------------------------------- | ------------------------------- | ------------------------------------------- | ------------------------------------- | ---------------------------------- |
| US-CP-01: JDK predicate contract | US-CP-03: @Condition annotation | US-CP-04: Predicate type validation         | US-CP-06: Predicate dispatch in proxy | US-CP-08: Spring DI for predicates |
| US-CP-02: @Condition annotation  |                                 | US-CP-05: Order uniqueness validation       | US-CP-07: Fallback behavior           |                                    |

---

### Walking Skeleton

The thinnest end-to-end slice connecting all core activities:

- **US-CP-01**: JDK predicate interface contract (`Predicate<String>`, IntPredicate, etc.)
- **US-CP-02**: @Condition annotation definition with `matches`/`notMatches`
- **US-CP-04**: Compile-time validation (predicate implements JDK predicate interface)
- **US-CP-06**: Proxy generation and runtime predicate dispatch

This delivers: define a predicate, annotate a variant, compile, and dispatch -- the complete happy path.

### Release 1: Core Condition Dispatch (Walking Skeleton)

Stories: US-CP-01, US-CP-02, US-CP-04, US-CP-06

Outcome: Developer can define predicates, annotate variants with @Condition, and get predicate-based dispatch at runtime. Exact matches and conditions can coexist on the same @Feature via unified ordered dispatch.

### Release 2: Compile-Time Safety

Stories: US-CP-03, US-CP-05

Outcome: All invalid configurations caught at compile time -- no surprises at runtime.

### Release 3: Fallback and Edge Cases

Stories: US-CP-07

Outcome: Graceful handling when no predicate matches. @DefaultVariant and FallbackStrategy work consistently with condition-based dispatch.

### Release 4: Spring DI Integration

Stories: US-CP-08

Outcome: Predicates instantiated via Spring DI (constructor injection, @Component) instead of no-arg constructor.

### Design Notes

- **Unified dispatch**: Exact matches and conditions can coexist on the same @Feature. `order` on @Variant controls evaluation sequence. Exact matches are checked first, then conditions by order.
- **order on @Variant**: The `order` attribute lives on `@Variant`, not on `@Condition`. It is optional when unambiguous.
- **Predicates test flag values**: Predicates receive the flag value (String, int, long, double), not EvaluationContext.

## Scope Assessment: PASS -- 8 stories, 2 contexts (flagzen-core annotations/processor, flagzen-core runtime), estimated 8-10 days
