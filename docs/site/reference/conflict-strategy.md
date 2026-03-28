# Conflict Strategy Reference

Strategy for handling conflicts when multiple environment variables map to the same flag key.

**Module**: `flagzen-key-mapping`

## Overview

When a `FlagProvider` implementation (like `EnvironmentVariableFlagProvider`) uses multiple parsers or formatters, different source names can map to the same flag key. For example:

```
Parser 1 (SCREAMING_SNAKE_CASE with "FLAGZEN_" prefix):
  FLAGZEN_CHECKOUT_FLOW => checkout-flow (with kebab-case format)

Parser 2 (camelCase with "myApp" prefix):
  myAppCheckoutFlow => checkout-flow (with kebab-case format)
```

Both map to the same flag key `checkout-flow` but come from different sources. The conflict strategy determines what happens in this situation.

## Enum

```java
public enum ConflictStrategy {
    WARN,
    ERROR
}
```

## Values

### `WARN`

**Behavior**: Log a warning and continue. The last mapping wins.

- At provider construction time: log a warning with the conflicted flag key and both source names
- On first `getString()` call for the conflicted key: log another warning
- Do not repeat on subsequent calls for the same key
- The flag value from the last matching source is used

**Use Case**: Migration scenarios or low-cardinality configs where conflicts are rare and acceptable.

**Example**

```
[WARN] Conflict detected for flag key 'checkout-flow':
  - FLAGZEN_CHECKOUT_FLOW => checkout-flow
  - myAppCheckoutFlow => checkout-flow
The last mapping will be used. This is probably unintentional.
```

### `ERROR`

**Behavior**: Throw `IllegalStateException` at construction time. The provider is not created.

- All conflicts detected upfront during provider construction
- Fails fast, preventing silent data loss
- Exact error message lists both source names and the conflicted flag key

**Use Case**: High-cardinality configs where conflicts are unexpected and must be prevented.

**Example**

```
IllegalStateException: Conflict detected: multiple sources map to flag key 'checkout-flow':
  - FLAGZEN_CHECKOUT_FLOW
  - myAppCheckoutFlow
Use ConflictStrategy.WARN to override, or adjust parsers/formatters to eliminate conflicts.
```

## Cardinality-Based Defaults

The default `ConflictStrategy` is determined automatically based on parser/formatter cardinality:

| Parsers | Formatters | Default |                                     Reasoning                                      |
| ------- | ---------- | ------- | ---------------------------------------------------------------------------------- |
| 1       | 1          | WARN    | Low collision risk; conflicts only from env var naming collisions                  |
| 2+      | 1          | WARN    | Medium risk; different parsers may match overlapping env vars                      |
| 1       | 2+         | WARN    | Medium risk; multiple formatters reduce collision surface                          |
| 2+      | 2+         | ERROR   | High risk; cartesian product of (parsers × formatters) maximizes collision surface |

## Usage

### With EnvironmentVariableFlagProvider

Use the builder's `.onConflict()` method:

```java
// Use WARN strategy (low cardinality defaults to WARN anyway)
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .format(FlagKeyFormats.kebabCase())
    .onConflict(ConflictStrategy.WARN)
    .build();

// Override high-cardinality ERROR default with WARN
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .parser(FlagKeyParsers.screamingSnakeCase("FF_"))
    .format(FlagKeyFormats.kebabCase())
    .format(FlagKeyFormats.snakeCase())
    .onConflict(ConflictStrategy.WARN)  // Override default ERROR
    .build();
```

### Default Strategy Selection

If `.onConflict()` is not called, the cardinality defaults apply:

```java
// Cardinality 1x1: defaults to WARN
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .format(FlagKeyFormats.kebabCase())
    .build();
// Constructs successfully, logs warnings if conflicts found

// Cardinality 2x2: defaults to ERROR
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .parser(FlagKeyParsers.screamingSnakeCase("FF_"))
    .format(FlagKeyFormats.kebabCase())
    .format(FlagKeyFormats.snakeCase())
    .build();
// Throws IllegalStateException if any conflict found
```

## Warning Tracking

For `WARN` strategy, the provider tracks which flag keys have had warnings emitted:

- Construction warning: logged once at provider creation if conflicts detected
- First-access warning: logged on the first `getString()` call for a conflicted key
- Subsequent access: no warning logged (avoid log spam)

This dual-phase approach ensures visibility both at startup and at the point of first use.

## Migration Scenarios

When transitioning from one environment variable convention to another, you may have both conventions active simultaneously:

```
Old convention: FLAGZEN_CHECKOUT_FLOW=CLASSIC
New convention: FF_CHECKOUT_FLOW=PREMIUM

Both map to flag key 'checkout-flow' but have different values.
```

Use `WARN` strategy with intentional parser ordering (first or last wins semantics) to manage this:

```java
EnvironmentVariableFlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))  // Old convention
    .parser(FlagKeyParsers.screamingSnakeCase("FF_"))       // New convention
    .format(FlagKeyFormats.kebabCase())
    .onConflict(ConflictStrategy.WARN)
    .build();

// During transition, warnings alert you to conflicting values
// Once migration complete, remove old parser to eliminate conflicts
```

## Best Practices

- **Low cardinality configs**: Use WARN (default). Conflicts are rare. Warnings alert you to unexpected overlaps.
- **High cardinality configs**: Use ERROR (default). Conflicts are likely. Fail fast rather than silently choosing one value.
- **When overriding defaults**: Document why. For example, "We intentionally use multiple parsers during a migration; WARN allows both conventions simultaneously."
- **Test with real env vars**: Before deploying, test your parser/formatter combination with your actual environment variables. Simulate conflicts to verify the strategy works as expected.
- **Monitor logs**: With WARN strategy, monitor your startup logs for conflict warnings. They indicate configuration issues worth resolving.

## Related

- `FlagKeyParser` — parses source names into segments
- `FlagKeyFormat` — formats segments into flag keys
- `EnvironmentVariableFlagProvider` — uses conflict strategy to handle env var mapping
