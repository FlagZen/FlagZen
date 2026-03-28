# How to Write a Custom FlagProvider

Implement the `FlagProvider` interface to source flags from your own backend.

## Goal

Create a custom flag provider for databases, APIs, configuration services, or any other source.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- Understanding of the `FlagProvider` SPI

## Steps

### 1. Implement the FlagProvider interface

Create a class implementing `com.flagzen.spi.FlagProvider`:

```java
import com.flagzen.spi.FlagProvider;
import java.util.Optional;

public class CustomFlagProvider implements FlagProvider {
    @Override
    public Optional<String> getString(String key) {
        // Fetch flag value from your source
        String value = fetchFromDatabase(key);
        return Optional.ofNullable(value);
    }

    private String fetchFromDatabase(String key) {
        // Your implementation here
        return null;
    }
}
```

### 2. Override type-specific methods for efficiency (optional)

The default implementations parse strings; override for native types to avoid round-tripping:

```java
public class CustomFlagProvider implements FlagProvider {
    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(fetchStringFromDatabase(key));
    }

    @Override
    public OptionalInt getInt(String key) {
        Integer value = fetchIntFromDatabase(key);
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        Boolean value = fetchBooleanFromDatabase(key);
        return Optional.ofNullable(value);
    }
}
```

### 3. Support evaluation context (optional)

If your flag source supports targeting (user ID, attributes, segments), override the context-aware methods:

```java
@Override
public Optional<String> getString(String key, EvaluationContext context) {
    String userId = context.targetingKey();
    String plan = (String) context.attributes().get("plan");

    // Fetch with targeting
    String value = fetchForUser(key, userId, plan);
    return Optional.ofNullable(value);
}

@Override
public OptionalInt getInt(String key, EvaluationContext context) {
    String userId = context.targetingKey();
    Integer value = fetchIntForUser(key, userId);
    return value != null ? OptionalInt.of(value) : OptionalInt.empty();
}
```

### 4. Register via ServiceLoader (optional)

For automatic discovery on the classpath, register your provider in `META-INF/services/`:

Create file: `src/main/resources/META-INF/services/com.flagzen.spi.FlagProvider`

```
com.myapp.flags.CustomFlagProvider
```

Then FlagZen can discover it automatically:

```java
FlagProvider provider = ServiceLoader.load(FlagProvider.class).findFirst().orElseThrow();
```

### 5. Use the provider

```java
FlagProvider provider = new CustomFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
flow.execute(); // Dispatches based on your provider's flags
```

### 6. Use with Spring Boot

Define it as a Spring bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new CustomFlagProvider();
    }
}
```

Spring auto-configuration detects it and registers all `@Feature` proxies.

## Result

Your custom provider supplies flag values for FlagZen dispatch. Override only the methods you need; defaults parse strings for backward compatibility.

## See Also

- [Reference: FlagProvider SPI](../reference/flag-provider.md) — interface contract
- [Reference: EvaluationContext](../reference/flag-provider.md) — targeted resolution
- [How-to: Environment Variables](environment-variables.md) — built-in env var provider
- [How-to: OpenFeature Integration](openfeature.md) — adapter for OpenFeature backends
