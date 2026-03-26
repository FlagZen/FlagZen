# ADR-003: FlagProvider SPI Contract

## Status

Accepted

## Context

FlagZen needs a pluggable interface for reading flag values from external sources (environment variables, LaunchDarkly, Togglz, OpenFeature, in-memory). This interface is the primary SPI -- it must be simple enough that implementing a new provider is trivial, yet expressive enough for real flag resolution.

The project uses two distinct flag consumption paths:

1. **Polymorphic dispatch**: `@Feature`/`@Variant` proxies need the flag value as a string to look up the matching variant
2. **Conditional API** (future): `flags.getBoolean("FEATURE_X_ENABLED")` needs typed values

### Design Decision (Pre-confirmed)

FlagProvider contract is `Optional<String> getString(String key)` only for Release 1. Typed accessors deferred.

## Decision

```
public interface FlagProvider {
    Optional<String> getString(String key);
}
```

Single method. String-only. The string value is matched against `@Variant` values for polymorphic dispatch. `Optional.empty()` means the flag is not configured (triggers fallback strategy).

Implementations must be thread-safe.

Discovery: `java.util.ServiceLoader` via `META-INF/services/com.flagzen.spi.FlagProvider` + programmatic registration via `FlagZen.configure()`.

## Alternatives Considered

### Alternative 1: Typed Multi-Method Interface

```
public interface FlagProvider {
    Optional<String> getString(String key);
    Optional<Boolean> getBoolean(String key);
    Optional<Integer> getInteger(String key);
    Optional<Object> getObject(String key);
}
```

- **Pro**: Type-safe accessors for the conditional API path
- **Pro**: Avoids string parsing at call site
- **Con**: Every provider implementation must implement 4+ methods
- **Con**: Polymorphic dispatch only needs strings (variant values are always strings)
- **Con**: Higher implementation burden reduces adoption of custom providers
- **Con**: Premature -- the conditional API path is not in Release 1

**Rejected for Release 1**: Only polymorphic dispatch needs flag values in R1, and it uses strings. Typed accessors add implementation burden without value until the conditional API path is built. Can be added as default methods in a future release without breaking existing providers.

### Alternative 2: Generic getValue with Type Parameter

```
public interface FlagProvider {
    <T> Optional<T> getValue(String key, Class<T> type);
}
```

- **Pro**: Single method handles all types
- **Pro**: Extensible to custom types
- **Con**: Implementations must handle type conversion generically -- complex
- **Con**: Type erasure complications at runtime
- **Con**: Forces every provider to implement type coercion logic
- **Con**: Overengineered for the current need (string matching for dispatch)

**Rejected**: Overengineered. Pushes type conversion complexity into every provider implementation. The simple `getString` covers 100% of Release 1 needs.

### Alternative 3: Evaluation-Context-Aware Contract

```
public interface FlagProvider {
    Optional<String> getString(String key, EvaluationContext context);
}
```

- **Pro**: Context-aware flag resolution (user targeting, A/B testing)
- **Con**: Forces every provider to accept a context parameter even if they ignore it
- **Con**: EvaluationContext design is not finalized for Release 1
- **Con**: Simple providers (env vars, in-memory) have no use for context

**Rejected for Release 1**: EvaluationContext is deferred. When added, it can be supported via an extended interface (`ContextAwareFlagProvider extends FlagProvider`) or by the FeatureDispatcher passing context through a separate mechanism.

## Consequences

### Positive

- Minimal interface -- implementing a new provider is a single method
- `Optional<String>` clearly signals "flag not found" vs. "flag has value"
- String values are universal -- every flag system can produce strings
- Easy to test: `InMemoryFlagProvider` is trivial to implement
- Non-breaking evolution path: add typed methods as `default` in future releases

### Negative

- Conditional API path (`if (flags.getBoolean(...))`) requires string parsing or a separate interface (deferred to future release)
- No built-in type coercion -- consumers of the conditional API must parse strings (acceptable trade-off for Release 1)
- Context-aware resolution requires architectural extension in a future release
