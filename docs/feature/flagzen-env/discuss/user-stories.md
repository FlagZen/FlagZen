<!-- markdownlint-disable MD024 -->

# User Stories -- flagzen-env: Environment Variable Provider

> **Module split**: The key-mapping infrastructure (FlagKeyParser, FlagKeyParsers, FlagKeyFormat, FlagKeyFormats, ConflictStrategy) lives in `flagzen-key-mapping` -- a reusable module for any provider. The `flagzen-env` module contains EnvironmentVariableFlagProvider, its builder, and ServiceLoader registration, and depends on `flagzen-key-mapping` transitively. Stories US-ENV-05, US-ENV-06, and US-ENV-09 target `flagzen-key-mapping`; all others target `flagzen-env`.

## US-ENV-01: Zero-Config Default (FLAGZEN\_ Prefix + Kebab Formatter)

### Problem

Kenji Tanaka is a backend developer deploying 12-factor apps to Kubernetes. He finds it tedious to wire up custom configuration every time he adds a library that reads from environment variables. He wants a provider that works out of the box: add the dependency, set `FLAGZEN_CHECKOUT_FLOW=PREMIUM` in his ConfigMap, and the flag key `"checkout-flow"` resolves to `"PREMIUM"` without writing any configuration code.

### Who

- Backend developer | Kubernetes deployment | Wants zero-config env var flag resolution

### Solution

`EnvironmentVariableFlagProvider.create()` returns a provider with sensible defaults: parser `FlagKeyParsers.screamingSnakeCase("FLAGZEN_")`, formatter `FlagKeyFormats.kebabCase()`, conflict strategy `WARN`. The provider eagerly reads all env vars at construction, parses matching ones into segments, formats segments into flag keys, and stores the result in an immutable map.

### Domain Examples

#### 1: Happy Path -- Kenji resolves a flag with default config

Kenji sets `FLAGZEN_CHECKOUT_FLOW=PREMIUM`. The default parser matches the `FLAGZEN_` prefix, strips it, and splits `CHECKOUT_FLOW` into segments `["checkout", "flow"]`. The default formatter joins them as `"checkout-flow"`. `provider.getString("checkout-flow")` returns `Optional.of("PREMIUM")`.

#### 2: Single-Segment Key -- Kenji has a one-word flag

Kenji sets `FLAGZEN_DARKMODE=on`. Parser strips prefix, produces segments `["darkmode"]`. Formatter produces `"darkmode"`. `provider.getString("darkmode")` returns `Optional.of("on")`.

#### 3: Non-Matching Env Var -- Kenji has unrelated env vars

Kenji has `HOME=/Users/kenji` and `PATH=/usr/bin` in his environment. These do not start with `FLAGZEN_`, so the default parser returns `Optional.empty()` for them. They are excluded from the flag map entirely.

#### 4: Missing Flag Key -- Kenji queries a flag that has no env var

No env var `FLAGZEN_UNKNOWN_FLAG` exists. The flag key `"unknown-flag"` has no entry in the immutable map. `provider.getString("unknown-flag")` returns `Optional.empty()`.

### UAT Scenarios (BDD)

#### Scenario: Default config resolves FLAGZEN_CHECKOUT_FLOW to checkout-flow

Given environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
When Kenji creates a provider with `EnvironmentVariableFlagProvider.create()`
And Kenji calls getString("checkout-flow")
Then the result is Optional.of("PREMIUM")

#### Scenario: Single-segment flag key resolves correctly

Given environment variable FLAGZEN_DARKMODE is set to "on"
When Kenji creates a provider with `EnvironmentVariableFlagProvider.create()`
And Kenji calls getString("darkmode")
Then the result is Optional.of("on")

#### Scenario: Non-matching env vars are excluded from the flag map

Given environment variable HOME is set to "/Users/kenji"
And environment variable FLAGZEN_DARK_MODE is set to "true"
When Kenji creates a provider with `EnvironmentVariableFlagProvider.create()`
Then getString("dark-mode") returns Optional.of("true")
And getString("home") returns Optional.empty()

#### Scenario: Missing flag key returns empty Optional

Given no environment variable starting with FLAGZEN_ matches "unknown-flag"
When Kenji calls getString("unknown-flag")
Then the result is Optional.empty()

### Acceptance Criteria

- [ ] `EnvironmentVariableFlagProvider.create()` returns a provider with default configuration
- [ ] Default parser: `FlagKeyParsers.screamingSnakeCase("FLAGZEN_")`
- [ ] Default formatter: `FlagKeyFormats.kebabCase()`
- [ ] Default conflict strategy: `WARN`
- [ ] `FLAGZEN_CHECKOUT_FLOW` maps to flag key `"checkout-flow"` with no user configuration
- [ ] Non-matching env vars are excluded from the flag map
- [ ] Missing flag keys return `Optional.empty()`

### Outcome KPIs

- **Who**: Backend developers adding flagzen-env
- **Does what**: Resolve flags from env vars with zero configuration code
- **By how much**: 0 lines of configuration for default FLAGZEN_ prefix convention
- **Measured by**: Integration test using `create()` with standard env vars
- **Baseline**: No env var provider exists; developers must build custom FlagProvider

### Technical Notes

- Default pipeline: read all env vars, filter by `FLAGZEN_` prefix, strip prefix, split on `_`, lowercase each segment, join with `-`
- Eager loading: all env vars read once at construction time via `System.getenv()`
- Immutable map: `getString(key)` is a pure `Map.get()` -- no `System.getenv()` at runtime
- Depends on US-ENV-02 (provider class), US-ENV-05 (parser), US-ENV-06 (formatter)

