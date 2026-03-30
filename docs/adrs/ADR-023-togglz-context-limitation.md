# ADR-023: Togglz EvaluationContext Incompatibility

## Status

Proposed

## Context

`FlagProvider` defines context-aware overloads for all methods (e.g., `getString(String key, EvaluationContext context)`). These allow FlagZen to pass targeting information (user ID, attributes) to the flag provider for context-sensitive evaluation.

Togglz handles user context through a `UserProvider` interface, which returns the current `FeatureUser` via a thread-local mechanism. The application sets the current user (typically in a servlet filter or request interceptor), and Togglz reads it during feature evaluation. This is fundamentally different from FlagZen's explicit parameter-passing approach.

### The Impedance Mismatch

| Aspect | FlagZen | Togglz |
| --- | --- | --- |
| Context passing | Explicit parameter on each call | Thread-local via `UserProvider` |
| Context lifecycle | Per-evaluation (caller controls) | Per-request (framework controls) |
| Context scope | Method-scoped | Thread-scoped |
| Who sets context | Caller of `FlagProvider` | Application filter/interceptor |

### Quality Attributes at Stake

- **Correctness**: Setting a thread-local inside the adapter for the duration of a call would be a side effect visible to other Togglz code on the same thread. Concurrent calls on the same thread could interfere.
- **Transparency**: Users should understand what the adapter can and cannot do. Silent context ignoring is worse than explicit documentation.
- **Simplicity**: The adapter should not introduce complex thread-local management to bridge an architectural mismatch.

## Decision

The context-aware overloads (`getString(key, ctx)`, `getBoolean(key, ctx)`) **delegate to their non-context counterparts**, ignoring the `EvaluationContext` parameter.

A one-time INFO-level log is emitted on the first context-aware call:

> TogglzFlagProvider does not support explicit EvaluationContext. Configure a Togglz UserProvider for user targeting.

This is implemented with an `AtomicBoolean` flag (initialized to `false`, set to `true` after first log). Thread-safe, zero overhead after first call.

## Alternatives Considered

### Alternative A: Set Thread-Local `UserProvider` per Call

Wrap each context-aware evaluation in a try-finally block that sets a temporary `ThreadLocalUserProvider` with the mapped `EvaluationContext`, calls Togglz, then restores the previous `UserProvider`.

**Evaluation:**

- (+) Context-aware evaluation would work through Togglz's native mechanism
- (-) **Side effects**: Setting a thread-local is visible to all Togglz code on that thread. If the caller has nested Togglz evaluations (e.g., a Togglz activation strategy that checks another feature), they would see the FlagZen-set context.
- (-) **Concurrency risk**: Virtual threads (Java 21+) and coroutine-like patterns can share thread-locals unpredictably. Setting a thread-local inside a library method is dangerous.
- (-) **Requires `FeatureManager` reconfiguration**: Togglz's `FeatureManager` must be configured with the `UserProvider`. Swapping it at runtime is not part of the public API.
- (-) Violates the "no side effects" principle for adapter methods

**Rejected because**: thread-local manipulation inside a library method is a correctness hazard and violates the adapter's contract of being a pure delegation.

### Alternative B: Accept `FeatureUser` Instead of `EvaluationContext`

Add a separate method `getBoolean(String key, FeatureUser user)` that uses Togglz-specific context.

**Evaluation:**

- (-) `FlagProvider` SPI does not have this method signature
- (-) Would require extending `FlagProvider` with a Togglz-specific method, coupling the SPI to a specific provider
- (-) Breaks the adapter pattern (the whole point is to abstract away provider specifics)

**Rejected because**: violates the adapter pattern and couples the SPI to Togglz.

### Alternative C: Silently Ignore Context Without Logging

Just delegate to non-context methods. No log, no documentation.

**Evaluation:**

- (+) Zero overhead, simplest implementation
- (-) **Invisible limitation**: Users pass `EvaluationContext` expecting it to affect evaluation. Silent ignoring means context-sensitive tests pass when they should fail. This is a correctness bug hiding behind silence.

**Rejected because**: transparency matters. Users deserve to know their context is being ignored.

## Consequences

### Positive

- **No side effects**: The adapter never manipulates thread-locals or global state.
- **Transparent limitation**: The INFO log alerts users on first occurrence. Users can configure Togglz's `UserProvider` for targeting.
- **Simple implementation**: Four override methods that delegate to their non-context counterparts.

### Negative

- **EvaluationContext is ignored**: Users who rely on FlagZen's explicit context passing for targeted evaluation cannot use it with Togglz through this adapter. They must use Togglz's native `UserProvider` mechanism instead.
- **Feature parity gap**: The LaunchDarkly and OpenFeature adapters support `EvaluationContext`; Togglz does not. This is an inherent limitation of Togglz's architecture, not a design choice the adapter can fix.
- **One-time log may be missed**: If the log happens during startup noise, users might not notice. This is mitigated by documenting the limitation in Javadoc and on the docs site.
