# ADR-012: FlagContext Carrier Strategy (ThreadLocal vs ScopedValue)

## Status

Accepted

## Context

`FlagContext.run()` needs a thread-local storage mechanism to carry `EvaluationContext` for the duration of a code block. Two mechanisms exist in the JDK:

- **ThreadLocal** (Java 1.2+): Universally available, works with platform and virtual threads. Virtual threads inherit ThreadLocal values from parent. However, ThreadLocal has performance overhead with virtual threads due to thread-local storage copying.
- **ScopedValue** (Java 21 preview, Java 25 final): Designed for structured concurrency and virtual threads. Better performance, automatic scoping, no cleanup needed. Not available on Java 17-20.

FlagZen targets Java 17+ as its minimum version.

## Decision

**Release 1**: Use ThreadLocal exclusively. Correct on all Java 17+ runtimes.

**Release 2**: Use ScopedValue on Java 21+ with ThreadLocal fallback on Java 17-20. Detection at class-loading time via reflective check for `java.lang.ScopedValue` class availability. The `FlagContext` public API is identical regardless of carrier.

### Detection Strategy

At class-loading time (static initializer or lazy singleton), `FlagContext` checks:

```
Class.forName("java.lang.ScopedValue") succeeds? --> ScopedValue carrier
Otherwise --> ThreadLocal carrier
```

This check happens once. The carrier is selected for the lifetime of the JVM.

### Internal Carrier Abstraction

An internal `ContextCarrier` interface (in `com.flagzen.internal`) with two implementations:
- `ThreadLocalContextCarrier` -- always available
- `ScopedValueContextCarrier` -- loaded only on Java 21+

This uses the strategy pattern to avoid conditional logic in `FlagContext.run()`.

## Alternatives Considered

### Alt 1: ThreadLocal only (no ScopedValue ever)

Sufficient for correctness. Rejected for Release 2 because:
- ScopedValue has better performance with virtual threads (no storage copying)
- ScopedValue has built-in scoping semantics (no manual cleanup)
- FlagZen targets technical excellence; supporting modern JDK features is aligned with that goal
- The carrier abstraction is minimal cost

### Alt 2: Multi-release JAR (MRJAR)

Use `META-INF/versions/21/` to provide a Java 21-specific `FlagContext` class. Rejected because:
- MRJAR replaces entire classes, not methods -- duplicates most of `FlagContext`
- Build complexity (separate source sets, testing on multiple JDKs)
- Runtime detection via reflection is simpler and achieves the same result
- MRJAR is better suited for large class differences, not a single carrier swap

### Alt 3: Require Java 21+ for ScopedValue, no fallback

Make ScopedValue an opt-in module (e.g., `flagzen-scoped-value`). Rejected because:
- Adds a module for a single internal implementation detail
- Users should not need to know about carrier strategy
- The detection-based approach is transparent

## Consequences

### Positive

- R1 is simple and correct on all Java 17+ runtimes
- R2 leverages ScopedValue performance benefits transparently
- Public API is identical across all Java versions
- Carrier detection happens once (no per-call overhead)

### Negative

- R2 uses one reflective class lookup at initialization time (acceptable: it is a class existence check, not runtime reflection for dispatch)
- ScopedValue API was preview until Java 25 -- users on Java 21-24 may need `--enable-preview` or may not have it. The crafter should verify ScopedValue availability status per JDK version and adjust the detection accordingly.
- Two code paths to test (ThreadLocal and ScopedValue) in R2
