# flagzen-env

`FlagProvider` implementation that reads feature flag values from environment variables.

Eagerly loads and parses env vars at startup into an immutable map. Supports configurable key parsers, formatters, prefixes, and conflict resolution strategies for multi-convention codebases.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-env:1.1.0")
}
```

Requires Java 17+. Transitively depends on [flagzen-key-mapping](../flagzen-key-mapping/README.md).

## Usage

```java
// Default: reads FLAGZEN_* env vars, SCREAMING_SNAKE_CASE
FlagProvider provider = new EnvironmentVariableFlagProvider();

FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

Set `FLAGZEN_CHECKOUT_FLOW=STREAMLINED` in your environment and it just works.

See [flagzen-examples](../flagzen-examples) for configuration examples with custom parsers and formatters.

## API Overview

| Type | Description |
| --- | --- |
| `EnvironmentVariableFlagProvider` | `FlagProvider` that reads from `System.getenv()` at construction time |

The provider is configured via its builder, which accepts `FlagKeyParser`, `FlagKeyFormat`, and `ConflictStrategy` from [flagzen-key-mapping](../flagzen-key-mapping/README.md).

## See Also

- [flagzen-core](../flagzen-core/README.md) -- `FlagProvider` SPI contract
- [flagzen-key-mapping](../flagzen-key-mapping/README.md) -- key parsing/formatting dependency
- [ADR-015: Key mapping module split](../docs/adrs/ADR-015-key-mapping-module-split.md)
- [ADR-016: Eager loading strategy](../docs/adrs/ADR-016-eager-loading-strategy.md)
- [ADR-017: Conflict strategy design](../docs/adrs/ADR-017-conflict-strategy-design.md)
- [Env provider architecture](../docs/feature/flagzen-env/design/architecture-design.md)
- [flagzen-examples](../flagzen-examples/README.md) -- runnable env examples
