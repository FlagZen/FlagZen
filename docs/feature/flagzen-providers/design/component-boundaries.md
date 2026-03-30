# Component Boundaries -- flagzen-providers

## 1. flagzen-launchdarkly

### Package Structure

```
com.flagzen.launchdarkly
  LaunchDarklyFlagProvider.java        (public, implements FlagProvider)
  EvaluationContextMapper.java         (package-private, maps to LDContext)
```

Two classes. Follows the same minimal pattern as `flagzen-openfeature`.

### Public API Surface

#### `LaunchDarklyFlagProvider`

| Member | Visibility | Description |
| --- | --- | --- |
| `create(LDClient)` | `public static` | Factory method accepting an explicit `LDClient` instance |
| `getString(String)` | `public` | Override from `FlagProvider`. Delegates to `LDClient.stringVariationDetail`. |
| `getString(String, EvaluationContext)` | `public` | Context-aware string resolution via `LDContext`. |
| `getBoolean(String)` | `public` | Override from `FlagProvider`. Delegates to `LDClient.boolVariationDetail`. |
| `getBoolean(String, EvaluationContext)` | `public` | Context-aware boolean resolution. |
| `getInt(String)` | `public` | Override from `FlagProvider`. Delegates to `LDClient.intVariationDetail`. |
| `getInt(String, EvaluationContext)` | `public` | Context-aware integer resolution. |
| `getLong(String)` | `public` | Override from `FlagProvider`. Delegates to `LDClient.jsonValueVariationDetail`. |
| `getLong(String, EvaluationContext)` | `public` | Context-aware long resolution via JSON value. |
| `getDouble(String)` | `public` | Override from `FlagProvider`. Delegates to `LDClient.doubleVariationDetail`. |
| `getDouble(String, EvaluationContext)` | `public` | Context-aware double resolution. |

**No no-arg constructor / no ServiceLoader registration.** `LDClient` requires an SDK key for construction; there is no sensible default.

#### `EvaluationContextMapper`

| Member | Visibility | Description |
| --- | --- | --- |
| `toLDContext(EvaluationContext)` | `package-private static` | Converts FlagZen `EvaluationContext` to `LDContext`. |

### Anonymous Context Handling

When `EvaluationContext` is not provided (non-context overloads), the adapter must still pass an `LDContext` to LaunchDarkly (it is required). The adapter uses an anonymous context: `LDContext.builder(ContextKind.DEFAULT, "anonymous").anonymous(true).build()`. This is a singleton, allocated once and reused.

### Dependency Direction

```
[LaunchDarklyFlagProvider] --implements--> [FlagProvider] (flagzen-core)
[LaunchDarklyFlagProvider] --delegates-to--> [LDClient] (launchdarkly-sdk)
[LaunchDarklyFlagProvider] --uses--> [EvaluationContextMapper] (same package)
[EvaluationContextMapper] --reads--> [com.flagzen.EvaluationContext] (flagzen-core)
[EvaluationContextMapper] --creates--> [LDContext] (launchdarkly-sdk)
```

---

## 2. flagzen-togglz

### Package Structure

```
com.flagzen.togglz
  TogglzFlagProvider.java              (public, implements FlagProvider)
  FeatureLookup.java                   (package-private, string-to-Feature cache)
```

Two classes. No `EvaluationContextMapper` -- Togglz context is not mapped (see ADR-023).

### Public API Surface

#### `TogglzFlagProvider`

| Member | Visibility | Description |
| --- | --- | --- |
| `create(FeatureManager)` | `public static` | Factory method accepting an explicit `FeatureManager` instance |
| `getString(String)` | `public` | Override. Reads strategy parameter `"value"`, falls back to enabled-as-string. |
| `getString(String, EvaluationContext)` | `public` | Override. Delegates to `getString(key)` -- context ignored (ADR-023). |
| `getBoolean(String)` | `public` | Override. Delegates to `FeatureState.isEnabled()`. |
| `getBoolean(String, EvaluationContext)` | `public` | Override. Delegates to `getBoolean(key)` -- context ignored (ADR-023). |

**Only 4 methods overridden** (not 10). `getInt`, `getLong`, `getDouble` use `FlagProvider` default implementations, which parse from `getString`. This is correct because Togglz has no native numeric types.

**No no-arg constructor / no ServiceLoader registration.** `FeatureManager` requires configuration for construction.

#### `FeatureLookup`

| Member | Visibility | Description |
| --- | --- | --- |
| `resolve(String)` | `package-private` | Returns `Optional<Feature>` for a string key. Builds and caches mapping from `FeatureManager.getFeatures()`. |

This class caches the `String -> Feature` mapping (case-insensitive) on first access. The cache is a `ConcurrentHashMap` populated lazily. Features in Togglz are static (registered at application startup), so the cache does not need invalidation.

### Dependency Direction

```
[TogglzFlagProvider] --implements--> [FlagProvider] (flagzen-core)
[TogglzFlagProvider] --delegates-to--> [FeatureManager] (togglz-core)
[TogglzFlagProvider] --uses--> [FeatureLookup] (same package)
[FeatureLookup] --reads--> [FeatureManager.getFeatures()] (togglz-core)
```

---

## 3. Boundary Rules (Both Adapters)

1. **One public class per module**: The `*FlagProvider` class is the only public type. Consumers interact exclusively through the `FlagProvider` interface.

2. **No public internal types**: Mappers, lookups, and utilities are package-private implementation details.

3. **No configuration object / builder**: Each adapter has exactly one configuration dimension (which client/manager to use). A factory method suffices.

4. **No subpackages**: Two or three classes do not warrant further package decomposition.

5. **No cross-adapter dependencies**: `flagzen-launchdarkly` and `flagzen-togglz` are completely independent. Neither depends on the other or on `flagzen-openfeature`.
