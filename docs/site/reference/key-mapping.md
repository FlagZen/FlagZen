# Key Mapping Reference

Parsing and formatting infrastructure for converting source system names into flag keys.

**Module**: `flagzen-key-mapping`

## Overview

Key mapping solves the problem of translating source names (like environment variable names) into canonical flag keys. It consists of:

- `FlagKeyParser` — parses a source name into segments
- `FlagKeyParsers` — factory methods for common parser implementations
- `FlagKeyFormat` — formats segments into a flag key string
- `FlagKeyFormats` — factory methods for common formatter implementations
- `ConflictStrategy` — handles conflicts when multiple source names map to the same flag key

## FlagKeyParser

Parses a source name into lowercase key segments.

### Interface

```java
@FunctionalInterface
public interface FlagKeyParser {
    Optional<List<String>> parse(String sourceName);
}
```

### Contract

- Takes a raw source name (e.g., an environment variable name)
- Returns lowercase segments if the name matches the expected pattern
- Returns `Optional.empty()` if the name does not match
- Each segment is a single lowercase word

### Built-In Parsers

#### `FlagKeyParsers.screamingSnakeCase(String prefix)`

Matches `SCREAMING_SNAKE_CASE` names with a given prefix.

**Signature**

```java
public static FlagKeyParser screamingSnakeCase(String prefix)
```

**Example**

```java
FlagKeyParser parser = FlagKeyParsers.screamingSnakeCase("FLAGZEN_");

parser.parse("FLAGZEN_CHECKOUT_FLOW");
// => Optional.of(["checkout", "flow"])

parser.parse("FLAGZEN_MAX_RETRIES");
// => Optional.of(["max", "retries"])

parser.parse("OTHER_PREFIX_VALUE");
// => Optional.empty() (prefix mismatch)
```

#### `FlagKeyParsers.screamingSnakeCase()`

Matches any `SCREAMING_SNAKE_CASE` name without a prefix.

**Signature**

```java
public static FlagKeyParser screamingSnakeCase()
```

**Example**

```java
FlagKeyParser parser = FlagKeyParsers.screamingSnakeCase();

parser.parse("CHECKOUT_FLOW");
// => Optional.of(["checkout", "flow"])

parser.parse("MAX_RETRIES");
// => Optional.of(["max", "retries"])

parser.parse("lowercase_value");
// => Optional.of(["lowercase", "value"]) (uppercasing is not validated)
```

#### `FlagKeyParsers.camelCase(String prefix)`

Matches camelCase names with a given prefix, splitting on uppercase boundaries.

**Signature**

```java
public static FlagKeyParser camelCase(String prefix)
```

**Example**

```java
FlagKeyParser parser = FlagKeyParsers.camelCase("myApp");

parser.parse("myAppCheckoutFlow");
// => Optional.of(["checkout", "flow"])

parser.parse("myAppMaxRetries");
// => Optional.of(["max", "retries"])

parser.parse("otherAppValue");
// => Optional.empty() (prefix mismatch)
```

#### `FlagKeyParsers.camelCase()`

Matches any camelCase name without a prefix.

**Signature**

```java
public static FlagKeyParser camelCase()
```

**Example**

```java
FlagKeyParser parser = FlagKeyParsers.camelCase();

parser.parse("checkoutFlow");
// => Optional.of(["checkout", "flow"])

parser.parse("maxRetries");
// => Optional.of(["max", "retries"])

parser.parse("x");
// => Optional.of(["x"]) (single segment)
```

## FlagKeyFormat

Formats lowercase key segments into a flag key string.

### Interface

```java
@FunctionalInterface
public interface FlagKeyFormat {
    String format(List<String> segments);
}
```

### Contract

- Takes a list of lowercase segments
- Returns a formatted flag key string
- All segments should be non-empty and lowercase

### Built-In Formatters

#### `FlagKeyFormats.kebabCase()`

Joins segments with hyphens.

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.kebabCase();

format.format(["checkout", "flow"]);
// => "checkout-flow"

format.format(["max", "retries"]);
// => "max-retries"
```

#### `FlagKeyFormats.snakeCase()`

Joins segments with underscores.

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.snakeCase();

format.format(["checkout", "flow"]);
// => "checkout_flow"

format.format(["max", "retries"]);
// => "max_retries"
```

#### `FlagKeyFormats.camelCase()`

