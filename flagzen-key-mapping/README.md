# flagzen-key-mapping

[![Javadoc](https://javadoc.io/badge2/com.flagzen/flagzen-key-mapping/javadoc.svg)](https://javadoc.io/doc/com.flagzen/flagzen-key-mapping)

Reusable key format parsing and formatting for mapping flag keys between naming conventions.

This module provides the building blocks for converting flag keys like `checkout-flow` to environment variable names like `FLAGZEN_CHECKOUT_FLOW` (or any other convention). It is a dependency of [flagzen-env](../flagzen-env/README.md) and can be used independently for custom providers that need key translation.

## Installation

```gradle
dependencies {
    implementation("com.flagzen:flagzen-key-mapping:1.1.0")
}
```

Requires Java 17+.

## Usage

```java
FlagKeyParser parser = FlagKeyParsers.kebabCase();
FlagKeyFormat formatter = FlagKeyFormats.screamingSnakeCase("FLAGZEN");
String envKey = formatter.format(parser.parse("checkout-flow"));
// "FLAGZEN_CHECKOUT_FLOW"
```

## API Overview

| Type | Description |
| --- | --- |
| `FlagKeyParser` | Parses a flag key string into normalized segments |
| `FlagKeyParsers` | Factory for built-in parsers (kebab-case, camelCase, etc.) |
| `FlagKeyFormat` | Formats parsed segments into a target convention |
| `FlagKeyFormats` | Factory for built-in formatters (SCREAMING_SNAKE, etc.) |
| `ConflictStrategy` | Resolution strategy when multiple env vars map to the same flag key |

## See Also

- [flagzen-env](../flagzen-env/README.md) -- primary consumer of this module
- [ADR-015: Key mapping module split](../docs/adrs/ADR-015-key-mapping-module-split.md)