---

## US-ENV-02: Eager Loading with Immutable Map

### Problem

Kenji Tanaka needs flag resolution to be fast and predictable in his high-throughput payment service. He finds it concerning when libraries call `System.getenv()` on every flag read because it adds unpredictable latency and makes behavior harder to reason about. He wants the provider to load all env vars once at construction and serve lookups from an immutable in-memory map.

### Who

- Backend developer | High-throughput service | Wants fast, predictable flag resolution

### Solution

`EnvironmentVariableFlagProvider` eagerly reads all env vars via `System.getenv()` at construction time, filters them through registered parsers, formats segments into flag keys, and stores the resulting `flagKey -> envVarValue` pairs in an immutable map. `getString(key)` is a pure `Map.get()` call. No `System.getenv()` at runtime.

### Domain Examples

#### 1: Happy Path -- Kenji gets consistent reads

Kenji sets `FLAGZEN_MAX_RETRIES=5` before constructing the provider. After construction, `provider.getString("max-retries")` returns `Optional.of("5")` on every call. Even if the process's env var is somehow changed after construction, the provider returns the original value.

#### 2: Typed Resolution -- Kenji resolves an integer flag

Kenji sets `FLAGZEN_MAX_RETRIES=5`. He calls `provider.getInt("max-retries")` and receives `OptionalInt.of(5)`. Typed parsing is handled by `FlagProvider`'s default method which parses the string `"5"`.

#### 3: Empty Value -- Kenji sets an env var to empty string

Kenji sets `FLAGZEN_DARK_MODE=` (empty string). The parser matches and produces segments, the formatter produces the flag key `"dark-mode"`, and the value is empty string. `provider.getString("dark-mode")` returns `Optional.of("")`.

#### 4: Unparseable Typed Value -- Kenji has a non-numeric value

Kenji sets `FLAGZEN_MAX_RETRIES=lots`. `provider.getString("max-retries")` returns `Optional.of("lots")`. `provider.getInt("max-retries")` returns `OptionalInt.empty()` because `FlagProvider`'s default parsing catches the `NumberFormatException`.

### UAT Scenarios (BDD)

#### Scenario: Provider loads env vars eagerly at construction

Given environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
When Kenji constructs an EnvironmentVariableFlagProvider
Then getString("checkout-flow") returns Optional.of("PREMIUM")
And no System.getenv() call occurs after construction

#### Scenario: getString is a pure map lookup

Given environment variable FLAGZEN_MAX_RETRIES is set to "5"
And the provider has been constructed
When Kenji calls getString("max-retries") 1000 times
Then every call returns Optional.of("5")
And the result is identical on every call

#### Scenario: Typed resolution delegates to FlagProvider defaults

Given environment variable FLAGZEN_MAX_RETRIES is set to "5"
When Kenji calls getInt("max-retries")
Then the result is OptionalInt.of(5)

#### Scenario: Empty env var value is preserved as empty string

Given environment variable FLAGZEN_DARK_MODE is set to ""
When Kenji calls getString("dark-mode")
Then the result is Optional.of("")

#### Scenario: Unparseable typed value returns empty for typed access

Given environment variable FLAGZEN_MAX_RETRIES is set to "lots"
When Kenji calls getInt("max-retries")
Then the integer result is OptionalInt.empty()
But getString("max-retries") returns Optional.of("lots")

### Acceptance Criteria

- [ ] `EnvironmentVariableFlagProvider` implements `FlagProvider`
- [ ] Constructor reads all env vars via `System.getenv()` once
- [ ] Parsed flag keys and values stored in an immutable map
- [ ] `getString(key)` is a pure `Map.get()` -- no `System.getenv()` at runtime
- [ ] Empty env var values are preserved as `Optional.of("")`
- [ ] Typed methods delegate to `FlagProvider` default implementations
- [ ] Provider is thread-safe (immutable internal state)
- [ ] Context-aware overloads delegate to context-free methods (env vars are static)

### Outcome KPIs

- **Who**: Backend developers in high-throughput services
- **Does what**: Resolve flags with O(1) map lookup, no runtime I/O
- **By how much**: Zero `System.getenv()` calls after construction
- **Measured by**: Unit test verifying no env access post-construction; benchmark test
- **Baseline**: Naive implementation would call `System.getenv()` on every `getString()`

### Technical Notes

- `System.getenv()` (no-arg) returns the entire environment as an unmodifiable map -- one call suffices
- Immutable map means no synchronization needed -- inherently thread-safe
- Class lives in `com.flagzen.env` package
- Null key behavior follows `FlagProvider` contract
- Depends on US-ENV-05 (parsers) and US-ENV-06 (formatters) for the construction pipeline

---

## US-ENV-03: ServiceLoader Registration with Default Config

### Problem

Kenji Tanaka wants the environment variable provider to work automatically when added to the classpath. He finds it annoying when libraries require boilerplate registration code just to activate a feature. He wants "add dependency, done" -- ServiceLoader should discover the provider without any configuration.

### Who

- Backend developer | Adding a new FlagProvider | Wants zero-config activation

### Solution

Register `EnvironmentVariableFlagProvider` in `META-INF/services/com.flagzen.spi.FlagProvider` so that `ServiceLoader.load(FlagProvider.class)` discovers it automatically. The ServiceLoader-discovered instance uses `EnvironmentVariableFlagProvider.create()` (default config).

### Domain Examples

