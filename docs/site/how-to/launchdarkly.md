# How to Connect to LaunchDarkly

Bridge FlagZen to LaunchDarkly's feature flag platform.

## Goal

Use LaunchDarkly as your FlagZen flag source with native typed resolution and full evaluation context support.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- LaunchDarkly account with an SDK key
- LaunchDarkly Java Server SDK 7.x

## Steps

### 1. Add the dependency

```gradle
dependencies {
    implementation("com.flagzen:flagzen-launchdarkly:1.2.0")
}
```

The LaunchDarkly Java Server SDK is included transitively.

### 2. Create the LaunchDarkly client

```java
LDClient ldClient = new LDClient("sdk-your-key");
```

The client connects to LaunchDarkly and streams flag updates. See the [LaunchDarkly Java SDK docs](https://docs.launchdarkly.com/sdk/server-side/java) for configuration options.

### 3. Bridge to FlagZen

Wrap the client with `LaunchDarklyFlagProvider`:

```java
FlagProvider provider = LaunchDarklyFlagProvider.create(ldClient);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

### 4. Resolve flags with evaluation context (optional)

Pass targeting context for personalized flag resolution:

```java
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .attribute("age", 32)
    .build();

Optional<String> value = provider.getString("checkout-flow", context);
```

The adapter maps the targeting key to `LDContext` key and attributes to custom `LDValue` fields. When no context is provided, an anonymous `LDContext` is used.

### 5. Use typed flag resolution (optional)

The adapter delegates to LaunchDarkly's native typed methods, avoiding string round-tripping:

```java
Optional<Boolean> enabled = provider.getBoolean("dark-mode");
OptionalInt retries = provider.getInt("max-retries");
OptionalDouble ratio = provider.getDouble("sampling-ratio");
OptionalLong limit = provider.getLong("rate-limit");
```

Long values use LaunchDarkly's JSON value API, supporting the full `long` range (unlike OpenFeature which is limited to `int` range).

### 6. Use with Spring Boot (optional)

Define the provider as a Spring bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        LDClient ldClient = new LDClient("sdk-your-key");
        return LaunchDarklyFlagProvider.create(ldClient);
    }
}
```

Spring auto-configuration detects it and registers all `@Feature` proxies as beans.

## How absence detection works

The adapter uses LaunchDarkly's evaluation reason to detect absent flags:

| Reason | Result | Why |
| ------ | ------ | --- |
| `FALLTHROUGH`, `TARGET_MATCH`, `RULE_MATCH` | Value returned | Real resolution |
| `OFF` | Off-variation value returned | Deliberately configured off-value |
| `ERROR` (flag not found, malformed, etc.) | Empty | Evaluation failed |
| `PREREQUISITE_FAILED` | Empty | Prerequisite flag not satisfied |

This means a flag that is turned OFF in LaunchDarkly still returns a value (the configured off-variation), which FlagZen dispatches on normally.

## Result

FlagZen dispatches to variants based on flags from LaunchDarkly. All five types (string, boolean, int, long, double) are resolved natively without string parsing.

## See Also

- [How-to: Evaluation Context](evaluation-context.md) -- targeted flag resolution
- [How-to: Spring Boot Integration](spring-boot.md) -- register proxies as beans
- [How-to: OpenFeature](openfeature.md) -- vendor-neutral alternative
- [How-to: Custom Provider](custom-provider.md) -- implement your own flag source
