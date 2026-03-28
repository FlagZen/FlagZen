# FeatureDispatcher Reference

Resolves feature interfaces to their active variant proxy implementations.

## Interface

```java
public interface FeatureDispatcher {
    <T> T resolve(Class<T> featureType);
    <T> T resolve(Class<T> featureType, EvaluationContext context);
}
```

## Methods

### `resolve(Class<T> featureType)`

Resolves a feature interface to a dispatch proxy implementing that interface.

**Signature**
```java
<T> T resolve(Class<T> featureType)
```

**Type Parameter**
- `T`: The feature interface type to resolve. Must be annotated with `@Feature`.

**Parameters**
- `featureType` (Class<T>): The feature interface class. Non-null.

**Returns**
- A proxy object that implements `T` and dispatches method calls to the active variant based on the current flag value.

**Behavior**
- The proxy is created once per feature per dispatcher and cached as a singleton
- Each method call on the proxy re-evaluates the flag value (dynamic dispatch)
- The flag value is retrieved from the configured `FlagProvider`
- If no variant matches, behavior is determined by the feature's `FallbackStrategy`

**Exceptions**
- `IllegalArgumentException`: if `featureType` is null
- `NoProviderException`: if no `FlagProvider` is available
- `UnmatchedVariantException`: if no variant matches and `fallback = FallbackStrategy.EXCEPTION`

**Example**

```java
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(flagProvider);

// First call: proxy created and cached
CheckoutFlow flow1 = dispatcher.resolve(CheckoutFlow.class);

// Second call: cached proxy returned
CheckoutFlow flow2 = dispatcher.resolve(CheckoutFlow.class);

// Same object
assert flow1 == flow2;

// On each method call, the flag is re-evaluated
String result = flow1.execute(); // reads checkout-flow flag, calls active variant
```

### `resolve(Class<T> featureType, EvaluationContext context)`

Resolves a feature interface to a dispatch proxy using a provided evaluation context.

**Signature**
```java
<T> T resolve(Class<T> featureType, EvaluationContext context)
```

**Type Parameters**
- `T`: The feature interface type to resolve. Must be annotated with `@Feature`.

**Parameters**
- `featureType` (Class<T>): The feature interface class. Non-null.
- `context` (EvaluationContext): The evaluation context for targeted resolution. Non-null.

**Returns**
- A proxy that dispatches with access to the provided context.

**Behavior**
- The proxy is created once and cached (same caching as `resolve(Class<T>)`)
- The provided context is made available to the flag provider via `FlagProvider.getString(key, context)` and typed equivalents
- Each method call re-evaluates the flag using the context
- Context accessors (ambient context sources) are consulted only when no explicit context is passed here

**Example**

```java
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(flagProvider);

EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .build();

CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class, context);

// On each call, the context is passed to the flag provider
String result = flow.execute(); // provider can make targeted decision based on user-7291 / enterprise plan
```

## Creating a FeatureDispatcher

### Factory Methods

**`FeatureDispatcher.withProvider(FlagProvider provider)`**

Creates a dispatcher with an explicit flag provider.

```java
FlagProvider provider = new InMemoryFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

**`FeatureDispatcher.discover()`**

Creates a dispatcher using the first `FlagProvider` discovered via `ServiceLoader`.

```java
FeatureDispatcher dispatcher = FeatureDispatcher.discover();
```

If no provider is found, throws `NoProviderException`.

## Proxy Behavior

### Dynamic Dispatch on Every Method Call

Each method call on the proxy evaluates the flag value at that moment:

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    public String execute() { return "classic"; }
}

@Variant(value = "PREMIUM", of = CheckoutFlow.class)
public class PremiumCheckout implements CheckoutFlow {
    public String execute() { return "premium"; }
}

FlagProvider provider = new InMemoryFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);

provider.set("checkout-flow", "CLASSIC");
String result1 = flow.execute(); // "classic"

provider.set("checkout-flow", "PREMIUM");
String result2 = flow.execute(); // "premium" (no restart needed)
```