Joins segments in camelCase (first segment as-is, subsequent segments capitalized).

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.camelCase();

format.format(["checkout", "flow"]);
// => "checkoutFlow"

format.format(["max", "retries"]);
// => "maxRetries"
```

#### `FlagKeyFormats.pascalCase()`

Joins segments in PascalCase (all segments capitalized).

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.pascalCase();

format.format(["checkout", "flow"]);
// => "CheckoutFlow"

format.format(["max", "retries"]);
// => "MaxRetries"
```

#### `FlagKeyFormats.dotCase()`

Joins segments with dots.

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.dotCase();

format.format(["checkout", "flow"]);
// => "checkout.flow"

format.format(["max", "retries"]);
// => "max.retries"
```

#### `FlagKeyFormats.colonCase()`

Joins segments with colons.

**Example**

```java
FlagKeyFormat format = FlagKeyFormats.colonCase();

format.format(["checkout", "flow"]);
// => "checkout:flow"

format.format(["max", "retries"]);
// => "max:retries"
```

## ConflictStrategy

Strategy for handling conflicts when multiple source names map to the same flag key.

### Enum

```java
public enum ConflictStrategy {
    WARN,
    ERROR
}
```

### Values

|  Value  |                                 Behavior                                 |
| ------- | ------------------------------------------------------------------------ |
| `WARN`  | Log a warning at construction and on first access, keep the last mapping |
| `ERROR` | Throw `IllegalStateException` at construction, provider is not created   |

### Cardinality-Based Defaults

The default conflict strategy depends on the number of parsers and formatters:

| Parsers | Formatters | Default |                      Reasoning                      |
| ------- | ---------- | ------- | --------------------------------------------------- |
| 1       | 1          | WARN    | Low collision risk (only env var name collision)    |
| N       | 1          | WARN    | Medium risk (different parsers may overlap)         |
| 1       | N          | WARN    | Medium risk (multiple formatters reduce collisions) |
| N       | N          | ERROR   | High risk (cartesian product maximizes collisions)  |

### Example

```java
// Low cardinality: WARN by default
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .format(FlagKeyFormats.kebabCase())
    .build();
// If FLAGZEN_X and other sources map to same key, logs warning

// High cardinality: ERROR by default
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .parser(FlagKeyParsers.screamingSnakeCase("FF_"))
    .format(FlagKeyFormats.kebabCase())
    .format(FlagKeyFormats.snakeCase())
    .build();
// Throws IllegalStateException if any conflict detected

// Override default
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .parser(FlagKeyParsers.screamingSnakeCase("FF_"))
    .format(FlagKeyFormats.kebabCase())
    .format(FlagKeyFormats.snakeCase())
    .onConflict(ConflictStrategy.WARN)
    .build();
// Logs warnings instead of throwing
```

## Parse/Format Pipeline Example

Combining parsers and formatters to create a full mapping:

```java
FlagKeyParser parser = FlagKeyParsers.screamingSnakeCase("FLAGZEN_");
FlagKeyFormat format = FlagKeyFormats.kebabCase();

// Environment variable: FLAGZEN_CHECKOUT_FLOW
// Step 1: Parse
Optional<List<String>> segments = parser.parse("FLAGZEN_CHECKOUT_FLOW");
// => Optional.of(["checkout", "flow"])

// Step 2: Format
String flagKey = segments.map(format::format).orElse(null);
// => "checkout-flow"

// Step 3: Resolve flag
FlagProvider provider = ...;
Optional<String> value = provider.getString("checkout-flow");
```

## Best Practices

- **Be explicit about prefix**: Use `screamingSnakeCase("PREFIX_")` rather than `screamingSnakeCase()` to avoid accidentally matching unrelated env vars.
- **Match your naming convention**: Choose parsers/formatters that match your application's naming style:
  - Env vars: `screamingSnakeCase("PREFIX_")` parser → `kebabCase()` format
  - Spring properties: `camelCase()` parser → `kebabCase()` format
  - Vault paths: custom parser → `colonCase()` format
- **Watch for conflicts**: If using multiple parsers/formatters, test with your actual env vars to catch conflicts. The `ERROR` default for high-cardinality configs is there to catch these early.
- **Document mapping**: In your configuration, document the parser/formatter combination so users understand the env var naming convention.

## Related

- `EnvironmentVariableFlagProvider` — uses key mapping to parse env var names
- `FlagProvider` — the flag resolution interface
