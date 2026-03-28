# ADR-017: Conflict Strategy Design

## Status

Accepted

## Context

When multiple parsers and/or multiple formatters are configured, different environment variables can map to the same flag key. For example:

- `FLAGZEN_CHECKOUT_FLOW=PREMIUM` (screamingSnakeCase parser) -> `checkout-flow`
- `myAppCheckoutFlow=BASIC` (camelCase parser) -> `checkout-flow`

Both map to flag key `checkout-flow` but with different values. The system must handle this conflict explicitly.

Additionally, the conflict risk varies by configuration cardinality:

| Parsers | Formatters |                                                      Conflict Risk                                                      |
| ------- | ---------- | ----------------------------------------------------------------------------------------------------------------------- |
| 1       | 1          | Low (only from env var collisions after prefix stripping)                                                               |
| N       | 1          | Medium (different parsers may match overlapping env vars)                                                               |
| 1       | N          | Medium (multiple formatters produce multiple keys, reducing collision chance, but different env vars can still collide) |
| N       | N          | High (cartesian product of parsers and formatters maximizes collision surface)                                          |

## Decision

Introduce a `ConflictStrategy` enum with two values:

- **WARN**: Log a warning at construction time (with both env var names and the conflicted flag key), keep the last mapping, record the flag key as conflicted. On first `getString()` access of a conflicted key, log another warning. Do not repeat on subsequent accesses.
- **ERROR**: Throw `IllegalStateException` at construction time with both env var names and the conflicted flag key. Provider is not created.

Default strategy is computed from parser/formatter cardinality:

| Parsers | Formatters | Default |
| ------- | ---------- | ------- |
| 1       | 1          | WARN    |
| N       | 1          | WARN    |
| 1       | N          | WARN    |
| N       | N          | ERROR   |

The default can be overridden via `.onConflict(ConflictStrategy)` on the builder.

## Alternatives Considered

### Alternative 1: No conflict handling (last-write-wins silently)

Simply overwrite the previous mapping when a conflict occurs. No warning, no error.

- **Pro**: Simplest implementation. No enum, no tracking, no warnings.
- **Pro**: Predictable behavior (last parser/formatter pair wins).
- **Con**: Silent data loss. Developer sets both `FLAGZEN_CHECKOUT_FLOW=PREMIUM` and `myAppCheckoutFlow=BASIC` and gets `BASIC` without knowing `PREMIUM` was overwritten.
- **Con**: Debugging production issues becomes nearly impossible when flag values silently change due to env var collisions.
- **Rejected because**: silent data loss violates the principle of least surprise. A library that silently discards configuration is hostile to its users.

### Alternative 2: Always ERROR on conflict

Any conflict throws `IllegalStateException`. No WARN mode.

- **Pro**: Strictest safety. Impossible to have silent conflicts.
- **Pro**: Simpler design (no warning tracking, no first-access logic).
- **Con**: Too strict for common cases. A single-parser, single-formatter config where two env vars accidentally collide (rare) would crash the app at startup.
- **Con**: Migration scenarios (transitioning from `FLAGZEN_` to `FF_` prefix) intentionally have overlapping env vars during the transition. ERROR would block this.
- **Con**: No graceful degradation.
- **Rejected because**: overly strict for the common case; blocks legitimate migration scenarios.

### Alternative 3: Priority-based resolution (first-wins or explicit priority)

Assign priority to parsers/formatters. Higher-priority mapping wins on conflict.

- **Pro**: Deterministic conflict resolution with explicit control.
- **Pro**: Useful for migration (old convention = low priority, new convention = high priority).
- **Con**: Adds complexity to the builder API (`.parser(parser, priority)` or ordering semantics).
- **Con**: Priority-based resolution still silently discards one value -- just in a more controlled way.
- **Con**: Can be achieved with WARN strategy + intentional parser ordering (first or last wins). Dedicated priority system is overengineering for the current use case.
- **Rejected because**: premature complexity. The WARN strategy with last-wins ordering achieves the same result. Priority can be added later if demand emerges.

## Consequences

### Positive

- Conflicts are never silent. WARN logs at construction and first access; ERROR fails fast.
- Cardinality-based defaults match risk level: low-risk configs warn, high-risk configs fail fast.
- Override via `.onConflict()` gives full control for advanced use cases.
- First-access warning provides conflict visibility at the point of use (not just buried in startup logs).
- ConflictStrategy lives in `flagzen-key-mapping` -- reusable by future providers.

### Negative

- First-access warning requires mutable state (a concurrent set tracking "already warned" keys) in an otherwise immutable provider. This is the only mutable state after construction.
- Two-phase warning (construction + first access) is more complex than single-phase.
- WARN + last-wins is non-deterministic if env var iteration order is not guaranteed (it is on most JVMs, but not contractually). Crafter should document this behavior.