### Singleton Caching Per Feature Per Dispatcher

The same proxy object is returned for multiple calls to `resolve()`:

```java
CheckoutFlow flow1 = dispatcher.resolve(CheckoutFlow.class);
CheckoutFlow flow2 = dispatcher.resolve(CheckoutFlow.class);
assert flow1 == flow2; // true
```

This is safe because the proxy is stateless -- dispatch behavior depends only on the current flag value, not on proxy state.

### Context Resolution Order

When no explicit context is passed to `resolve()`, the dispatcher consults `ContextAccessor` implementations:

1. Check if explicit context was passed to `resolve(Class<T>, EvaluationContext)`
   - If yes, use it and skip steps 2-3
2. Load all `ContextAccessor` implementations via `ServiceLoader`
3. Sort by `priority()` (lower value = higher priority)
4. Call each accessor in order until one returns a non-empty context
5. Pass the first non-empty context to the flag provider, or pass none if all are empty

This allows libraries like Reactor and Mutiny to provide implicit context from request-scoped or reactive contexts.

## Fallback Strategies

The `FallbackStrategy` enum on `@Feature` determines what happens when no variant matches:

| Strategy | Behavior |
|----------|----------|
| `EXCEPTION` (default) | Throw `UnmatchedVariantException` |
| `NOOP` | Return default value (null for objects, 0 for primitives, false for boolean) |

The proxy is not aware of the strategy -- the generated proxy code handles it.

## Thread Safety

- `FeatureDispatcher` is thread-safe
- Proxies are immutable after creation and are thread-safe
- Multiple threads can concurrently call `resolve()` and invoke methods on proxies
- Flag values can change between calls (proxies re-evaluate on each call)

## Exception Handling

### `UnmatchedVariantException`

Thrown when:
- No variant matches the flag value
- No `@DefaultVariant` is defined
- Feature's `fallback = FallbackStrategy.EXCEPTION`

Example:

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    public String execute() { return "classic"; }
}

FlagProvider provider = new InMemoryFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);

provider.set("checkout-flow", "UNKNOWN");
flow.execute(); // throws UnmatchedVariantException
```

### `NoProviderException`

Thrown when no `FlagProvider` is available (e.g., when using `discover()` with no providers on classpath).

## Integration

### Spring Boot

Spring Boot auto-configuration (`flagzen-spring`) registers a `FeatureDispatcher` bean:

```java
@Service
public class OrderService {
    @Autowired
    private CheckoutFlow checkoutFlow;
    // dispatcher resolved automatically
}
```

### JUnit 5

The testing extension (`flagzen-test`) provides a test-scoped dispatcher:

```java
@ExtendWith(FlagZenExtension.class)
@PinFlag(feature = "checkout-flow", variant = "CLASSIC")
class CheckoutTest {
    @Test
    void testClassicFlow(CheckoutFlow flow) {
        assertEquals("classic", flow.execute());
    }
}
```

## Best Practices

- **Create one dispatcher per application**: Reuse the same `FeatureDispatcher` instance across your application. Proxies are cached, so subsequent calls are cheap.
- **Do not cache proxies separately**: The dispatcher caches proxies for you. Do not create and store your own proxy references.
- **Pass context for targeted decisions**: If your flag provider supports user/segment targeting, pass an `EvaluationContext` to `resolve(Class, EvaluationContext)`.
- **Plan for missing flags**: Use `@DefaultVariant` or `FallbackStrategy.NOOP` for optional features to gracefully degrade when flags are absent.

## Related

- `@Feature` — marks feature interfaces
- `@Variant` — marks variant implementations
- `FlagProvider` — pluggable flag value source
- `EvaluationContext` — context for targeted resolution
- `ContextAccessor` — ambient context source SPI
