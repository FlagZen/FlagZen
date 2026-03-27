# Walking Skeleton: Evaluation Context (flagzen-eval-context)

## Overview

Three walking skeletons trace thin vertical slices through the evaluation context feature, each delivering observable developer value end-to-end.

## Skeleton 1: Explicit Context Resolution

**Stories**: US-EC-01, US-EC-02, US-EC-03

**User Goal**: Developer builds an evaluation context and passes it to flag resolution. The flag provider receives the context and resolves the flag differently per user.

**Slice**: EvaluationContext.builder() -> FeatureDispatcher.resolve(Class, EvaluationContext) -> FlagProvider.getString(key, context) -> proxy dispatches to context-determined variant

**Demo-able outcome**: "Given a VIP user context, the checkout resolves to PREMIUM instead of CLASSIC."

**Litmus test**:

- Title describes user goal: YES -- "Developer resolves a feature with per-user evaluation context"
- Given/When describe user actions: YES -- building context, resolving feature
- Then describes observable outcome: YES -- proxy dispatches to correct variant, provider received context
- Stakeholder confirmation: YES -- a developer can confirm this is the core value proposition

## Skeleton 2: Block-Scoped Context

**Stories**: US-EC-05, US-EC-02

**User Goal**: Developer scopes context to a block so multiple resolve calls share the same user context without threading parameters through the call stack.

**Slice**: EvaluationContext.builder() -> FlagContext.run(ctx, block) -> multiple FeatureDispatcher.resolve(Class) calls -> all use same context

**Demo-able outcome**: "Both CheckoutFlow and PaymentMethod resolve using Maria's context within a single code block."

**Litmus test**:

- Title describes user goal: YES -- "Developer scopes evaluation context to a block of code"
- Given/When describe user actions: YES -- wrapping resolve calls in scoped block
- Then describes observable outcome: YES -- both features resolved with same targeting key
- Stakeholder confirmation: YES -- eliminates parameter drilling, a clear developer productivity win

## Skeleton 3: Resolution Order Precedence

**Stories**: US-EC-07, US-EC-06

**User Goal**: When multiple context sources are active, explicit context always wins. The developer can predict which context will be used.

**Slice**: ContextAccessor (registered) + FlagContext.run() (active) + explicit parameter -> FeatureDispatcher.resolve(Class, EvaluationContext) -> explicit wins

**Demo-able outcome**: "Despite an accessor and scoped context being active, the explicit context with targeting key 'explicit-user' is used."

**Litmus test**:

- Title describes user goal: YES -- "Explicit context takes precedence over all other context sources"
- Given/When describe user actions: YES -- multiple sources configured, explicit passed
- Then describes observable outcome: YES -- provider received explicit targeting key, accessor not consulted
- Stakeholder confirmation: YES -- deterministic resolution is critical for debugging

## Implementation Sequence

Enable and implement in this order:

1. **Skeleton 1** first -- establishes the core context flow (build, pass, receive)
2. **Skeleton 2** second -- adds block-scoping atop the core flow
3. **Skeleton 3** third -- adds resolution order atop scoping and accessors

Each skeleton builds on the infrastructure established by the previous one.
