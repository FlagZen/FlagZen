# Spring Boot Auto-Configuration Reference

Spring Boot auto-configuration for FlagZen feature flag injection.

**Module**: `flagzen-spring`

## Overview

The Spring Boot starter automatically:

1. Creates a `FeatureDispatcher` bean from the available `FlagProvider`
2. Discovers all `@Feature` interfaces via `ServiceLoader<FeatureMetadata>`
3. Registers a Spring bean for each feature interface
4. Enables `@Autowired` injection of feature proxies into Spring beans

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
}
```

Requires Spring Boot 2.7+ and Java 17+.

## Auto-Configuration Class

```java
@AutoConfiguration
@ConditionalOnClass(FeatureDispatcher.class)
@Import(FeatureProxyRegistrar.class)
public class FlagZenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FeatureDispatcher featureDispatcher(
            ObjectProvider<FlagProvider> flagProvider) {
        FlagProvider provider = flagProvider.getIfAvailable(() ->
            new InMemoryFlagProvider());
        return new DefaultFeatureDispatcher(provider);
    }
}
```

## Beans Created

### FeatureDispatcher

A singleton `FeatureDispatcher` bean that resolves feature proxies.

- **Type**: `com.flagzen.FeatureDispatcher`
- **Scope**: Singleton
- **Lazy Init**: No (created eagerly during auto-configuration)
- **Condition**: `@ConditionalOnMissingBean` — if you register your own `FeatureDispatcher` bean, the auto-configuration bean is skipped

### Feature Proxy Beans

For each `@Feature` interface discovered, a Spring bean is registered with:

- **Type**: The feature interface class
- **Scope**: Singleton
- **Lazy Init**: Yes (created on first injection)
- **Name**: Decapitalized interface name (e.g., `CheckoutFlow` → `checkoutFlow`)
- **Instance**: Resolved via `FeatureDispatcher.resolve(featureType)` on first access

## FlagProvider Detection

The auto-configuration looks for a `FlagProvider` bean in the Spring context:

1. If a `FlagProvider` bean is found: use it to create `FeatureDispatcher`
2. If no `FlagProvider` is found: use `InMemoryFlagProvider` (all flags return empty)

### Explicit FlagProvider

Register a `FlagProvider` bean to use a specific flag source:

```java
@Configuration
public class FlagConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new EnvironmentVariableFlagProvider.builder()
            .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
            .format(FlagKeyFormats.kebabCase())
            .build();
    }
}
```

The `FlagZenAutoConfiguration` will automatically use this provider.

### Multiple Providers (ServiceLoader)

If multiple `FlagProvider` implementations are on the classpath, the first one discovered by `ServiceLoader` is used (order is undefined). To guarantee a specific provider:

1. Register a `FlagProvider` bean explicitly (as above), or
2. Exclude other providers from the classpath

## Feature Injection

With auto-configuration enabled, inject feature proxies directly into Spring beans:

```java
@Service
public class OrderService {
    private final CheckoutFlow checkoutFlow;
    private final DarkMode darkMode;

    public OrderService(CheckoutFlow checkoutFlow, DarkMode darkMode) {
        this.checkoutFlow = checkoutFlow;
        this.darkMode = darkMode;
    }

    public void processOrder() {
        String result = checkoutFlow.execute();
        String theme = darkMode.theme();
        // ...
    }
}
```

Or with field injection:

```java
@Controller
public class UIController {
    @Autowired
    private CheckoutFlow checkoutFlow;

    @Autowired
    private DarkMode darkMode;

