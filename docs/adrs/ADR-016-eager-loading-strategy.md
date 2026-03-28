# ADR-016: Eager Loading Strategy

## Status

Accepted

## Context

`EnvironmentVariableFlagProvider` needs to read environment variables and map them to flag keys. The question is when to read `System.getenv()`: eagerly at construction time or lazily on each `getString()` call.

Quality attribute priorities for this module:
- **Performance**: O(1) flag resolution with zero runtime I/O
- **Testability**: deterministic behavior, easy to verify
- **Reliability**: thread-safe, predictable

The environment variable map does not change during a JVM process's lifetime in normal operation (process-level env vars are set at startup and are immutable from Java's perspective).

## Decision

Eagerly load all environment variables at construction time via a single `System.getenv()` call. Run the parse/format pipeline immediately. Store results in an immutable `Map<String, String>`. `getString(key)` becomes a pure `Map.get()` call with zero runtime I/O.

Testability is achieved by accepting a `Supplier<Map<String, String>>` on the builder (defaulting to `System::getenv`), allowing tests to inject controlled environment maps.

## Alternatives Considered

### Alternative 1: Lazy loading (System.getenv(key) per call)

Call `System.getenv(reverseMap.get(key))` on each `getString()` invocation.

- **Pro**: No upfront cost. Only reads env vars that are actually queried.
- **Pro**: Reflects any hypothetical runtime env var changes.
- **Con**: Requires reverse-mapping from flag key back to env var name(s) -- complex with multiple parsers/formatters.
- **Con**: `System.getenv()` on every call adds latency and unpredictability.
- **Con**: Thread safety requires careful synchronization of the reverse map or per-call computation.
- **Con**: Harder to test -- must mock `System.getenv()` with PowerMock/Mockito static mocking.
- **Con**: "Reflects runtime changes" is not a real benefit -- Java env vars are process-level immutable in practice.
- **Rejected because**: performance penalty on every read, complexity of reverse mapping, testability cost.

### Alternative 2: Lazy loading with cache (read once per key, cache result)

First `getString()` call for a key triggers `System.getenv()`, caches the result.

- **Pro**: Amortized O(1) after first access per key.
- **Pro**: Lower upfront cost than eager loading.
- **Con**: First call per key has unpredictable latency (cold cache).
- **Con**: Still needs reverse mapping logic.
- **Con**: Cache invalidation is undefined (env vars don't change, so cache never invalidates -- making the lazy approach equivalent to eager with extra complexity).
- **Con**: Thread-safe cache requires `ConcurrentHashMap.computeIfAbsent()` or similar.
- **Rejected because**: equivalent to eager loading but with more moving parts and unpredictable first-call latency.

### Alternative 3: Eager loading with periodic refresh

Load eagerly, then refresh on a timer (e.g., every 60 seconds).

- **Pro**: Would pick up env var changes if they occurred.
- **Pro**: Mostly immutable between refreshes.
- **Con**: Java env vars do not change at runtime. Periodic refresh reads the same values repeatedly.
- **Con**: Adds thread scheduling complexity and a mutable state window during refresh.
- **Con**: Violates the "simple solution first" principle for zero benefit.
- **Rejected because**: solving a problem that does not exist (env var mutation in running JVM).

## Consequences

### Positive

- `getString()` is O(1) with zero allocation and zero I/O -- ideal for high-throughput services
- Immutable map is inherently thread-safe -- no synchronization needed
- Deterministic: flag map is fully determined at construction time
- Testable: `Supplier<Map<String, String>>` injection enables clean unit tests without mocking frameworks
- Simple: one `System.getenv()` call, one pipeline pass, one `Map.copyOf()`

### Negative

- Env var changes after construction are not reflected (not a real concern -- Java process env vars are immutable in practice)
- All matching env vars processed at construction even if most flags are never queried (negligible cost for typical env var counts)
- Constructor does non-trivial work (pipeline execution) -- violates "lightweight constructor" heuristic, but justified by the immutability guarantee
