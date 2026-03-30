# flagzen-launchdarkly

[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-launchdarkly/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-launchdarkly)

`FlagProvider` adapter that delegates to the [LaunchDarkly](https://launchdarkly.com/) Java Server SDK.

All five flag types (string, boolean, int, double, long) are resolved natively through LaunchDarkly's typed variation methods -- no string round-tripping. Long values use `jsonValueVariationDetail` for full `long` range support.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-launchdarkly:1.2.0")
}
```

The LaunchDarkly Java Server SDK (7.x) is included transitively. Requires Java 17+.

## Usage

```java
LDClient ldClient = new LDClient("sdk-your-key");
FlagProvider provider = LaunchDarklyFlagProvider.create(ldClient);
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
```

### With evaluation context

```java
EvaluationContext context = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .build();

Optional<String> value = provider.getString("checkout-flow", context);
```

The adapter maps FlagZen `EvaluationContext` to a single-kind `LDContext`. When no context is provided, a shared anonymous `LDContext` is used.

## Absence Detection

The adapter uses LaunchDarkly's evaluation reason to detect absent flags (ADR-021):

| Reason kind | Result |
| --- | --- |
| `FALLTHROUGH`, `TARGET_MATCH`, `RULE_MATCH` | Value returned |
| `OFF` | Off-variation value returned (not empty) |
| `ERROR`, `PREREQUISITE_FAILED` | Empty |

## API Overview

| Type | Description |
| --- | --- |
| `LaunchDarklyFlagProvider` | `FlagProvider` that delegates to `LDClientInterface` with native typed resolution |
| `EvaluationContextMapper` | Maps FlagZen `EvaluationContext` to LaunchDarkly `LDContext` |

## See Also

- [flagzen-core](../flagzen-core/README.md) -- `FlagProvider` SPI contract
- [How-to: Connect to LaunchDarkly](../docs/site/how-to/launchdarkly.md)
- [ADR-021: LaunchDarkly absence and long handling](../docs/adrs/ADR-021-launchdarkly-absence-and-long-handling.md)
- [Architecture design](../docs/feature/flagzen-providers/design/architecture-design.md)
