# Component Boundaries -- flagzen-openfeature

## Package Structure

```
com.flagzen.openfeature
  OpenFeatureFlagProvider.java        (public, implements FlagProvider)
  EvaluationContextMapper.java        (package-private, pure mapping function)
```

Two classes total. This is a thin adapter module -- complexity lives in the OpenFeature SDK and in flagzen-core.

## Public API Surface

### `OpenFeatureFlagProvider`

| Member | Visibility | Description |
|--------|-----------|-------------|
| No-arg constructor | `public` | Uses `OpenFeatureAPI.getInstance().getClient()`. Required for ServiceLoader. |
| `create(Client)` | `public static` | Factory method accepting an explicit OpenFeature `Client` instance. |
| `getString(String)` | `public` | Override from `FlagProvider`. Delegates to `Client.getStringDetails`. |
| `getString(String, EvaluationContext)` | `public` | Override from `FlagProvider`. Context-aware string resolution. |
| `getBoolean(String)` | `public` | Override from `FlagProvider`. Delegates to `Client.getBooleanDetails`. |
| `getBoolean(String, EvaluationContext)` | `public` | Override from `FlagProvider`. Context-aware boolean resolution. |
| `getInt(String)` | `public` | Override from `FlagProvider`. Delegates to `Client.getIntegerDetails`. |
| `getInt(String, EvaluationContext)` | `public` | Override from `FlagProvider`. Context-aware integer resolution. |
| `getLong(String)` | `public` | Override from `FlagProvider`. Delegates to `Client.getIntegerDetails` + widening. |
| `getLong(String, EvaluationContext)` | `public` | Override from `FlagProvider`. Context-aware long resolution. |
| `getDouble(String)` | `public` | Override from `FlagProvider`. Delegates to `Client.getDoubleDetails`. |
| `getDouble(String, EvaluationContext)` | `public` | Override from `FlagProvider`. Context-aware double resolution. |

### `EvaluationContextMapper`

| Member | Visibility | Description |
|--------|-----------|-------------|
| Mapping method | `package-private static` | Converts `com.flagzen.EvaluationContext` to `dev.openfeature.sdk.EvaluationContext`. |

Package-private because it is an internal implementation detail. Only `OpenFeatureFlagProvider` uses it. No reason to expose it in the public API.

## Boundary Rules

1. **One public class**: `OpenFeatureFlagProvider` is the only public type in this module. Consumers interact exclusively through the `FlagProvider` interface.

2. **No public mapper**: The context mapper is an implementation detail. If a consumer needs to map contexts directly, they are likely doing something wrong -- the adapter handles it.

3. **No configuration object**: Unlike `flagzen-env` which has a builder with multiple configuration options (parsers, formatters, conflict strategy), this adapter has at most one configuration dimension (which `Client` to use). A factory method suffices; a builder would be overengineering.

4. **No subpackages**: Two classes do not warrant further package decomposition.

## ServiceLoader Registration

File: `META-INF/services/com.flagzen.spi.FlagProvider`

Content: `com.flagzen.openfeature.OpenFeatureFlagProvider`

This enables auto-discovery when the JAR is on the classpath.

## Dependency Direction

```
[OpenFeatureFlagProvider] --implements--> [FlagProvider] (flagzen-core)
[OpenFeatureFlagProvider] --delegates-to--> [Client] (openfeature-sdk)
[OpenFeatureFlagProvider] --uses--> [EvaluationContextMapper] (same package)
[EvaluationContextMapper] --reads--> [com.flagzen.EvaluationContext] (flagzen-core)
[EvaluationContextMapper] --creates--> [dev.openfeature.sdk.EvaluationContext] (openfeature-sdk)
```

Dependencies flow outward from the adapter to both its port (flagzen-core) and its external system (openfeature-sdk). The adapter has no inbound dependencies from other FlagZen modules.
