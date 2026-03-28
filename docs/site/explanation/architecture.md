# Architecture Explanation

Understanding FlagZen's design, from compile-time code generation to runtime dispatch.

## The Problem FlagZen Solves

Feature flags are widely used to toggle code paths at runtime without redeployment. Traditional implementations scatter conditional logic throughout code:

```java
// Without FlagZen: scattered if/else chains
if (flagProvider.getString("checkout-flow").orElse("").equals("PREMIUM")) {
    return new PremiumCheckout();
} else if (flagProvider.getString("checkout-flow").orElse("").equals("CLASSIC")) {
    return new ClassicCheckout();
} else {
    throw new UnknownVariantException(...);
}
```

This approach has drawbacks:

- **No type safety**: Flag keys and variant values are strings, prone to typos
- **No compile-time validation**: Missing variants discovered only at runtime
- **Boilerplate-heavy**: Each feature requires manual dispatch logic
- **Hard to test**: Setting up flags requires mocking or environment manipulation
- **Reflection at runtime**: Some frameworks use reflection for variant discovery, adding complexity and GraalVM incompatibility

FlagZen replaces this with **compile-time code generation**, giving you a clean polymorphic API:

```java
// With FlagZen: type-safe, generated dispatch
@Feature("checkout-flow")
public interface CheckoutFlow { String execute(); }

@Variant(value = "PREMIUM", of = CheckoutFlow.class)
public class PremiumCheckout implements CheckoutFlow { ... }

CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class); // Type-safe resolution
flow.execute(); // Dispatches to active variant
```

## Core Design Philosophy

FlagZen's architecture rests on three principles:

1. **Compile-time safety** — the annotation processor validates everything upfront
2. **Zero runtime reflection** — all dispatch is generated code and method calls
3. **Pluggable providers** — different flag sources (env vars, LaunchDarkly, etc.) via SPI

## How It Works: The Full Pipeline

### Phase 1: Compile Time (Annotation Processing)

You write:

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "classic"; }
}

@Variant(value = "PREMIUM", of = CheckoutFlow.class)
public class PremiumCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "premium"; }
}
```

The **annotation processor** (discovered via `META-INF/services/javax.annotation.processing.Processor`) runs during compilation and:

1. **Discovers** all `@Feature` interfaces and `@Variant` implementations
2. **Validates**:
   - Each `@Variant` implements its feature interface
   - No duplicate variant values
   - All values match the feature's declared type (STRING, INT, LONG, BOOLEAN, DOUBLE)
   - REQUIRED fallback strategy requires all possible values to be covered
3. **Generates** a proxy class per feature using JavaPoet:

```java
// Generated: CheckoutFlow_FlagZenProxy.java
public class CheckoutFlow_FlagZenProxy implements CheckoutFlow {
    private final FlagProvider provider;
    private final Map<String, Supplier<CheckoutFlow>> variants;

    // Package-private constructor; accessed via FeatureMetadata factory
    CheckoutFlow_FlagZenProxy(
            FlagProvider provider,
            Map<String, Supplier<CheckoutFlow>> variants) {
        this.provider = provider;
        this.variants = variants;
    }

    @Override
    public String execute() {
        // On every call: resolve the flag, find the variant, delegate
        String flagValue = provider.getString("checkout-flow").orElse("");
        Supplier<CheckoutFlow> variantFactory = variants.get(flagValue);
        if (variantFactory != null) {
            return variantFactory.get().execute();
        }
        // Handle fallback (EXCEPTION, NOOP, etc.)
        throw new UnmatchedVariantException(...);
    }

    @Override
    public String toString() {
        return "FlagZenProxy[checkout-flow]";
    }
}
```

4. **Generates metadata** for runtime discovery:

```java
// Generated: CheckoutFlow_FlagZenMetadata.java
public class CheckoutFlow_FlagZenMetadata implements FeatureMetadata {
    @Override
    public Class<?> featureType() { return CheckoutFlow.class; }

    @Override
    public Object createProxy(FlagProvider provider) {
        Map<String, Supplier<CheckoutFlow>> variants = Map.of(
            "CLASSIC", () -> new ClassicCheckout(),
            "PREMIUM", () -> new PremiumCheckout()
        );
        return new CheckoutFlow_FlagZenProxy(provider, variants);
    }
}
```

The metadata is registered via `META-INF/services/com.flagzen.spi.FeatureMetadata` for runtime discovery.

### Phase 2: Runtime (Dispatch)

At runtime, you create a `FeatureDispatcher`:

```java
FlagProvider provider = new InMemoryFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

When you resolve a feature:

