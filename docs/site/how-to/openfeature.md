# How to Connect to OpenFeature

Bridge FlagZen to any OpenFeature-compatible flag management service.

## Goal

Use LaunchDarkly, Flagd, Split, or any OpenFeature provider as your FlagZen flag source.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`
- OpenFeature SDK 1.12.0+
- An OpenFeature provider configured (e.g., Flagd, LaunchDarkly)

## Steps

### 1. Add the dependency

```gradle
dependencies {
    implementation("com.flagzen:flagzen-openfeature:1.1.0")
    implementation("dev.openfeature:sdk:1.12.0")
    // Plus your OpenFeature provider:
    implementation("dev.openfeature.contrib.providers:flagd:0.9.0")
}
```

### 2. Configure OpenFeature with your provider

Set up OpenFeature with your chosen backend (example uses Flagd):

```java
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProviderAndWait(new FlagdProvider("http://localhost:8013"));
```

### 3. Bridge to FlagZen

Get an OpenFeature `Client` and wrap it with `OpenFeatureFlagProvider`:

```java
Client client = api.getClient();
FlagProvider provider = new OpenFeatureFlagProvider(client);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

### 4. Use a named OpenFeature client (optional)

OpenFeature supports multiple named clients. Create a provider for a specific one:

```java
Client paymentsClient = OpenFeatureAPI.getInstance().getClient("payments");
FlagProvider provider = OpenFeatureFlagProvider.create(paymentsClient);
```

### 5. Resolve flags with evaluation context (optional)

Pass targeting context (user ID, attributes) for personalized flag resolution:

```java
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .build();

FlagProvider provider = OpenFeatureFlagProvider.create(client);
Optional<String> value = provider.getString("checkout-flow", context);
```

The adapter automatically maps FlagZen `EvaluationContext` to OpenFeature context.

### 6. Use with Spring Boot (optional)

Define the provider as a Spring bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        OpenFeatureAPI api = OpenFeatureAPI.getInstance();
        api.setProviderAndWait(new FlagdProvider("http://localhost:8013"));
        Client client = api.getClient();
        return OpenFeatureFlagProvider.create(client);
    }
}
```

Spring auto-configuration detects it and registers all `@Feature` proxies as beans.

## Result

FlagZen dispatches to variants based on flags from your OpenFeature backend. Type-safe dispatch, dynamic flag updates, and no string comparisons.

## See Also

- [Reference: OpenFeatureFlagProvider](../reference/modules.md) — adapter API
- [How-to: Evaluation Context](evaluation-context.md) — targeted flag resolution
- [How-to: Spring Boot Integration](spring-boot.md) — register proxies as beans
- [How-to: Custom Provider](custom-provider.md) — implement your own flag source
