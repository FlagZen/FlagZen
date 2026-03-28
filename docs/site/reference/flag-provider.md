# FlagProvider SPI Reference

The pluggable service provider interface for flag value resolution.

## Interface

```java
public interface FlagProvider {
    Optional<String> getString(String key);
    default Optional<String> getString(String key, EvaluationContext context);
    default Optional<Boolean> getBoolean(String key);
    default Optional<Boolean> getBoolean(String key, EvaluationContext context);
    default OptionalInt getInt(String key);
    default OptionalInt getInt(String key, EvaluationContext context);
    default OptionalLong getLong(String key);
    default OptionalLong getLong(String key, EvaluationContext context);
    default OptionalDouble getDouble(String key);
    default OptionalDouble getDouble(String key, EvaluationContext context);
}
```

## Contract

### Abstract Method: `getString(String key)`

Returns the current value for the given flag key as a string.

**Signature**

```java
Optional<String> getString(String key)
```

**Parameters**

- `key` (String): The flag key to look up. Non-null.

**Returns**

- `Optional<String>`: The flag value if found, or `Optional.empty()` if the key is not set or unavailable.

**Thread Safety**

- Implementations must be thread-safe. Multiple threads may call this method concurrently.

**Stability**

- Flag values can change between calls (e.g., from a remote service). Callers should not assume cached values.

**Example**

```java
public class MyFlagProvider implements FlagProvider {
    private final Map<String, String> flags;

    public MyFlagProvider(Map<String, String> flags) {
        this.flags = flags;
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(flags.get(key));
    }
}
```

### Context Method: `getString(String key, EvaluationContext context)`

Returns the current value for the given flag key using the provided evaluation context.

**Signature**

```java
default Optional<String> getString(String key, EvaluationContext context) {
    return getString(key);
}
```

**Parameters**

- `key` (String): The flag key to look up. Non-null.
- `context` (EvaluationContext): The evaluation context containing targeting key and attributes. Non-null. See `EvaluationContext` reference.

**Returns**

- `Optional<String>`: The flag value if found, or `Optional.empty()`.

**Default Behavior**

- The default implementation ignores the context and delegates to `getString(String)`. This ensures backward compatibility for providers that do not need contextual resolution.

**Override When**

- Your provider supports targeting flags to specific users, segments, or request scopes
- You need custom logic based on the targeting key or attributes

**Example**

```java
public class UserSegmentFlagProvider implements FlagProvider {
    private final LaunchDarklyClient ldClient;

    @Override
    public Optional<String> getString(String key, EvaluationContext context) {
        User user = new User(context.targetingKey());
        context.attributes().forEach((k, v) -> user.withAttribute(k, (String) v));
        return Optional.ofNullable(ldClient.variation(key, user, null));
    }
}
```

## Typed Methods

All typed methods have default implementations that parse the string value from `getString()`.

### `getBoolean(String key)`

**Default Implementation**

```java
default Optional<Boolean> getBoolean(String key) {
    return getString(key)
            .filter(v -> v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false"))
            .map(Boolean::parseBoolean);
}
```

**Parsing**

- Only exact `"true"` or `"false"` (case-insensitive) are recognized
- Any other value returns `Optional.empty()`

### `getBoolean(String key, EvaluationContext context)`

**Default Implementation**

```java
default Optional<Boolean> getBoolean(String key, EvaluationContext context) {
    return getString(key, context)
            .filter(v -> v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false"))
            .map(Boolean::parseBoolean);
}
```

### `getInt(String key)`

**Default Implementation**

```java
default OptionalInt getInt(String key) {
    try {
        return getString(key)
                .map(Integer::parseInt)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    } catch (NumberFormatException e) {
        return OptionalInt.empty();
    }
}
```

**Parsing**

- Calls `Integer.parseInt(value)` on the string value
- Returns empty on `NumberFormatException`
- Valid range: -2,147,483,648 to 2,147,483,647

### `getInt(String key, EvaluationContext context)`

**Default Implementation**

```java
default OptionalInt getInt(String key, EvaluationContext context) {
    try {
        return getString(key, context)
                .map(Integer::parseInt)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    } catch (NumberFormatException e) {
        return OptionalInt.empty();
    }
}
```