#### 1: Happy Path -- Kenji adds the dependency and it just works

Kenji adds `com.flagzen:flagzen-env:1.1.0` to his build.gradle. He calls `FlagZen.create()`. The factory uses `ServiceLoader` to discover `EnvironmentVariableFlagProvider`. Kenji never writes a single line of provider registration code.

#### 2: Explicit Override -- Kenji prefers explicit wiring

Kenji wants custom configuration (different prefix). He creates the provider explicitly with `EnvironmentVariableFlagProvider.builder().parser(...).formatter(...).build()` and passes it to `FlagZen.withProvider(envProvider)`. ServiceLoader is not used in this path.

#### 3: Multiple Providers on Classpath -- Kenji has both env and InMemory

Kenji has both `flagzen-env` and `InMemoryFlagProvider` on the classpath. `ServiceLoader` discovers both. The `FlagZen.create()` factory handles multiple providers according to its existing priority/chaining logic (defined in flagzen-core, outside this story's scope).

### UAT Scenarios (BDD)

#### Scenario: ServiceLoader discovers EnvironmentVariableFlagProvider

Given the flagzen-env module JAR is on the classpath
And the JAR contains META-INF/services/com.flagzen.spi.FlagProvider
When ServiceLoader.load(FlagProvider.class) is invoked
Then EnvironmentVariableFlagProvider is among the discovered providers

#### Scenario: ServiceLoader file contains correct fully-qualified class name

Given the file META-INF/services/com.flagzen.spi.FlagProvider exists in flagzen-env
When its contents are read
Then it contains "com.flagzen.env.EnvironmentVariableFlagProvider"

#### Scenario: Provider works without explicit registration

Given Kenji has flagzen-env on the classpath
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "STREAMLINED"
When Kenji resolves the "checkout-flow" flag through the auto-discovered provider
Then the value "STREAMLINED" is returned

### Acceptance Criteria

- [ ] `META-INF/services/com.flagzen.spi.FlagProvider` exists in flagzen-env resources
- [ ] File contains `com.flagzen.env.EnvironmentVariableFlagProvider`
- [ ] `EnvironmentVariableFlagProvider` has a public no-arg constructor (required for ServiceLoader)
- [ ] No-arg constructor uses default configuration (`create()` equivalent)
- [ ] Provider is discoverable via `ServiceLoader.load(FlagProvider.class)`
- [ ] No additional registration or configuration required by the user

### Outcome KPIs

- **Who**: Developers adding flagzen-env to their project
- **Does what**: Get a working env var provider with zero configuration code
- **By how much**: 0 lines of registration boilerplate required
- **Measured by**: Integration test that loads provider via ServiceLoader without explicit setup
- **Baseline**: InMemoryFlagProvider requires explicit construction

### Technical Notes

- ServiceLoader requires: public class + public no-arg constructor + META-INF/services file
- The services file is a plain text file, one FQCN per line
- Depends on US-ENV-02 (provider class must exist)
- If `FlagZen.create()` does not yet support multi-provider discovery, this story only guarantees ServiceLoader registration -- the factory integration is flagzen-core's responsibility
- The no-arg constructor creates a provider with default config (FLAGZEN_ prefix parser + kebab formatter)

---

## US-ENV-04: Custom Parser Configuration

### Problem

Mei-Lin Chen is a platform engineer maintaining a shared Kubernetes cluster where multiple teams deploy services using FlagZen. She finds the default `FLAGZEN_` prefix insufficient because her team's convention requires a team-specific prefix (`FF_`). She also needs to support a legacy system that uses camelCase env var names. Without custom parser configuration, she would have to fork the provider or write a custom one.

### Who

- Platform engineer | Shared Kubernetes cluster | Wants custom env var parsing to match team conventions

### Solution

The builder accepts `.parser(FlagKeyParser)` to configure how env var names are recognized and decomposed into segments. Developers can use built-in parsers with custom prefixes, or pass a lambda for fully custom parsing. Prefix is per-parser, not a global setting.

### Domain Examples

#### 1: Happy Path -- Mei-Lin uses a team-specific prefix

Mei-Lin creates a provider with `FlagKeyParsers.screamingSnakeCase("FF_")`. She sets `FF_CHECKOUT_FLOW=PREMIUM`. The parser matches the `FF_` prefix, strips it, and produces segments `["checkout", "flow"]`. With the default kebab formatter, `provider.getString("checkout-flow")` returns `Optional.of("PREMIUM")`.

#### 2: No Prefix -- Kenji uses raw SCREAMING_SNAKE env vars

Kenji's ops team has existing env vars like `CHECKOUT_FLOW=PREMIUM` without any library prefix. He creates a provider with `FlagKeyParsers.screamingSnakeCase()` (no prefix). The parser matches all SCREAMING_SNAKE env vars. `provider.getString("checkout-flow")` returns `Optional.of("PREMIUM")`.

#### 3: Custom Lambda Parser -- Kenji has a project-specific convention

Kenji's legacy system uses env vars like `FEAT_CHECKOUT_FLOW=PREMIUM` with a non-standard naming pattern. He passes a lambda parser:

```java
name -> name.startsWith("FEAT_")
    ? Optional.of(Arrays.asList(name.substring(5).toLowerCase().split("_")))
    : Optional.empty()
```

`provider.getString("checkout-flow")` returns `Optional.of("PREMIUM")`.

### UAT Scenarios (BDD)

#### Scenario: Custom prefix parser matches team-specific env vars

Given Mei-Lin configures parser FlagKeyParsers.screamingSnakeCase("FF_")
And environment variable FF_CHECKOUT_FLOW is set to "PREMIUM"
When the provider is constructed and Mei-Lin calls getString("checkout-flow")
Then the result is Optional.of("PREMIUM")

#### Scenario: No-prefix parser matches all SCREAMING_SNAKE env vars

Given Kenji configures parser FlagKeyParsers.screamingSnakeCase() with no prefix
And environment variable CHECKOUT_FLOW is set to "BASIC"
When the provider is constructed and Kenji calls getString("checkout-flow")
Then the result is Optional.of("BASIC")

#### Scenario: Custom lambda parser matches project-specific convention

Given Kenji configures a custom lambda parser for "FEAT_" prefixed env vars
And environment variable FEAT_CHECKOUT_FLOW is set to "BETA"
When the provider is constructed and Kenji calls getString("checkout-flow")
Then the result is Optional.of("BETA")

#### Scenario: Parser that does not match an env var excludes it

Given Mei-Lin configures parser FlagKeyParsers.screamingSnakeCase("FF_")
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
When the provider is constructed
Then getString("checkout-flow") returns Optional.empty()

### Acceptance Criteria

- [ ] Builder accepts `.parser(FlagKeyParser)` for custom parser configuration
- [ ] `FlagKeyParser` is a `@FunctionalInterface` with `Optional<List<String>> parse(String envVarName)`
- [ ] Prefix is per-parser, not a global builder setting
- [ ] Lambda expressions accepted wherever `FlagKeyParser` is expected
- [ ] Non-matching env vars are excluded from the flag map

### Outcome KPIs

- **Who**: Platform engineers with team-specific naming conventions
- **Does what**: Use custom env var prefixes and parsing logic without forking
- **By how much**: Any parsing strategy supported via lambda
- **Measured by**: Parameterized tests with different parser configurations
- **Baseline**: FLAGZEN_ prefix is hardcoded, no customization possible

### Technical Notes

- `FlagKeyParser` returns `Optional<List<String>>` -- empty means "does not match this env var"
- Prefix is part of the parser, not the provider -- each parser handles its own prefix matching
- Configuration is programmatic only (builder) -- no self-configuration via env vars
- Depends on US-ENV-02 (provider class) and US-ENV-05 (built-in parsers)

---

## US-ENV-05: Built-in Parsers (screamingSnakeCase, camelCase)

### Problem

Kenji Tanaka wants to use built-in parsers without writing parsing logic from scratch. The two most common env var conventions are `SCREAMING_SNAKE_CASE` (standard Unix) and `camelCase` (common in JVM ecosystems). He wants companion factory methods that handle prefix matching and segment extraction for these conventions.

### Who

- Backend developer | Standard env var conventions | Wants ready-made parsers

### Solution

`FlagKeyParsers` companion class with static factory methods:

- `screamingSnakeCase("FLAGZEN_")` -- matches `FLAGZEN_FOO_BAR`, strips prefix, splits on `_`, lowercases -> `["foo", "bar"]`
- `screamingSnakeCase()` -- same without prefix, matches all SCREAMING_SNAKE vars
- `camelCase("myApp")` -- matches `myAppFooBar`, strips prefix, splits on uppercase boundaries -> `["foo", "bar"]`
- `camelCase()` -- same without prefix, splits on uppercase boundaries

### Domain Examples

#### 1: screamingSnakeCase with prefix -- Kenji's standard FLAGZEN vars

`FlagKeyParsers.screamingSnakeCase("FLAGZEN_")` receives `"FLAGZEN_CHECKOUT_FLOW"`. Prefix matches. Strips prefix to get `"CHECKOUT_FLOW"`. Splits on `_` to get `["CHECKOUT", "FLOW"]`. Lowercases to `["checkout", "flow"]`. Returns `Optional.of(List.of("checkout", "flow"))`.

#### 2: screamingSnakeCase without prefix -- Kenji's raw env vars

`FlagKeyParsers.screamingSnakeCase()` receives `"CHECKOUT_FLOW"`. No prefix to strip. Splits on `_` and lowercases to `["checkout", "flow"]`. Returns `Optional.of(List.of("checkout", "flow"))`.

#### 3: camelCase with prefix -- Mei-Lin's Spring app

`FlagKeyParsers.camelCase("myApp")` receives `"myAppCheckoutFlow"`. Prefix `"myApp"` matches. Strips prefix to get `"CheckoutFlow"`. Splits on uppercase boundaries to `["Checkout", "Flow"]`. Lowercases to `["checkout", "flow"]`. Returns `Optional.of(List.of("checkout", "flow"))`.

#### 4: camelCase without prefix -- bare camelCase env vars

`FlagKeyParsers.camelCase()` receives `"checkoutFlow"`. Splits on uppercase boundaries to `["checkout", "Flow"]`. Lowercases to `["checkout", "flow"]`. Returns `Optional.of(List.of("checkout", "flow"))`.

#### 5: Non-matching prefix -- parser returns empty

`FlagKeyParsers.screamingSnakeCase("FLAGZEN_")` receives `"HOME"`. Prefix `"FLAGZEN_"` does not match. Returns `Optional.empty()`.

### UAT Scenarios (BDD)

#### Scenario: screamingSnakeCase with prefix parses matching env var

Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
When the parser receives "FLAGZEN_CHECKOUT_FLOW"
Then it returns segments ["checkout", "flow"]

#### Scenario: screamingSnakeCase with prefix rejects non-matching env var

Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
When the parser receives "HOME"
Then it returns empty

#### Scenario: screamingSnakeCase without prefix parses any SCREAMING_SNAKE var

Given parser FlagKeyParsers.screamingSnakeCase() with no prefix
When the parser receives "CHECKOUT_FLOW"
Then it returns segments ["checkout", "flow"]

#### Scenario: camelCase with prefix parses matching env var

Given parser FlagKeyParsers.camelCase("myApp")
When the parser receives "myAppCheckoutFlow"
Then it returns segments ["checkout", "flow"]

#### Scenario: camelCase without prefix parses bare camelCase var

Given parser FlagKeyParsers.camelCase() with no prefix
When the parser receives "checkoutFlow"
Then it returns segments ["checkout", "flow"]

#### Scenario: Single-segment env var produces single segment

Given parser FlagKeyParsers.screamingSnakeCase("FLAGZEN_")
When the parser receives "FLAGZEN_DARKMODE"
Then it returns segments ["darkmode"]

### Acceptance Criteria

- [ ] `FlagKeyParsers.screamingSnakeCase(String prefix)` matches prefix, strips, splits on `_`, lowercases
- [ ] `FlagKeyParsers.screamingSnakeCase()` matches all, splits on `_`, lowercases
- [ ] `FlagKeyParsers.camelCase(String prefix)` matches prefix, strips, splits on uppercase boundaries, lowercases
- [ ] `FlagKeyParsers.camelCase()` splits on uppercase boundaries, lowercases
- [ ] Non-matching env vars return `Optional.empty()`
- [ ] All parsers produce lowercase segments

### Outcome KPIs

- **Who**: Developers with standard env var naming conventions
- **Does what**: Use built-in parsers without custom parsing code
- **By how much**: 2 parser types (SCREAMING_SNAKE, camelCase) with optional prefix cover most conventions
- **Measured by**: Parameterized tests for each parser variant
- **Baseline**: No built-in parsers exist

### Technical Notes

- `FlagKeyParsers` is a companion class with static factory methods (not on the interface)
- All parsers produce lowercase segments to normalize input for formatters
- Prefix matching is case-sensitive (exact string match)
- Empty prefix string behaves the same as the no-prefix overload
- Lives in `com.flagzen.keymapping` package (flagzen-key-mapping module -- reusable across providers)

---

## US-ENV-06: Built-in Formatters (kebabCase, snakeCase, camelCase, pascalCase, dotCase, colonCase)

### Problem

Kenji Tanaka works on a project where flag keys use different conventions across subsystems: `kebab-case` for the frontend team, `snake_case` for the data team, `camelCase` for the Spring config. He wants built-in formatters that join segments into the right flag key format without writing custom formatting logic.

### Who

- Backend developer | Multi-convention codebase | Wants ready-made flag key formatters

### Solution

`FlagKeyFormats` companion class with static factory methods for 6 common conventions. Each takes a `List<String>` of segments and joins them into a flag key string.

### Domain Examples

#### 1: kebabCase -- Kenji's frontend flag keys

`FlagKeyFormats.kebabCase()` receives `["checkout", "flow"]` and produces `"checkout-flow"`.

#### 2: snakeCase -- Kenji's data team convention

`FlagKeyFormats.snakeCase()` receives `["checkout", "flow"]` and produces `"checkout_flow"`.

#### 3: camelCase -- Kenji's Spring config keys

`FlagKeyFormats.camelCase()` receives `["checkout", "flow"]` and produces `"checkoutFlow"`.

#### 4: pascalCase -- Mei-Lin's .NET service

`FlagKeyFormats.pascalCase()` receives `["checkout", "flow"]` and produces `"CheckoutFlow"`.

#### 5: dotCase -- property-style keys

`FlagKeyFormats.dotCase()` receives `["checkout", "flow"]` and produces `"checkout.flow"`.

#### 6: colonCase -- Redis-style namespaced keys

`FlagKeyFormats.colonCase()` receives `["checkout", "flow"]` and produces `"checkout:flow"`.

### UAT Scenarios (BDD)

#### Scenario: kebabCase formatter joins segments with hyphens

Given formatter FlagKeyFormats.kebabCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "checkout-flow"

#### Scenario: snakeCase formatter joins segments with underscores

Given formatter FlagKeyFormats.snakeCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "checkout_flow"

#### Scenario: camelCase formatter capitalizes subsequent segments

Given formatter FlagKeyFormats.camelCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "checkoutFlow"

#### Scenario: pascalCase formatter capitalizes all segments

Given formatter FlagKeyFormats.pascalCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "CheckoutFlow"

#### Scenario: dotCase formatter joins segments with dots

Given formatter FlagKeyFormats.dotCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "checkout.flow"

#### Scenario: colonCase formatter joins segments with colons

Given formatter FlagKeyFormats.colonCase()
When it formats segments ["checkout", "flow"]
Then the flag key is "checkout:flow"

#### Scenario: Single segment formatted without delimiter

Given formatter FlagKeyFormats.kebabCase()
When it formats segments ["darkmode"]
Then the flag key is "darkmode"

### Acceptance Criteria

- [ ] `FlagKeyFormats.kebabCase()` joins with `-`
- [ ] `FlagKeyFormats.snakeCase()` joins with `_`
- [ ] `FlagKeyFormats.camelCase()` first segment lowercase, subsequent capitalized, no delimiter
- [ ] `FlagKeyFormats.pascalCase()` all segments capitalized, no delimiter
- [ ] `FlagKeyFormats.dotCase()` joins with `.`
- [ ] `FlagKeyFormats.colonCase()` joins with `:`
- [ ] Single-segment input produces that segment without delimiter
- [ ] `FlagKeyFormat` is a `@FunctionalInterface` -- lambdas accepted

### Outcome KPIs

- **Who**: Developers with varying flag key conventions
- **Does what**: Use built-in formatters without custom formatting code
- **By how much**: 6 built-in formatters + lambda support cover all common conventions
- **Measured by**: Parameterized tests for each formatter
- **Baseline**: No built-in formatters exist

### Technical Notes

- `FlagKeyFormats` is a companion class with static factory methods
- `FlagKeyFormat` SAM interface: `String format(List<String> segments)`
- Segments are assumed lowercase (produced by parsers in US-ENV-05)
- Custom formatters via lambda: `segments -> String.join("/", segments)`
- Lives in `com.flagzen.keymapping` package (flagzen-key-mapping module -- reusable across providers)

---

## US-ENV-07: Multiple Parsers (Legacy Migration)

### Problem

Mei-Lin Chen manages a service that receives env vars from two systems: the new platform uses `FLAGZEN_CHECKOUT_FLOW` (SCREAMING_SNAKE with prefix), while the legacy system sets `myAppCheckoutFlow` (camelCase with prefix). She needs a single provider that recognizes both conventions and maps them to the same flag key format.

### Who

- Platform engineer | Legacy migration | Wants one provider for multiple env var conventions

### Solution

The builder accepts multiple `.parser()` calls. Each registered parser is tried against each env var. When multiple parsers match the same env var, each match produces a separate entry. When multiple env vars map to the same flag key (conflict), the `ConflictStrategy` governs behavior.

### Domain Examples

#### 1: Happy Path -- Both systems contribute different flags

Mei-Lin registers two parsers: `screamingSnakeCase("FLAGZEN_")` and `camelCase("myApp")`. Env var `FLAGZEN_CHECKOUT_FLOW=PREMIUM` matches the first parser -> flag key `"checkout-flow"`. Env var `myAppMaxRetries=5` matches the second parser -> flag key `"max-retries"`. Both resolve correctly.

#### 2: Same Flag From Two Sources -- Conflict detected

Mei-Lin has both `FLAGZEN_CHECKOUT_FLOW=PREMIUM` and `myAppCheckoutFlow=BASIC`. Both parsers produce segments `["checkout", "flow"]`. The formatter produces flag key `"checkout-flow"` twice with different values. Default conflict strategy `WARN` logs a warning, last mapping wins.

#### 3: No Overlap -- Clean migration

During migration, the legacy system only has `myAppMaxRetries=5` and the new system only has `FLAGZEN_CHECKOUT_FLOW=PREMIUM`. No conflicts. Both flags resolve without warnings.

### UAT Scenarios (BDD)

#### Scenario: Multiple parsers contribute different flags

Given the provider is configured with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
And environment variable myAppMaxRetries is set to "5"
When the provider is constructed
Then getString("checkout-flow") returns Optional.of("PREMIUM")
And getString("max-retries") returns Optional.of("5")

#### Scenario: Conflict from multiple parsers triggers warning

Given the provider is configured with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
And environment variable myAppCheckoutFlow is set to "BASIC"
When the provider is constructed with default conflict strategy WARN
Then a warning is logged mentioning both env var names
And getString("checkout-flow") returns a value (last mapping wins)

#### Scenario: Multiple parsers with no overlap produce no conflict

Given the provider is configured with parsers screamingSnakeCase("FLAGZEN_") and camelCase("myApp")
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
And no camelCase env var maps to "checkout-flow"
When the provider is constructed
Then no conflict warning is logged

### Acceptance Criteria

- [ ] Builder accepts multiple `.parser()` calls
- [ ] Each parser is tried against each env var independently
- [ ] When parsers produce the same flag key from different env vars, ConflictStrategy applies
- [ ] Default conflict strategy for multiple parsers + single formatter is `WARN`
- [ ] Warning includes both env var names and the conflicting flag key

### Outcome KPIs

- **Who**: Platform engineers managing legacy migrations
- **Does what**: Resolve flags from multiple env var conventions with one provider
- **By how much**: Eliminates need for multiple provider instances during migration
- **Measured by**: Integration test with two parsers and mixed env vars
- **Baseline**: Must create multiple providers or custom parser

### Technical Notes

- Multiple parsers are tried in registration order
- All matching parsers contribute entries (not just first match)
- Conflict detection happens during construction, not at query time
- Depends on US-ENV-04 (parser configuration), US-ENV-09 (ConflictStrategy)

---

## US-ENV-08: Multiple Formatters (Multi-Convention Codebase)

### Problem

Kenji Tanaka's codebase has flag keys in two different formats: the newer subsystem uses `kebab-case` keys while the legacy data pipeline uses `snake_case` keys. The same env var `FLAGZEN_CHECKOUT_FLOW` should produce entries for both `"checkout-flow"` and `"checkout_flow"` so that code using either convention can resolve the flag.

### Who

- Backend developer | Multi-convention codebase | Wants one env var to produce flag keys in multiple formats

### Solution

The builder accepts multiple `.formatter()` calls. Each parsed set of segments is formatted by every registered formatter, producing multiple flag key entries from a single env var.

### Domain Examples

#### 1: Happy Path -- One env var produces two flag keys

Kenji registers two formatters: `kebabCase()` and `snakeCase()`. Env var `FLAGZEN_CHECKOUT_FLOW=PREMIUM` is parsed to segments `["checkout", "flow"]`. `kebabCase()` produces `"checkout-flow"`. `snakeCase()` produces `"checkout_flow"`. Both `getString("checkout-flow")` and `getString("checkout_flow")` return `Optional.of("PREMIUM")`.

#### 2: Single-Segment Key -- No conflict possible

Env var `FLAGZEN_DARKMODE=on` produces segments `["darkmode"]`. Both formatters produce the same key `"darkmode"`. No conflict -- same value maps to same key.

#### 3: Formatter Collision -- Two formatters produce the same key from different env vars (unlikely but possible)

This scenario is rare with single parser + multiple formatters, but if two env vars produce the same segments, multiple formatters multiply the collision risk. ConflictStrategy applies.

### UAT Scenarios (BDD)

#### Scenario: Multiple formatters produce multiple flag keys from one env var

Given the provider is configured with formatters kebabCase() and snakeCase()
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
When the provider is constructed
Then getString("checkout-flow") returns Optional.of("PREMIUM")
And getString("checkout_flow") returns Optional.of("PREMIUM")

#### Scenario: Single-segment key resolves identically from both formatters

Given the provider is configured with formatters kebabCase() and snakeCase()
And environment variable FLAGZEN_DARKMODE is set to "on"
When the provider is constructed
Then getString("darkmode") returns Optional.of("on")

#### Scenario: Default conflict strategy for single parser + multiple formatters is WARN

Given the provider is configured with one parser and formatters kebabCase() and snakeCase()
When the provider is constructed
Then the default conflict strategy is WARN

### Acceptance Criteria

- [ ] Builder accepts multiple `.formatter()` calls
- [ ] Each set of parsed segments is formatted by every registered formatter
- [ ] Multiple formatters can produce the same flag key from different segments -- ConflictStrategy applies
- [ ] Default conflict strategy for single parser + multiple formatters is `WARN`
- [ ] Same value mapping to same key via different formatters does not trigger a conflict warning

### Outcome KPIs

- **Who**: Developers with multi-convention flag key formats
- **Does what**: Resolve flags using any convention from a single env var source
- **By how much**: One env var produces entries for all configured flag key formats
- **Measured by**: Integration test verifying multi-formatter resolution
- **Baseline**: Must choose one flag key format or maintain separate mappings

### Technical Notes

- Multiple formatters multiply the number of flag key entries per env var
- Same-value-same-key collision (from single env var through multiple formatters producing same key) should not trigger warning
- Different-value-same-key collision should trigger ConflictStrategy
- Depends on US-ENV-06 (built-in formatters), US-ENV-09 (ConflictStrategy)

---

## US-ENV-09: ConflictStrategy (WARN vs ERROR)

### Problem

Mei-Lin Chen needs to control what happens when multiple env vars map to the same flag key. In her staging environment, she wants warnings so she can detect conflicts without crashing the service. In her production environment with multiple parsers AND multiple formatters, she wants strict error-on-conflict because the cartesian explosion of combinations is too dangerous to silently resolve.

### Who

- Platform engineer | Multi-environment deployment | Wants configurable conflict handling

### Solution

`ConflictStrategy` enum with two values: `WARN` (log warning, last mapping wins) and `ERROR` (throw `IllegalStateException` at construction time). The builder accepts `.onConflict(ConflictStrategy)`. Default depends on configuration:

- Single parser + single formatter: `WARN`
- Multiple parsers + single formatter: `WARN`
- Single parser + multiple formatters: `WARN`
- Multiple parsers + multiple formatters: `ERROR` (can be overridden to `WARN`)

### Domain Examples

#### 1: WARN Strategy -- Mei-Lin's staging environment

Mei-Lin has two parsers and `FLAGZEN_CHECKOUT_FLOW=PREMIUM` plus `myAppCheckoutFlow=BASIC`. Both map to `"checkout-flow"`. ConflictStrategy is `WARN`. The provider logs a warning naming both env vars, keeps the last mapping (`"BASIC"`), and continues running.

#### 2: ERROR Strategy -- Mei-Lin's production with multi×multi

Mei-Lin configures two parsers and two formatters without setting an explicit conflict strategy. The default is `ERROR` for multi×multi. During construction, a conflict is detected. The provider throws `IllegalStateException` listing the conflicting env vars and the flag key, preventing the service from starting with ambiguous configuration.

#### 3: Explicit WARN Override on multi×multi

Mei-Lin explicitly sets `.onConflict(ConflictStrategy.WARN)` on a multi×multi configuration. The default `ERROR` is overridden. Conflicts produce warnings instead of exceptions.

### UAT Scenarios (BDD)

#### Scenario: WARN strategy logs warning and keeps last mapping

Given the provider is configured with conflict strategy WARN
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
And environment variable FF_CHECKOUT_FLOW is set to "BASIC"
And both parsers map to flag key "checkout-flow"
When the provider is constructed
Then a warning is logged mentioning "FLAGZEN_CHECKOUT_FLOW" and "FF_CHECKOUT_FLOW"
And the provider continues operating

#### Scenario: ERROR strategy throws at construction on conflict

Given the provider is configured with conflict strategy ERROR
And environment variable FLAGZEN_CHECKOUT_FLOW is set to "PREMIUM"
And environment variable FF_CHECKOUT_FLOW is set to "BASIC"
And both parsers map to flag key "checkout-flow"
When the provider is constructed
Then an IllegalStateException is thrown
And the exception message mentions both env var names and flag key "checkout-flow"

#### Scenario: Multi×multi defaults to ERROR

Given the provider is configured with 2 parsers and 2 formatters
And no explicit conflict strategy is set
When the builder builds the provider
Then the default conflict strategy is ERROR

#### Scenario: Multi×multi can be overridden to WARN

Given the provider is configured with 2 parsers and 2 formatters
And conflict strategy is explicitly set to WARN
When the builder builds the provider
Then the conflict strategy is WARN

#### Scenario: Single parser + single formatter defaults to WARN

Given the provider is configured with 1 parser and 1 formatter
And no explicit conflict strategy is set
When the builder builds the provider
Then the default conflict strategy is WARN

### Acceptance Criteria

- [ ] `ConflictStrategy` enum with values `WARN` and `ERROR`
- [ ] `WARN`: logs warning with both env var names and flag key, last mapping wins
- [ ] `ERROR`: throws `IllegalStateException` with both env var names and flag key
- [ ] Default `WARN` for: single+single, multi+single, single+multi
- [ ] Default `ERROR` for: multi+multi (more than one parser AND more than one formatter)
- [ ] `.onConflict(ConflictStrategy)` overrides the default
- [ ] Conflict detection happens at construction time, not query time

### Outcome KPIs

- **Who**: Platform engineers managing complex configurations
- **Does what**: Choose between strict (fail-fast) and lenient (warn) conflict handling
- **By how much**: Prevents silent flag value ambiguity in production
- **Measured by**: Tests for both strategies with controlled conflicts
- **Baseline**: No conflict detection -- last `System.getenv()` call wins silently

### Technical Notes

- Pattern reference: `FallbackStrategy` enum in flagzen-core for the enum design
- Conflict = same flag key produced from different env vars with different values
- Same flag key from same env var (via multiple formatters producing identical key) is not a conflict
- Construction-time detection means all conflicts are found eagerly, not at query time
- Warning logged both at parse time (during construction) AND on first access of conflicted key (US-ENV-10)
- Lives in `com.flagzen.keymapping` package (flagzen-key-mapping module -- reusable across providers)
- Depends on US-ENV-07 (multiple parsers) and US-ENV-08 (multiple formatters)

---

## US-ENV-10: Conflict Warning on First Access

### Problem

Kenji Tanaka configured his provider with `WARN` strategy during a migration. He saw the construction-time warning in the startup logs but missed it among hundreds of other startup messages. When his code later reads the conflicted flag key, he has no way to know the returned value was ambiguous. He wants a warning surfaced again on first access of a conflicted key.

### Who

- Backend developer | Migration scenario | Wants conflict visibility at point of use

### Solution

When a flag key was involved in a conflict during construction (under `WARN` strategy), the provider tracks it. On the first `getString()` call for that key, a warning is logged again mentioning the conflict. Subsequent calls for the same key do not repeat the warning.

### Domain Examples

#### 1: Happy Path -- Kenji accesses a conflicted key

During construction, `FLAGZEN_CHECKOUT_FLOW=PREMIUM` and `FF_CHECKOUT_FLOW=BASIC` both mapped to `"checkout-flow"`. A warning was logged. Later, Kenji calls `getString("checkout-flow")`. A second warning is logged: "Flag key 'checkout-flow' was resolved from conflicting env vars: FLAGZEN_CHECKOUT_FLOW, FF_CHECKOUT_FLOW." The method returns the stored value.

#### 2: Second Access -- No repeated warning

Kenji calls `getString("checkout-flow")` a second time. No warning is logged. The method returns the same value as before.

#### 3: Non-Conflicted Key -- No warning

Kenji calls `getString("max-retries")` which had no conflict. No warning is logged. Normal resolution.

### UAT Scenarios (BDD)

#### Scenario: First access of conflicted key logs a warning

Given the provider was constructed with WARN strategy
And flag key "checkout-flow" had a conflict between FLAGZEN_CHECKOUT_FLOW and FF_CHECKOUT_FLOW
When Kenji calls getString("checkout-flow") for the first time
Then a warning is logged mentioning the conflict
And the stored value is returned

#### Scenario: Subsequent access does not repeat warning

Given Kenji has already called getString("checkout-flow") once
When Kenji calls getString("checkout-flow") again
Then no additional warning is logged
And the same value is returned

#### Scenario: Non-conflicted key produces no warning on access

Given flag key "max-retries" had no conflict during construction
When Kenji calls getString("max-retries")
Then no warning is logged

### Acceptance Criteria

- [ ] Provider tracks which flag keys had conflicts during construction (WARN strategy only)
- [ ] First `getString()` call for a conflicted key logs a warning with conflict details
- [ ] Subsequent calls for the same key do not repeat the warning
- [ ] Non-conflicted keys produce no warning on access
- [ ] Warning includes the flag key and the conflicting env var names

### Outcome KPIs

- **Who**: Developers resolving flags during migration
- **Does what**: Get conflict visibility at the point of use, not just at startup
- **By how much**: Conflict warning surfaced within the code path that reads the ambiguous value
- **Measured by**: Test verifying warning on first access and no warning on subsequent access
- **Baseline**: Conflict warning only at startup, easily missed in log noise

### Technical Notes

- Implementation: `Set<String>` of conflicted keys + `Set<String>` of already-warned keys
- Thread safety: concurrent first-access from multiple threads should produce at most one warning per key
- Warning logged via standard logging (SLF4J or `java.util.logging`) -- solution-architect decides
- Only applies to `WARN` strategy -- `ERROR` strategy throws at construction, so no conflicted keys exist at runtime
- Depends on US-ENV-09 (ConflictStrategy)
