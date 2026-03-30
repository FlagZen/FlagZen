# FlagZen

<!-- markdownlint-disable MD033 -->
<p align="center">
  <img src="branding/logo.png" alt="FlagZen" width="120">
</p>
<!-- markdownlint-enable MD033 -->

[![CI](https://github.com/FlagZen/FlagZen/actions/workflows/ci.yml/badge.svg)](https://github.com/FlagZen/FlagZen/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.flagzen/flagzen-core)](https://central.sonatype.com/namespace/com.flagzen)
[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-core/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-core)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE.txt)
[![Docs](https://img.shields.io/badge/docs-flagzen.com-blue)](https://flagzen.com)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/projects/jdk/17/)

Type-safe polymorphic dispatch layer for feature flags in Java 17+.

FlagZen sits between your code and your flag provider (LaunchDarkly, OpenFeature, Togglz, env vars). Define a `@Feature` interface, implement `@Variant` classes, and the generated proxy routes method calls to the active variant at runtime. No `if/else` chains, no string comparisons, no reflection.

## Quick Start

**1. Add the dependency**

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

**2. Define a feature**

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}
```

**3. Implement variants**

```java
@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "classic"; }
}

@Variant(value = "STREAMLINED", of = CheckoutFlow.class)
public class StreamlinedCheckout implements CheckoutFlow {
    @Override
    public String execute() { return "streamlined"; }
}
```

**4. Resolve at runtime**

```java
FlagProvider provider = new InMemoryFlagProvider();
provider.set("checkout-flow", "STREAMLINED");

FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
flow.execute(); // "streamlined"
```

The proxy re-evaluates the flag on every method call -- change the flag value, and the next call dispatches to the new variant. No restart needed.

## What FlagZen Does (and Doesn't)

FlagZen is a **dispatch layer**, not a flag management platform. It gives you type-safe, compile-time-verified feature dispatch on top of whatever flag source you already use.

| Responsibility | FlagZen | Your Flag Provider |
| --- | --- | --- |
| Type-safe feature interfaces | Yes | -- |
| Compile-time variant validation | Yes | -- |
| Polymorphic dispatch (no `if/else`) | Yes | -- |
| A/B testing and percentage rollouts | -- | Yes (LaunchDarkly, OpenFeature, etc.) |
| User targeting and segmentation | -- | Yes (via evaluation context passthrough) |
| Flag management UI / dashboard | -- | Yes |

FlagZen passes evaluation context (user ID, attributes) through to your provider, which decides what value to return. FlagZen then dispatches to the matching variant. The provider owns targeting logic; FlagZen owns dispatch logic.

## Modules

| Module | Description |
| --- | --- |
| `flagzen-core` | Annotations, annotation processor, proxy generation, `FeatureDispatcher`, `FlagProvider` SPI |
| `flagzen-test` | JUnit 5 extension: `@PinFlag`, `@FlagSource`, `TestFlagContext` |
| `flagzen-key-mapping` | Reusable key format parsing/formatting (used by `flagzen-env`) |
| `flagzen-env` | Environment variable `FlagProvider` with configurable key mapping |
| `flagzen-spring` | Spring Boot auto-configuration: `@Autowired` feature proxy injection |
| `flagzen-openfeature` | OpenFeature SDK adapter |
| `flagzen-launchdarkly` | LaunchDarkly Java Server SDK adapter |
| `flagzen-togglz` | Togglz feature toggle adapter |

## Module Documentation

Each module has its own README with installation, usage, and API details:

- [flagzen-core](flagzen-core/README.md) -- annotations, annotation processor, proxy generation, dispatcher
- [flagzen-test](flagzen-test/README.md) -- JUnit 5 extension for testing with pinned flags
- [flagzen-key-mapping](flagzen-key-mapping/README.md) -- key format parsing and formatting
- [flagzen-env](flagzen-env/README.md) -- environment variable flag provider
- [flagzen-spring](flagzen-spring/README.md) -- Spring Boot auto-configuration
- [flagzen-openfeature](flagzen-openfeature/README.md) -- OpenFeature SDK adapter
- [flagzen-launchdarkly](flagzen-launchdarkly/README.md) -- LaunchDarkly Java Server SDK adapter
- [flagzen-togglz](flagzen-togglz/README.md) -- Togglz feature toggle adapter
- [flagzen-examples](flagzen-examples/README.md) -- runnable examples (not published to Maven Central)

## Typed Dispatch

Flag values aren't limited to strings:

```java
@Feature(value = "max-retries", type = FeatureType.INT)
public interface RetryStrategy { int getMaxRetries(); }

@Variant(intValue = 3, of = RetryStrategy.class)
public class ConservativeRetry implements RetryStrategy { ... }

@Variant(intValue = 10, of = RetryStrategy.class)
public class AggressiveRetry implements RetryStrategy { ... }
```

Supported types: `STRING`, `INT`, `LONG`, `BOOLEAN`, `DOUBLE` (with `@CloseTo` for approximate matching).

## Multi-Value Variants

Map multiple flag values to the same implementation:

```java
@Variant(value = {"CLASSIC", "LEGACY"}, of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow { ... }
```

## Boolean Convenience Annotations

```java
@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
public interface DarkMode { String theme(); }

@WhenTrue(of = DarkMode.class)
public class DarkTheme implements DarkMode { ... }

@WhenFalse(of = DarkMode.class)
public class LightTheme implements DarkMode { ... }
```

## Environment Variables

```gradle
dependencies {
    implementation("com.flagzen:flagzen-env:1.1.0")
}
```

Set `FLAGZEN_CHECKOUT_FLOW=STREAMLINED` and it just works. The provider eagerly loads env vars at startup into an immutable map. Configurable parsers, formatters, and conflict strategies for multi-convention codebases.

## Spring Boot

```gradle
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
}
```

Add to classpath and every `@Feature` proxy becomes injectable:

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

## Testing

```gradle
dependencies {
    testImplementation("com.flagzen:flagzen-test:1.1.0")
}
```

```java
@ExtendWith(FlagZenExtension.class)
@PinFlag(key = "checkout-flow", value = "STREAMLINED")
class CheckoutTest {
    @Test
    void usesStreamlinedFlow(CheckoutFlow flow) {
        assertEquals("streamlined", flow.execute());
    }
}
```

## Requirements

- Java 17+
- Gradle or Maven (annotation processing must be configured)

## License

Apache License 2.0