```java
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

The dispatcher:

1. Checks if it already has a cached proxy for this feature (it caches singletons per feature)
2. If not cached:
   - Uses `ServiceLoader` to discover `FeatureMetadata` implementations
   - Finds the metadata matching `CheckoutFlow.class`
   - Calls the metadata's factory method to create the proxy
   - Caches the proxy
3. Returns the cached proxy

When you call a method on the proxy:

```java
String result = flow.execute();
```

The generated proxy's method:

1. Queries the flag provider: `provider.getString("checkout-flow")`
2. Looks up the variant factory in the map
3. Creates (or reuses) the variant instance
4. Delegates the method call to it

**Key insight**: The flag is re-evaluated on every method call. Change the flag value, and the next call automatically uses the new variant. No restart needed.

## Architecture Patterns

### Ports-and-Adapters (Hexagonal Architecture)

FlagZen follows a strict ports-and-adapters pattern:

**Core module** (`flagzen-core`):
- Defines the contracts (ports): `FlagProvider` SPI, `ContextAccessor` SPI
- Provides the machinery: `FeatureDispatcher`, annotation processor, proxy generation
- Zero external dependencies — minimal, stable core

**Adapter modules** (`flagzen-env`, `flagzen-spring`, `flagzen-launchdarkly`, etc.):
- Implement ports: each provider adapts to a different flag source
- Depend inward on `flagzen-core`
- No adapter depends on another adapter

This ensures low coupling and high reusability.

### Code Generation vs. Reflection

Many flag frameworks use runtime reflection to discover and instantiate variants. FlagZen inverts this:

- **Reflection approach**: Load `@Variant` classes at runtime via `Class.forName()`, instantiate via `Constructor.newInstance()`
  - Pro: Dynamic discovery, minimal code size
  - Con: Slower (reflection overhead), incompatible with GraalVM native image, harder to debug

- **Generation approach** (FlagZen): Annotation processor generates dispatch code at compile time
  - Pro: Zero reflection, fast dispatch, GraalVM native-image compatible, debuggable bytecode
  - Con: Slightly larger generated code, requires annotation processor setup

FlagZen chose generation for performance, GraalVM compatibility, and debuggability.

## Module Dependencies

```text
flagzen-core (zero external deps)
  ↑
  ├─ flagzen-test (JUnit 5 testing)
  ├─ flagzen-spring (Spring Boot integration)
  ├─ flagzen-env (Environment variables)
  ├─ flagzen-reactor (Reactor context)
  ├─ flagzen-mutiny (Mutiny context)
  ├─ flagzen-launchdarkly (LaunchDarkly adapter)
  ├─ flagzen-togglz (Togglz adapter)
  └─ flagzen-openfeature (OpenFeature adapter)
```

Each extension module:
- Depends only on `flagzen-core` (and its SPI contracts)
- Does not depend on other extension modules
- Can be used independently

This design allows you to mix and match: use `flagzen-core` + `flagzen-env` in one project, `flagzen-core` + `flagzen-spring` + `flagzen-launchdarkly` in another, without carrying unnecessary dependencies.

## The SPI: How to Plug In

FlagZen's extensibility is built on Service Provider Interfaces (SPIs):

### FlagProvider SPI

```java
public interface FlagProvider {
    Optional<String> getString(String key);
    default Optional<String> getString(String key, EvaluationContext context) { ... }
    // Typed convenience methods: getBoolean, getInt, getLong, getDouble
}
```

To add a new flag source:

1. Implement `FlagProvider`:

```java
public class MyCustomProvider implements FlagProvider {
    @Override
    public Optional<String> getString(String key) {
        // Query your flag source (database, API, file, etc.)
        return getFromMySource(key);
    }
}
```

2. Register via `ServiceLoader`:

```text
# In META-INF/services/com.flagzen.spi.FlagProvider
com.example.MyCustomProvider
```

3. Or register explicitly:

```java
FlagProvider provider = new MyCustomProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

### ContextAccessor SPI

For providers that make context-aware decisions (e.g., "return PREMIUM variant for enterprise users"), the `ContextAccessor` SPI provides evaluation context:

```java
public interface ContextAccessor {
    Optional<EvaluationContext> getContext();
    int priority();
}
```

Implementations (like `ReactorContextAccessor`) supply context from framework-specific sources. The dispatcher queries all accessors in priority order and passes the first context found to the provider.

## Thread Safety

FlagZen is designed for thread-safe, concurrent access:

| Component | Strategy |
|-----------|----------|
| Generated proxies | Stateless; all dispatch is method calls and lookups. Thread-safe by design. |
| `FeatureDispatcher` | Caches proxies in `ConcurrentHashMap`. Thread-safe. |
| `InMemoryFlagProvider` | Uses `ConcurrentHashMap`. Thread-safe. |
| `EnvironmentVariableFlagProvider` | Eagerly loads env vars at construction into an immutable map. Thread-safe. |

The proxy re-evaluates the flag on every call, so flag changes are reflected immediately across all threads.

## Performance Characteristics

**Proxy construction**: Happens once per feature per dispatcher. Cost: service loading + proxy instantiation. Cached afterward.

**Method call dispatch**: O(1) operation:
- Query flag provider: typically `Map.get()` or similar constant-time operation
- Look up variant: `Map.get()` in the variant map
- Delegate method: direct method invocation

