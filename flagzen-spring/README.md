# flagzen-spring

Spring Boot auto-configuration that registers every `@Feature` proxy as a Spring bean.

Add this module to your classpath and all generated FlagZen proxies become injectable via `@Autowired`. No manual bean registration required.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

Requires Java 17+ and Spring Boot 3.x.

## Usage

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

The auto-configuration scans for `@Feature`-annotated interfaces, creates proxy beans via `FeatureProxyFactoryBean`, and registers a `FlagProvider` from the application context.

See [flagzen-examples](../flagzen-examples) for a complete Spring Boot example.

## API Overview

| Type | Description |
| --- | --- |
| `FlagZenAutoConfiguration` | Spring Boot auto-configuration entry point |
| `FeatureProxyRegistrar` | `BeanDefinitionRegistryPostProcessor` that registers proxy beans |
| `FeatureProxyFactoryBean` | `FactoryBean` that creates dispatcher-backed proxy instances |
| `InMemoryFlagProvider` | Simple in-memory `FlagProvider` for Spring integration tests |

## See Also

- [flagzen-core](../flagzen-core/README.md) -- core annotations and dispatcher
- [ADR-019: Proxy bean registration strategy](../docs/adrs/ADR-019-proxy-bean-registration-strategy.md)
- [Spring integration architecture](../docs/feature/flagzen-spring/design/architecture-design.md)
- [flagzen-examples](../flagzen-examples/README.md) -- runnable Spring examples