    @GetMapping("/checkout")
    public String checkout() {
        return checkoutFlow.execute();
    }
}
```

## Customizing the Dispatcher

To use a custom `FeatureDispatcher` implementation or configuration, register your own bean:

```java
@Configuration
public class FlagConfig {
    @Bean
    public FeatureDispatcher featureDispatcher(FlagProvider provider) {
        // Custom dispatcher with specialized logic
        return new CustomFeatureDispatcher(provider);
    }
}
```

The auto-configuration's `@ConditionalOnMissingBean` ensures your custom bean is used instead.

## Conditional Beans

All beans are conditional on the presence of `FeatureDispatcher` on the classpath:

```java
@ConditionalOnClass(FeatureDispatcher.class)
public class FlagZenAutoConfiguration { ... }
```

If `flagzen-core` is not on the classpath, auto-configuration is skipped entirely.

## Disabling Auto-Configuration

To disable auto-configuration entirely:

```properties
spring.autoconfigure.exclude=com.flagzen.spring.FlagZenAutoConfiguration
```

Or in `@SpringBootTest`:

```java
@SpringBootTest(
    excludeAutoConfiguration = FlagZenAutoConfiguration.class
)
class MyTest { ... }
```

## Configuration Properties

FlagZen auto-configuration does not currently expose configuration properties. Configuration is done via:

- Explicit `FlagProvider` bean registration (recommended)
- Environment variables (for `EnvironmentVariableFlagProvider`)
- Java system properties (if using a custom provider)

Future releases may add properties like `flagzen.fallback-strategy` or `flagzen.context-timeout`.

## Spring Integration Example

Complete example with Spring Boot:

```gradle
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.0.0")
    implementation("com.flagzen:flagzen-spring:1.1.0")
    implementation("com.flagzen:flagzen-env:1.1.0")
}
```

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

@Configuration
public class FlagConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new EnvironmentVariableFlagProvider.builder()
            .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
            .format(FlagKeyFormats.kebabCase())
            .build();
    }
}

@Service
public class OrderService {
    private final CheckoutFlow checkout;

    public OrderService(CheckoutFlow checkout) {
        this.checkout = checkout;
    }

    public void processOrder() {
        String result = checkout.execute();
        System.out.println("Using: " + result);
    }
}

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // With env var: FLAGZEN_CHECKOUT_FLOW=PREMIUM
        // OrderService will inject PremiumCheckout
        SpringApplication.run(Application.class, args);
    }
}
```

## Testing with Spring Boot

Use `@SpringBootTest` with `FlagZenExtension`:

```java
@SpringBootTest
@ExtendWith(FlagZenExtension.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    @PinFlag(feature = "checkout-flow", variant = "CLASSIC")
    void testClassicCheckout() {
        orderService.processOrder();
        // Verifies behavior with CLASSIC variant
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void testPremiumCheckout() {
        orderService.processOrder();
        // Verifies behavior with PREMIUM variant
    }
}
```

## Troubleshooting

### "No FlagProvider bean found" Warning

```
WARN ... No FlagProvider bean found; activating InMemoryFlagProvider fallback (dev/test only)
```

**Cause**: No `FlagProvider` bean is registered and no provider is found via `ServiceLoader`.

**Fix**: Register a `FlagProvider` bean explicitly (see "Explicit FlagProvider" section above).

### Feature Proxy Not Injected

**Symptom**: `NoSuchBeanDefinitionException` or bean not found when autowiring a feature interface.

**Causes**:

1. Feature interface is not in the classpath (missing dependency)
2. Annotation processor did not generate metadata (missing `annotationProcessor` dependency in build)
3. Auto-configuration is disabled (check `spring.autoconfigure.exclude`)

**Fix**: Verify `annotationProcessor("com.flagzen:flagzen-core:1.1.0")` is in your build.gradle.

### Multiple FlagProvider Beans

**Symptom**: `NoUniqueBeanDefinitionException` when multiple `FlagProvider` beans are registered.

**Fix**: Annotate your primary `FlagProvider` bean with `@Primary`:

```java
@Configuration
public class FlagConfig {
    @Bean
    @Primary
    public FlagProvider primaryProvider() { ... }

    @Bean
    public FlagProvider secondaryProvider() { ... }
}
```

## Related

- `FeatureDispatcher` — resolves feature proxies
- `FlagProvider` SPI — pluggable flag source
- `flagzen-env` — environment variable provider
- `FlagZenExtension` — testing support
