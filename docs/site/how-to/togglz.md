# How to Connect to Togglz

Bridge FlagZen to the Togglz feature toggle library.

## Goal

Use Togglz as your FlagZen flag source for boolean toggles and string-valued features.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- Togglz 4.x configured with a `FeatureManager`
- Togglz features defined as Java enums

## Steps

### 1. Add the dependency

```gradle
dependencies {
    implementation("com.flagzen:flagzen-togglz:1.2.0")
}
```

Togglz Core is included transitively.

### 2. Set up your Togglz features

Define features as Togglz enums (if you haven't already):

```java
public enum MyFeatures implements Feature {
    DARK_MODE,
    CHECKOUT_FLOW;
}
```

### 3. Bridge to FlagZen

Wrap your `FeatureManager` with `TogglzFlagProvider`:

```java
FeatureManager featureManager = // your existing FeatureManager
FlagProvider provider = TogglzFlagProvider.create(featureManager);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
DarkMode darkMode = dispatcher.resolve(DarkMode.class);
```

### 4. Resolve boolean flags

Boolean flags map directly to Togglz's enabled/disabled state:

```java
Optional<Boolean> enabled = provider.getBoolean("DARK_MODE");
```

Feature key lookup is case-insensitive: `"dark_mode"`, `"Dark_Mode"`, and `"DARK_MODE"` all resolve to the same Togglz feature.

### 5. Use string values via strategy parameters (optional)

Togglz is primarily a boolean toggle system, but you can attach string values using activation strategy parameters. Set a parameter named `"value"` on the feature state:

```java
FeatureState state = new FeatureState(MyFeatures.CHECKOUT_FLOW, true);
state.setStrategyId("custom-strategy");
state.setParameter("value", "PREMIUM");
featureManager.setFeatureState(state);
```

Then resolve it:

```java
Optional<String> variant = provider.getString("CHECKOUT_FLOW");
// Returns Optional.of("PREMIUM")
```

If no `"value"` parameter is set, `getString` returns the enabled state as a string (`"true"` or `"false"`).

### 6. Use numeric values (optional)

Numeric types (`getInt`, `getLong`, `getDouble`) work by parsing the string value. Set a numeric `"value"` parameter:

```java
state.setParameter("value", "42");
featureManager.setFeatureState(state);

OptionalInt retries = provider.getInt("CHECKOUT_FLOW");
// Returns OptionalInt.of(42)
```

### 7. Use with Spring Boot (optional)

Define the provider as a Spring bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider(FeatureManager featureManager) {
        return TogglzFlagProvider.create(featureManager);
    }
}
```

Spring auto-configuration detects it and registers all `@Feature` proxies as beans.

## Limitations

### No explicit evaluation context

Togglz manages user context through its own `UserProvider` mechanism (thread-local). FlagZen's explicit `EvaluationContext` parameter cannot be mapped to Togglz's model.

Context-aware methods (`getString(key, ctx)`, `getBoolean(key, ctx)`) delegate to their non-context versions. The `EvaluationContext` parameter is ignored. An INFO log is emitted on the first context-aware call:

> TogglzFlagProvider does not support explicit EvaluationContext. Configure a Togglz UserProvider for user targeting.

If you need user targeting with Togglz, configure a `UserProvider` in your application as you normally would with Togglz.

### Boolean-first design

Togglz was designed as a boolean toggle library. String, int, long, and double resolution all depend on the `"value"` strategy parameter convention. If your use case is primarily typed dispatch with multiple variants, consider [LaunchDarkly](launchdarkly.md) or [OpenFeature](openfeature.md) instead.

## Result

FlagZen dispatches to variants based on Togglz feature state. Boolean toggles work natively. String and numeric values use the strategy parameter convention.

## See Also

- [How-to: Spring Boot Integration](spring-boot.md) -- register proxies as beans
- [How-to: LaunchDarkly](launchdarkly.md) -- full typed resolution with evaluation context
- [How-to: OpenFeature](openfeature.md) -- vendor-neutral alternative
- [How-to: Custom Provider](custom-provider.md) -- implement your own flag source
