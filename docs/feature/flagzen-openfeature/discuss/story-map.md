# Story Map: flagzen-openfeature

## User: Ricardo Alves (Java developer using OpenFeature + Flagd)

## Goal: Resolve FlagZen feature flags through the OpenFeature SDK

## Backbone

| Add Dependency |      Configure Provider      |              Resolve String Flags              |               Resolve Typed Flags                |                      Resolve with Context                      |
| -------------- | ---------------------------- | ---------------------------------------------- | ------------------------------------------------ | -------------------------------------------------------------- |
| Add Gradle dep | ServiceLoader auto-discovery | getString delegates to client.getStringDetails | getBoolean delegates to client.getBooleanDetails | Map FlagZen EvaluationContext to OpenFeature EvaluationContext |
|                | Explicit Client constructor  | Handle DEFAULT/ERROR reason -> empty           | getInt delegates to client.getIntegerDetails     | Targeting key 1:1 mapping                                      |
|                | Spring Bean registration     |                                                | getDouble delegates to client.getDoubleDetails   | Attribute type conversion (String, Boolean, Number, List, Map) |
|                |                              |                                                | getLong via getIntegerDetails (widening)         | Unsupported attribute types logged and skipped                 |

---

### Walking Skeleton

The thinnest end-to-end slice that proves the adapter works:

1. **Add Dependency** -- `flagzen-openfeature` module exists with `build.gradle.kts`
2. **Configure Provider** -- No-arg constructor uses global OpenFeature client; ServiceLoader registration
3. **Resolve String Flags** -- `getString(key)` delegates to `client.getStringDetails(key, "")`, returns `Optional.empty()` on DEFAULT/ERROR reason
4. **Resolve Typed Flags** -- (skipped in skeleton -- FlagProvider defaults parse from getString)
5. **Resolve with Context** -- (skipped in skeleton -- context-free overload sufficient)

### Release 1: Walking Skeleton + Typed Methods

**Outcome**: Ricardo can resolve string and typed flags through OpenFeature without context.

- US-OF-01: String flag resolution through OpenFeature (walking skeleton)
- US-OF-02: Typed flag resolution (boolean, int, long, double) via native OpenFeature methods

### Release 2: Context-Aware Resolution

**Outcome**: Ricardo can pass targeting context for per-user flag resolution.

- US-OF-03: EvaluationContext mapping and context-aware resolution

## Scope Assessment: PASS -- 3 stories, 1 bounded context (flagzen-openfeature), estimated 3-4 days
