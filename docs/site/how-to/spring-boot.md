# How to Integrate with Spring Boot

Inject feature flag proxies as Spring beans using auto-configuration.

## Goal

Make all `@Feature` proxies available for `@Autowired` injection in Spring components.

## Prerequisites

- Spring Boot 3.x
- FlagZen basics: `@Feature`, `@Variant`

## Steps

### 1. Add the dependency

```gradle
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

### 2. Define your features and variants (as usual)

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = "STREAMLINED", of = CheckoutFlow.class)
public class StreamlinedCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "streamlined"; }
}
```

### 3. Inject into your Spring components

```java
@Service
public class OrderService {
    @Autowired
    private CheckoutFlow checkoutFlow;

    public String process() {
        return checkoutFlow.execute();
    }
}
```

The auto-configuration scans for all `@Feature`-annotated interfaces and registers generated proxies as Spring beans.

### 4. (Optional) Define a custom FlagProvider bean

By default, the auto-configuration uses an in-memory provider (safe for development, but returns empty for all flags). To use environment variables or a custom provider, define a `FlagProvider` bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new EnvironmentVariableFlagProvider();
    }
}
```

The auto-configuration detects your `FlagProvider` bean and uses it for all proxies.

### 5. (Optional) Use profile-based configuration

Define different providers per environment:

```java
@Configuration
@Profile("production")
public class ProdFlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new EnvironmentVariableFlagProvider();
    }
}

@Configuration
@Profile("test")
public class TestFlagsConfig {
    @Bean
    public FlagProvider flagProvider() {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "STREAMLINED");
        return provider;
    }
}
```

### 6. (Optional) Override the FeatureDispatcher bean

If you need custom dispatcher logic, define your own `FeatureDispatcher` bean:

```java
@Configuration
public class FlagsConfig {
    @Bean
    public FlagProvider flagProvider() { ... }

    @Bean
    public FeatureDispatcher featureDispatcher(FlagProvider provider) {
        return FeatureDispatcher.withProvider(provider);
    }
}
```

The auto-configuration backs off when it detects your custom bean.

## Result

Feature proxies are available as Spring beans throughout your application context. The dispatcher is shared across all injected proxies, ensuring consistent flag resolution.

## See Also

- [Reference: FlagZenAutoConfiguration](../reference/spring-auto-configuration.md) — auto-configuration entry point
- [How-to: Environment Variables](environment-variables.md) — configure flag sources
- [How-to: Custom Provider](custom-provider.md) — implement your own flag source
- [How-to: OpenFeature Integration](openfeature.md) — connect to LaunchDarkly, Flagd, etc.
