# Story Map: Evaluation Context (flagzen-eval-context)

## User: Kenji Tanaka -- Senior Java developer extending FlagZen with per-user/tenant flag targeting

## Goal: Resolve feature flags based on evaluation context for A/B testing and user segmentation

## Backbone

|                Model Context                 |                       Pass Explicit Context                       |             Block-Scoped Context              |                   ContextAccessor SPI                   |           Configure Defaults            |            Test with Context            |
| -------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------- | ------------------------------------------------------- | --------------------------------------- | --------------------------------------- |
| EvaluationContext model (immutable, builder) | resolve(Class, EvaluationContext) overload on FeatureDispatcher   | FlagContext.run(ctx, Runnable/Supplier)       | ContextAccessor SPI interface + ServiceLoader discovery | Default context via FlagZen.configure() | @PinFlag + TestFlagContext with context |
|                                              | FlagProvider.getString(key, context) overload with default method | ScopedValue (Java 21+) / ThreadLocal fallback | Priority ordering of multiple accessors                 |                                         | Context-aware assertions                |
|                                              | Generated proxy passes context to FlagProvider                    | Nested context scoping                        | ContextAccessor returns Optional.empty() fallthrough    |                                         |                                         |
|                                              |                                                                   | Context cleanup on block exit                 |                                                         |                                         |                                         |

---

### Walking Skeleton

The thinnest end-to-end slice that connects all activities:

1. **Model Context**: `EvaluationContext` with targeting key + attributes, builder, immutable
2. **Pass Explicit Context**: `FeatureDispatcher.resolve(Class, EvaluationContext)` + `FlagProvider.getString(key, context)` default method
3. **Block-Scoped Context**: `FlagContext.run(ctx, Runnable)` with ThreadLocal (simplest path)
4. **ContextAccessor SPI**: `ContextAccessor` interface + ServiceLoader discovery (no implementations yet -- those are M6)
5. **Configure Defaults**: Default context via `FlagZen.configure()`
6. **Test with Context**: Existing `@PinFlag` and `TestFlagContext` work alongside context (no special changes needed for skeleton)

### Release 1: Core Context Support (Walking Skeleton + Completeness)

**Outcome**: Developer can pass evaluation context to flag resolution through all four paths (explicit, scoped, accessor, default) with correct resolution order.

Stories:

- US-EC-01: EvaluationContext model (targeting key, attributes, builder, immutability)
- US-EC-02: Explicit context on FeatureDispatcher.resolve()
- US-EC-03: FlagProvider.getString(key, context) with backward-compatible default method
- US-EC-04: Generated proxy updated to pass context through to FlagProvider
- US-EC-05: FlagContext.run() block-scoped context (ThreadLocal)
- US-EC-06: ContextAccessor SPI interface + ServiceLoader discovery + priority ordering
- US-EC-07: Resolution order: explicit > accessor > scoped > default

### Release 2: Java 21+ Optimization (Enhancement)

**Outcome**: FlagContext.run() uses ScopedValue on Java 21+ for better virtual thread compatibility.

Stories:

- US-EC-08: ScopedValue carrier for FlagContext.run() on Java 21+ with ThreadLocal fallback on 17-20

## Scope Assessment: PASS -- 8 stories, 1 primary bounded context (flagzen-core), estimated 8-10 days

All stories are within flagzen-core (the SPI definitions and internal resolution logic). The ContextAccessor SPI is defined here but implementations (reactor, mutiny) belong to M6. This is right-sized for a single delivery cycle.