### `getLong(String key)`

**Default Implementation**

```java
default OptionalLong getLong(String key) {
    try {
        return getString(key)
                .map(Long::parseLong)
                .map(OptionalLong::of)
                .orElse(OptionalLong.empty());
    } catch (NumberFormatException e) {
        return OptionalLong.empty();
    }
}
```

**Parsing**

- Calls `Long.parseLong(value)` on the string value
- Returns empty on `NumberFormatException`
- Valid range: -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807

### `getLong(String key, EvaluationContext context)`

**Default Implementation**

```java
default OptionalLong getLong(String key, EvaluationContext context) {
    try {
        return getString(key, context)
                .map(Long::parseLong)
                .map(OptionalLong::of)
                .orElse(OptionalLong.empty());
    } catch (NumberFormatException e) {
        return OptionalLong.empty();
    }
}
```

### `getDouble(String key)`

**Default Implementation**

```java
default OptionalDouble getDouble(String key) {
    try {
        return getString(key)
                .map(Double::parseDouble)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
    } catch (NumberFormatException e) {
        return OptionalDouble.empty();
    }
}
```

**Parsing**

- Calls `Double.parseDouble(value)` on the string value
- Returns empty on `NumberFormatException`
- Valid range: IEEE 754 double range

### `getDouble(String key, EvaluationContext context)`

**Default Implementation**

```java
default OptionalDouble getDouble(String key, EvaluationContext context) {
    try {
        return getString(key, context)
                .map(Double::parseDouble)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
    } catch (NumberFormatException e) {
        return OptionalDouble.empty();
    }
}
```

## Implementing FlagProvider

### Minimal Implementation

```java
public class InMemoryFlagProvider implements FlagProvider {
    private final Map<String, String> flags = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(flags.get(key));
    }

    public void set(String key, String value) {
        flags.put(key, value);
    }
}
```

### With Context Support

```java
public class ContextAwareFlagProvider implements FlagProvider {
    private final FlagStore store;

    @Override
    public Optional<String> getString(String key) {
        return store.globalFlag(key);
    }

    @Override
    public Optional<String> getString(String key, EvaluationContext context) {
        String targetingKey = context.targetingKey();
        if (targetingKey != null) {
            Optional<String> userSpecific = store.userFlag(targetingKey, key);
            if (userSpecific.isPresent()) {
                return userSpecific;
            }
        }
        return getString(key);
    }
}
```

## Discovery and Registration

### ServiceLoader Discovery

Implementations are discovered via `java.util.ServiceLoader`:

1. Create a `META-INF/services/com.flagzen.spi.FlagProvider` file in your JAR
2. List one fully qualified class name per line
3. The file is discovered automatically when the provider module is on the classpath

Example `META-INF/services/com.flagzen.spi.FlagProvider`:

```text
com.flagzen.env.EnvironmentVariableFlagProvider
```

### Programmatic Registration

Register a provider explicitly when constructing `FeatureDispatcher`:

```java
FlagProvider provider = new MyFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

## Exception Handling

### Should Not Throw

Implementations should not throw exceptions for missing or unparseable values. Return `Optional.empty()` or the appropriate empty optional type instead:

- Missing key → `Optional.empty()` (for string) or empty variant (for typed)
- Unparseable value → return empty (e.g., `OptionalInt.empty()` if string cannot be parsed as int)
- Provider unavailable → return empty (graceful fallback to default variants)

### May Throw

- Constructor errors are acceptable if configuration is invalid
- `IllegalArgumentException` for null keys

## Best Practices

- **Thread-safe**: All implementations must be thread-safe.
- **Stateless or immutable**: If mutable (e.g., for testing), document the mutability clearly.
- **Fast**: Flag resolution should be O(1) or near-constant time. Avoid blocking I/O on flag reads.
- **Caching**: If querying a remote service, implement caching with a reasonable TTL.
- **Default behavior**: Override only the methods you need. Use default implementations for others.
- **Documentation**: Document the flag key naming convention, supported types, and any configuration requirements.

## Related

- `EvaluationContext` — context passed to context-aware providers
- `ContextAccessor` — ambient context source SPI
- `FeatureDispatcher` — consumes flag provider for dispatch
