# Data Models -- flagzen-env

## FlagKeyParser (SAM Interface)

```
@FunctionalInterface
FlagKeyParser {
    Optional<List<String>> parse(String sourceKeyName)
}
```

- Input: a source system key name (e.g., `"FLAGZEN_CHECKOUT_FLOW"`, `"myAppCheckoutFlow"`)
- Output: `Optional<List<String>>` where:
  - `Optional.empty()` = this parser does not recognize/match the key name
  - `Optional.of(List.of("checkout", "flow"))` = parsed segments (always lowercase)
- The `List<String>` is ordered: segments preserve the positional meaning of the original key

### Built-in Parser Behaviors

| Factory Method | Input Example | Output |
| --- | --- | --- |
| `screamingSnakeCase("FLAGZEN_")` | `"FLAGZEN_CHECKOUT_FLOW"` | `Optional.of(["checkout", "flow"])` |
| `screamingSnakeCase("FLAGZEN_")` | `"HOME"` | `Optional.empty()` |
| `screamingSnakeCase("FLAGZEN_")` | `"FLAGZEN_DARKMODE"` | `Optional.of(["darkmode"])` |
| `screamingSnakeCase()` | `"CHECKOUT_FLOW"` | `Optional.of(["checkout", "flow"])` |
| `camelCase("myApp")` | `"myAppCheckoutFlow"` | `Optional.of(["checkout", "flow"])` |
| `camelCase("myApp")` | `"FLAGZEN_CHECKOUT_FLOW"` | `Optional.empty()` |
| `camelCase()` | `"checkoutFlow"` | `Optional.of(["checkout", "flow"])` |
| `snakeCase("flagzen_")` | `"flagzen_checkout_flow"` | `Optional.of(["checkout", "flow"])` |
| `snakeCase()` | `"checkout_flow"` | `Optional.of(["checkout", "flow"])` |

## FlagKeyFormat (SAM Interface)

```
@FunctionalInterface
FlagKeyFormat {
    String format(List<String> segments)
}
```

- Input: list of lowercase string segments (e.g., `["checkout", "flow"]`)
- Output: formatted flag key string

### Built-in Formatter Behaviors

| Factory Method | Input Segments | Output |
| --- | --- | --- |
| `kebabCase()` | `["checkout", "flow"]` | `"checkout-flow"` |
| `snakeCase()` | `["checkout", "flow"]` | `"checkout_flow"` |
| `camelCase()` | `["checkout", "flow"]` | `"checkoutFlow"` |
| `pascalCase()` | `["checkout", "flow"]` | `"CheckoutFlow"` |
| `dotCase()` | `["checkout", "flow"]` | `"checkout.flow"` |
| `colonCase()` | `["checkout", "flow"]` | `"checkout:flow"` |
| (any) | `["darkmode"]` | `"darkmode"` (single segment, no delimiter) |

Exception: `camelCase()` with single segment returns `"darkmode"`, `pascalCase()` with single segment returns `"Darkmode"`.

## ConflictStrategy (Enum)

```
enum ConflictStrategy {
    WARN,   // Log warning at construction + first access, keep last mapping
    ERROR   // Throw IllegalStateException at construction
}
```

## Immutable Flag Map

After construction, the provider holds:

| Field | Type | Mutability | Purpose |
| --- | --- | --- | --- |
| Flag map | `Map<String, String>` | Immutable (e.g., `Map.copyOf()`) | flagKey -> envVarValue. The result of the parse/format pipeline. |
| Conflicted keys | `Set<String>` | Immutable (e.g., `Set.copyOf()`) | Set of flag keys that had conflicts during construction (WARN strategy only). |
| Warned keys | (mutable concurrent set) | Thread-safe mutable | Tracks which conflicted keys have had their first-access warning logged. Only mutable state after construction. |

### Map Construction Example

Given environment:
- `FLAGZEN_CHECKOUT_FLOW=PREMIUM`
- `FLAGZEN_MAX_RETRIES=5`
- `HOME=/Users/kenji`
- `PATH=/usr/bin`

With default config (parser: `screamingSnakeCase("FLAGZEN_")`, formatter: `kebabCase()`):

| Env Var | Parser Result | Formatter Result | Map Entry |
| --- | --- | --- | --- |
| `FLAGZEN_CHECKOUT_FLOW` | `Optional.of(["checkout", "flow"])` | `"checkout-flow"` | `"checkout-flow" -> "PREMIUM"` |
| `FLAGZEN_MAX_RETRIES` | `Optional.of(["max", "retries"])` | `"max-retries"` | `"max-retries" -> "5"` |
| `HOME` | `Optional.empty()` | (skipped) | (excluded) |
| `PATH` | `Optional.empty()` | (skipped) | (excluded) |

Final immutable map: `{"checkout-flow": "PREMIUM", "max-retries": "5"}`

## Builder API

```
EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParser)                           // add a parser (accumulates)
    .formatter(FlagKeyFormat)                        // add a formatter (accumulates)
    .onConflict(ConflictStrategy)                    // override default strategy
    .environmentSource(Supplier<Map<String, String>>) // override System::getenv
    .build()                                         // execute pipeline, return provider
```

### Builder State

| Field | Type | Default |
| --- | --- | --- |
| parsers | `List<FlagKeyParser>` | empty (defaults to `[screamingSnakeCase("FLAGZEN_")]` at build time) |
| formatters | `List<FlagKeyFormat>` | empty (defaults to `[kebabCase()]` at build time) |
| conflictStrategy | `ConflictStrategy` (nullable) | null (computed from cardinality at build time) |
| environmentSource | `Supplier<Map<String, String>>` | `System::getenv` |

### Build-Time Validation

1. If parsers is empty, use default parser
2. If formatters is empty, use default formatter
3. If conflictStrategy is null, compute from cardinality rules
4. Call `environmentSource.get()` to obtain env var map
5. Run parse/format pipeline
6. Apply conflict strategy
7. Freeze map and conflicted-keys set
8. Return immutable provider

## ServiceLoader Contract

For ServiceLoader auto-discovery, the provider needs a public no-arg constructor. This constructor delegates to `create()`:

```
// Conceptual -- crafter implements
public EnvironmentVariableFlagProvider() {
    // delegates to create() which uses builder with all defaults
}
```

The ServiceLoader-discovered instance uses default configuration. Custom configuration requires explicit `builder().build()`.