**Flag provider implementation matters**: If your provider queries a remote service synchronously on every `getString()` call, dispatch will be slow. Use caching or eager loading (as `EnvironmentVariableFlagProvider` does).

## Type Dispatch: Beyond Strings

The `FeatureType` enum supports STRING, INT, LONG, BOOLEAN, and DOUBLE. The generated proxy adapts to the declared type:

For a `BOOLEAN` feature:

```java
@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
public interface DarkMode { String theme(); }

@WhenTrue(of = DarkMode.class)
public class DarkTheme implements DarkMode { ... }

@WhenFalse(of = DarkMode.class)
public class LightTheme implements DarkMode { ... }
```

The generated proxy calls `provider.getBoolean("dark-mode")` instead of `getString()`, parsing the string value as a boolean. The annotation processor ensures the provider supports typed resolution.

## Testing Strategy

The `FlagZenExtension` (JUnit 5) provides test isolation:

```java
@ExtendWith(FlagZenExtension.class)
@PinFlag(feature = "checkout-flow", variant = "CLASSIC")
class CheckoutTest {
    @Test
    void testClassicFlow(CheckoutFlow flow) {
        // Extension creates a test-scoped InMemoryFlagProvider
        // @PinFlag pins checkout-flow -> CLASSIC
        // flow.resolve() returns the CLASSIC variant proxy
    }
}
```

Each test method gets its own `TestFlagContext` with an isolated provider, guaranteeing test independence without complex setup or mocking.

## Design Trade-Offs

### Why Compile-Time Generation?

Trade-off: More boilerplate at compile time vs. runtime simplicity.

**Chosen**: Compile-time generation.

**Rationale**:
- Zero runtime reflection ⇒ faster, GraalVM compatible, debuggable
- Compile-time validation ⇒ fail fast, before production
- Generated code is clear, inspectable bytecode

### Why Singleton Proxies?

Trade-off: Single proxy per feature vs. multiple instances.

**Chosen**: Singleton caching.

**Rationale**:
- Proxies are stateless; caching is safe
- Reduces GC pressure and object churn
- Simpler API (dispatcher doesn't need object management)

### Why Eager Evaluation of Flags?

Trade-off: Proxy re-evaluates on every call vs. caching within proxy.

**Chosen**: Eager evaluation on every call.

**Rationale**:
- Reflect flag changes immediately without restart
- No cache invalidation complexity
- Correct for dynamic flag systems (LaunchDarkly, etc.)
- Provider-level caching (if needed) is the responsibility of the provider

## Integration Points

### Spring Boot

`FlagZenAutoConfiguration` registers:
- `FeatureDispatcher` bean from the available `FlagProvider`
- Feature proxy beans for `@Autowired` injection
- Uses `ImportBeanDefinitionRegistrar` for dynamic bean discovery

This allows seamless injection:

```java
@Service
public class OrderService {
    @Autowired
    private CheckoutFlow checkout;
    // Injected by Spring, proxied by FlagZen
}
```

### Reactive (Reactor / Mutiny)

`ReactorContextAccessor` and `MutinyContextAccessor` provide evaluation context from reactive contexts. The dispatcher passes this context to providers, enabling user-scoped flag decisions in reactive pipelines.

## Where Code Lives

```text
src/main/java/com/flagzen/
  Feature.java                  # User-facing annotation
  Variant.java                  # User-facing annotation
  DefaultVariant.java           # User-facing annotation
  FeatureDispatcher.java        # User-facing interface
  FallbackStrategy.java         # Enum
  FeatureType.java              # Enum
  EvaluationContext.java        # Value object for context

src/main/java/com/flagzen/spi/
  FlagProvider.java             # SPI interface
  ContextAccessor.java          # SPI interface

src/main/java/com/flagzen/internal/
  DefaultFeatureDispatcher.java # Implementation (package-private)
  InMemoryFlagProvider.java     # Test provider
  # Generated code written here by annotation processor

src/main/java/com/flagzen/processor/
  FlagZenProcessor.java         # Annotation processor
  ProxyGenerator.java           # Code generator
  FeatureModel.java             # Internal data structures
```

## Summary

FlagZen's architecture achieves type-safe, compile-time-validated, zero-reflection feature flags through:

1. **Annotation-driven design**: Developers declare features and variants with annotations
2. **Compile-time processing**: Processor generates dispatch code and metadata
3. **Runtime dispatch**: Lightweight proxy delegation with dynamic flag re-evaluation
4. **Pluggable providers**: SPI-based extensibility for different flag sources
5. **Modular design**: Core + adapters, zero cross-dependencies between extensions

This design gives you the benefits of polymorphism, type safety, and dynamic dispatch without the complexity of reflection or the brittleness of conditional chains.

## Further Reading

- [Design Decisions](design-decisions.md) — rationale behind module split, eager loading, and other architectural choices
- [Why Zero Reflection](zero-reflection.md) — performance and GraalVM implications
- [Modules Reference](../reference/modules.md) — all available modules and their roles
