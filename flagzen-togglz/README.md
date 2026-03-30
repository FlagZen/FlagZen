# flagzen-togglz

[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-togglz/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-togglz)

`FlagProvider` adapter for the [Togglz](https://www.togglz.org/) feature toggle library.

Boolean flags resolve natively via `FeatureState.isEnabled()`. String values use the activation strategy parameter `"value"` convention. Numeric types parse from the string value using `FlagProvider` defaults.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-togglz:1.2.0")
}
```

Togglz Core (4.x) is included transitively. Requires Java 17+.

## Usage

```java
FeatureManager featureManager = // your existing FeatureManager
FlagProvider provider = TogglzFlagProvider.create(featureManager);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

### String values via strategy parameter

Set a parameter named `"value"` on the feature state:

```java
FeatureState state = new FeatureState(MyFeatures.CHECKOUT_FLOW, true);
state.setParameter("value", "PREMIUM");
featureManager.setFeatureState(state);

provider.getString("CHECKOUT_FLOW"); // Optional.of("PREMIUM")
```

If no `"value"` parameter is set, `getString` returns the enabled state as `"true"` or `"false"`.

## Limitations

- **No explicit evaluation context**: Togglz uses thread-local `UserProvider` for targeting. FlagZen's `EvaluationContext` parameter is ignored (ADR-023). A one-time INFO log is emitted on first context-aware call.
- **Boolean-first**: Only `getBoolean` and `getString` have provider-specific implementations. Numeric types use default string parsing.
- **Case-insensitive lookup**: Feature keys are matched case-insensitively against `Feature.name()`.

## API Overview

| Type | Description |
| --- | --- |
| `TogglzFlagProvider` | `FlagProvider` that delegates to Togglz `FeatureManager` |
| `FeatureLookup` | Case-insensitive string-to-Feature cache (package-private) |

## See Also

- [flagzen-core](../flagzen-core/README.md) -- `FlagProvider` SPI contract
- [How-to: Connect to Togglz](../docs/site/how-to/togglz.md)
- [ADR-022: Togglz string value strategy](../docs/adrs/ADR-022-togglz-string-value-strategy.md)
- [ADR-023: Togglz context limitation](../docs/adrs/ADR-023-togglz-context-limitation.md)
- [Architecture design](../docs/feature/flagzen-providers/design/architecture-design.md)
