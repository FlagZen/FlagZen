# Wave Decisions -- flagzen-providers DESIGN

## Decision Summary

### Decisions Shared (Both Adapters)

| # | Decision | Choice | Rationale |
| --- | --- | --- | --- |
| D-S1 | Architectural style | Driven adapter in existing ports-and-adapters | Same as OpenFeature; textbook adapter pattern |
| D-S2 | Context mapper visibility | Package-private | Same as OpenFeature D4 |
| D-S3 | Constructor strategy | Static factory `create(Client/Manager)` only | No no-arg; both SDKs require configuration. No builder; one config dimension. |
| D-S4 | Logging framework | java.util.logging (JUL) | Same as OpenFeature D6; zero added deps |
| D-S5 | ServiceLoader registration | None for either adapter | Both require injected client/manager; no sensible default |

### Decisions -- LaunchDarkly Adapter

| # | Decision | Choice | Rationale |
| --- | --- | --- | --- |
| D-L1 | Absent flag detection | Reason kind-based: ERROR and PREREQUISITE_FAILED = absent, all others = resolved | LaunchDarkly uses `EvaluationReason.Kind` enum, not string reasons. OFF returns a real off-variation value. See ADR-021. |
| D-L2 | Typed method delegation | Override all 10 FlagProvider methods | Native typed calls for string, boolean, int, double. JSON value for long. |
| D-L3 | Long type handling | `jsonValueVariationDetail` + `LDValue.longValue()` | LaunchDarkly has no `longVariationDetail` but JSON values support full long range. See ADR-021. |
| D-L4 | Anonymous context | Shared singleton `LDContext` with `anonymous(true)` | LaunchDarkly requires a context for every evaluation. Singleton avoids allocation per call. |
| D-L5 | `OFF` reason handling | Treat as resolved (return off-variation value) | OFF returns a deliberately configured off-variation, not the client default. Real resolution. |
| D-L6 | SDK version | 7.x (latest stable) | v7 uses `LDContext` (current model). v6 uses deprecated `LDUser`. |

### Decisions -- Togglz Adapter

| # | Decision | Choice | Rationale |
| --- | --- | --- | --- |
| D-T1 | Typed method delegation | Override only `getBoolean` and `getString` (4 of 10 methods) | Togglz has no native numeric types. Default impls parse from getString. |
| D-T2 | EvaluationContext handling | Context parameter ignored; delegate to non-context overloads | Togglz uses thread-local `UserProvider`, incompatible with explicit context passing. See ADR-023. |
| D-T3 | String value source | Activation strategy parameter `"value"`, fallback to enabled-as-string | Togglz has no native string value. Convention-based approach. See ADR-022. |
| D-T4 | Feature key resolution | Case-insensitive match on `Feature.name()`, cached in `ConcurrentHashMap` | Togglz uses enum names (uppercase), FlagZen uses string keys (any case). |
| D-T5 | Absent flag detection | Feature not found OR state is null = absent | Togglz has no evaluation detail/reason concept. Simple existence check. |
| D-T6 | Context warning | One-time INFO log on first context-aware call | Transparent about limitation rather than silently ignoring context. |
| D-T7 | SDK version | 4.x (latest stable) | v4 supports Java 17+ and is the current release. |

## Decisions Carried Forward from OpenFeature

The following OpenFeature design decisions (wave-decisions D1-D8) apply unchanged unless overridden above:

- **D1**: Driven adapter in existing ports-and-adapters -- applies to both (D-S1)
- **D4**: Context mapper is package-private -- applies to both (D-S2)
- **D5**: Static factory constructors, no builder -- applies to both (D-S3, adjusted: no no-arg constructor)
- **D6**: JUL for logging -- applies to both (D-S4)

The following OpenFeature decisions are **overridden** by provider-specific decisions:

- **D2** (reason-based absence via `DEFAULT` string): Overridden by D-L1 (LaunchDarkly uses enum-based reasons) and D-T5 (Togglz uses existence check)
- **D3** (override all 10 methods): Overridden by D-T1 (Togglz overrides only 4)
- **D7** (long via int widening): Overridden by D-L3 (LaunchDarkly uses JSON value for full long range)
- **D8** (`DISABLED` reason handling): Not applicable; LaunchDarkly and Togglz have different reason models

## Open Questions Resolved

### Q: Should `TogglzFlagProvider` override `getInt`/`getLong`/`getDouble` to parse from strategy parameter directly?

**No.** The `FlagProvider` default implementations already parse from `getString`. Since Togglz `getString` reads the `"value"` strategy parameter, the default numeric parsing works correctly. Overriding would duplicate logic without benefit.

### Q: Should the LaunchDarkly adapter support multi-context (`LDContext` with multiple kinds)?

**Not in this release.** FlagZen's `EvaluationContext` has a flat attribute model with a single targeting key. Mapping to multi-context would require extending `EvaluationContext` with a "kind" concept, which is out of scope. Users who need multi-context can construct their own `LDContext` and use the LaunchDarkly SDK directly for those evaluations.

### Q: Should the Togglz adapter handle key format differences (kebab-case vs SCREAMING_SNAKE)?

**No.** FlagZen already has `flagzen-key-mapping` for key format transformations. The adapter resolves keys case-insensitively against `Feature.name()`. Any further key format mapping is the user's responsibility via the key-mapping module. Adding format heuristics to the adapter would create surprising behavior.

### Q: Should the LaunchDarkly adapter catch SDK exceptions (e.g., if `LDClient` is closed)?

**Yes.** The adapter should catch exceptions from `LDClient` method calls and return `empty`, consistent with the principle that adapter errors map to absent flags. The `FallbackStrategy` then handles the absent case. Exceptions should be logged at WARNING level.
