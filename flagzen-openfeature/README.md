# flagzen-openfeature

[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-openfeature/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-openfeature)

`FlagProvider` adapter that delegates to the [OpenFeature](https://openfeature.dev/) SDK.

Bridges FlagZen's `FlagProvider` SPI to the OpenFeature `Client`, so any OpenFeature-compatible backend (LaunchDarkly, Flagd, Split, etc.) can serve FlagZen feature flags.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-openfeature:1.1.0")
    implementation("dev.openfeature:sdk:1.12.0") // or your preferred version
}
```

Requires Java 17+.

## Usage

```java
// Configure OpenFeature with your provider
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProviderAndWait(new MyOpenFeatureProvider());

// Bridge to FlagZen
Client client = api.getClient();
FlagProvider provider = new OpenFeatureFlagProvider(client);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

## API Overview

| Type | Description |
| --- | --- |
| `OpenFeatureFlagProvider` | `FlagProvider` that delegates to an OpenFeature `Client` |
| `EvaluationContextMapper` | Maps FlagZen `EvaluationContext` to OpenFeature `EvaluationContext` |

## See Also

- [flagzen-core](../flagzen-core/README.md) -- `FlagProvider` SPI contract
- [ADR-020: Absent flag detection strategy](../docs/adrs/ADR-020-absent-flag-detection-strategy.md)
- [OpenFeature adapter architecture](../docs/feature/flagzen-openfeature/design/architecture-design.md)
- [flagzen-examples](../flagzen-examples/README.md) -- runnable OpenFeature examples
