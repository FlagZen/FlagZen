# ADR-011: Context Resolution Order

## Status

Accepted

## Context

FlagZen M1 introduces evaluation context for targeted flag resolution. Multiple context sources can be active simultaneously:

1. **Explicit parameter** -- passed directly to `resolve(Class, EvaluationContext)`
2. **ContextAccessor SPI** -- framework adapters (Reactor, Mutiny, Servlet) providing context
3. **Block-scoped context** -- `FlagContext.run(ctx, ...)` setting context for a code block
4. **Default context** -- configured via `FlagZen.configure(config -> config.defaultContext(...))`

When multiple sources are active, the system must deterministically select one. The order must be intuitive, documented, and non-configurable (simplicity over flexibility, per DISCUSS wave decision D7).

## Decision

Fixed resolution order: **explicit > accessor > scoped > default**. When none provides a context, fall back to `FlagProvider.getString(key)` without context (M0 behavior).

The order is hardcoded and cannot be reconfigured by the user.

## Alternatives Considered

### Alt 1: Configurable resolution order

Allow users to reorder the chain (e.g., scoped before accessor). Rejected because:
- Adds configuration complexity with minimal practical benefit
- The fixed order covers all known use cases
- Configurable order makes behavior harder to reason about in debugging
- No concrete use case was identified where the fixed order is wrong

### Alt 2: Merge contexts from multiple sources

Instead of "first wins," merge attributes from all sources (explicit overrides accessor overrides scoped, etc.). Rejected because:
- Merging semantics are ambiguous (deep merge? shallow? what about conflicting targeting keys?)
- Increases cognitive load -- developers cannot predict the final context without understanding all active sources
- No known feature flag SDK uses merge semantics for context resolution
- Violates simplicity principle

### Alt 3: Scoped before accessor

Place `FlagContext.run()` scoped context before ContextAccessor in the chain. Rejected because:
- Framework-provided context (via accessor) is typically more specific than thread-scoped context
- Accessor context comes from the request/reactive pipeline and carries per-request identity
- Scoped context is a convenience for grouping resolve calls, not a precision targeting mechanism
- OpenFeature's evaluation context precedence model places API-level context (analogous to accessor) before transaction-level context (analogous to scoped)

## Consequences

### Positive

- Deterministic: same inputs always produce the same context selection
- Intuitive: follows the specificity principle (most specific wins)
- Simple: no configuration, no merging, no ambiguity
- Documented: Javadoc on FeatureDispatcher specifies the exact order

### Negative

- Inflexible: if a use case requires scoped-before-accessor, the user must use explicit context instead
- Fixed at compile time: cannot be changed without a library release
