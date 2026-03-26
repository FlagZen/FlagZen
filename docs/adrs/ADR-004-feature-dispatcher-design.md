# ADR-004: FeatureDispatcher Design

## Status

Accepted

## Context

The `FeatureDispatcher` is the runtime entry point for resolving `@Feature` interfaces to their generated proxies. Developers call `dispatcher.resolve(CheckoutFlow.class)` to obtain the proxy. The dispatcher must:

1. Map `Class<T>` to the generated proxy instance
2. Return the same proxy instance per feature (singleton)
3. Be thread-safe
4. Be testable (mockable/replaceable in tests)
5. Work with DI frameworks (injectable as a bean)

### Design Decision (Pre-confirmed)

FeatureDispatcher is an interface with a default factory method. Concrete implementation is internal/package-private.

## Decision

`FeatureDispatcher` is a public interface in `com.flagzen`:

```
public interface FeatureDispatcher {
    <T> T resolve(Class<T> featureType);
}
```

The concrete implementation (`DefaultFeatureDispatcher`) is package-private in `com.flagzen.internal`. It is created via the `FlagZen` factory:

```
FeatureDispatcher dispatcher = FlagZen.dispatcher();
```

Or via configuration:

```
FlagZen.configure(config -> config.provider(myProvider));
FeatureDispatcher dispatcher = FlagZen.dispatcher();
```

Proxy instances are singletons per feature per dispatcher instance (stored in `ConcurrentHashMap<Class<?>, Object>`).

## Alternatives Considered

### Alternative 1: Final Class with Static Methods

```
public final class FeatureDispatcher {
    public static <T> T resolve(Class<T> featureType) { ... }
}
```

- **Pro**: Simplest API -- no instantiation needed
- **Pro**: One less concept for users
- **Con**: Static methods are untestable without mocking frameworks (PowerMock, Mockito static)
- **Con**: Cannot have multiple dispatchers with different providers (useful in testing, multi-tenant)
- **Con**: Global mutable state for configuration -- thread safety issues in tests
- **Con**: Cannot be injected by DI frameworks (Spring @Autowired requires instance)

**Rejected**: Violates testability (primary quality attribute). Static methods create global state and prevent DI injection.

### Alternative 2: Abstract Class with Factory Method

```
public abstract class FeatureDispatcher {
    public static FeatureDispatcher create(FlagProvider provider) { ... }
    public abstract <T> T resolve(Class<T> featureType);
}
```

- **Pro**: Hides concrete implementation
- **Pro**: Can add convenience methods without breaking interface contract
- **Con**: Abstract classes prevent consumers from creating test doubles via interface
- **Con**: Less idiomatic Java -- interfaces are preferred for SPI-style contracts
- **Con**: Cannot implement multiple "interfaces" if future needs arise

**Rejected**: Interface is more idiomatic, more testable (easy to mock/stub), and more flexible for DI frameworks.

### Alternative 3: Interface with Multiple Implementations Exposed

Make `DefaultFeatureDispatcher` public so users can instantiate directly.

- **Pro**: Transparent -- users see the concrete class
- **Con**: Couples users to implementation -- cannot change internal structure without breaking API
- **Con**: Users bypass the factory and may misconfigure
- **Con**: Public API surface increases unnecessarily

**Rejected**: Exposing the implementation couples consumers to it. The factory pattern (`FlagZen.dispatcher()`) allows internal evolution without API breaks.

## Consequences

### Positive

- Interface is testable -- consumers can create test doubles without mocking frameworks
- DI frameworks can inject `FeatureDispatcher` as a bean (Spring, CDI)
- Factory method (`FlagZen.dispatcher()`) controls construction and configuration
- Internal implementation can evolve without breaking public API
- Multiple dispatcher instances possible (different providers per dispatcher)

### Negative

- Two concepts to understand: `FeatureDispatcher` (interface) and `FlagZen` (factory)
- Factory pattern slightly more complex than a simple constructor
- Package-private implementation means consumers cannot extend `DefaultFeatureDispatcher` (intentional)
