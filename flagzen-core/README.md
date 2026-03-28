# flagzen-core

[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-core/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-core)

Compile-time annotation processor and runtime dispatch engine for FlagZen feature flags.

This is the foundation module. It provides the `@Feature` / `@Variant` annotations, the annotation processor that generates proxy classes, and the `FeatureDispatcher` that routes method calls to the active variant at runtime. Zero runtime reflection.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

Requires Java 17+.

## Usage

```java
@Feature("checkout-flow")
public interface CheckoutFlow {
    String execute();
}

@Variant(value = "STREAMLINED", of = CheckoutFlow.class)
public class StreamlinedCheckout implements CheckoutFlow {
    public String execute() { return "streamlined"; }
}

FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

See [flagzen-examples](../flagzen-examples) for complete runnable examples.

## API Overview

| Type | Description |
| --- | --- |
| `@Feature` | Marks an interface as a feature flag dispatch point |
| `@Variant` | Maps a flag value to an implementation class |
| `@WhenTrue` / `@WhenFalse` | Boolean convenience annotations |
| `@CloseTo` | Approximate matching for `DOUBLE`-typed features |
| `@DefaultVariant` | Fallback when no variant matches |
| `FeatureType` | Enum: `STRING`, `INT`, `LONG`, `BOOLEAN`, `DOUBLE` |
| `FeatureDispatcher` | Resolves a `@Feature` interface to its active variant |
| `FlagProvider` (SPI) | Pluggable flag value source (`spi.FlagProvider`) |
| `EvaluationContext` | Contextual metadata passed to providers |
| `FlagContext` | Thread-scoped evaluation context holder |

## See Also

- [flagzen-test](../flagzen-test/README.md) -- JUnit 5 testing support
- [flagzen-env](../flagzen-env/README.md) -- environment variable provider
- [flagzen-spring](../flagzen-spring/README.md) -- Spring Boot auto-configuration
- [flagzen-openfeature](../flagzen-openfeature/README.md) -- OpenFeature adapter
- [flagzen-examples](../flagzen-examples/README.md) -- runnable examples
- [Architecture design](../docs/feature/flagzen/design/architecture-design.md) -- M0 core architecture
- [Typed variants design](../docs/feature/flagzen-typed-variants/) -- INT, LONG, BOOLEAN, DOUBLE dispatch
- [Evaluation context design](../docs/feature/flagzen-eval-context/) -- context propagation
