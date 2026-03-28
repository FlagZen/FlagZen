# ADR-015: Key Mapping Module Split

## Status

Accepted

## Context

The flagzen-env feature introduces a parse/format pipeline for converting source system key names (e.g., environment variable names) into flag keys. This pipeline consists of:

- `FlagKeyParser` -- parses a source key name into segments
- `FlagKeyFormat` -- formats segments into a flag key string
- `FlagKeyParsers` / `FlagKeyFormats` -- built-in factory companions
- `ConflictStrategy` -- controls behavior when multiple source keys map to the same flag key

The question is whether these types should live in `flagzen-env` alongside the environment variable provider, or in a separate reusable module.

Future providers (`flagzen-file`, `flagzen-vault`, `flagzen-consul`) face the same key-mapping problem: config property names, vault paths, and consul keys all need parsing and formatting into flag keys.

## Decision

Extract key-mapping infrastructure into a separate `flagzen-key-mapping` module (`com.flagzen.keymapping` package) with zero external dependencies. `flagzen-env` depends on `flagzen-key-mapping` (api scope) and `flagzen-core` (implementation scope).

## Alternatives Considered

### Alternative 1: Key-mapping types in flagzen-env

All parser/formatter/conflict types live in `com.flagzen.env`.

- **Pro**: Simpler project structure (one module instead of two). Fewer Gradle files.
- **Pro**: No transitive dependency coordination.
- **Con**: Future providers (`flagzen-file`, `flagzen-vault`) would depend on `flagzen-env` just to get parsers/formatters. This creates a false dependency: vault provider depends on env var provider.
- **Con**: Violates single responsibility -- env var provider module would own generic key-mapping concerns.
- **Rejected because**: false dependency chain is a worse trade-off than the small overhead of a second module.

### Alternative 2: Key-mapping types in flagzen-core

All parser/formatter types live in `com.flagzen` or `com.flagzen.spi` within flagzen-core.

- **Pro**: Available to all modules without additional dependency.
- **Pro**: Simplest dependency graph.
- **Con**: Bloats flagzen-core with types that only provider modules need. Developers using InMemoryFlagProvider or programmatic providers get parser/formatter types they never use.
- **Con**: Violates flagzen-core's "minimal, zero-dependency" design principle.
- **Rejected because**: flagzen-core should remain minimal; key mapping is a provider concern, not a core concern.

### Alternative 3: Key-mapping types as part of an SPI extension in flagzen-core

Define `FlagKeyParser` and `FlagKeyFormat` in `com.flagzen.spi` as additional SPI contracts.

- **Pro**: No new module.
- **Con**: These are not SPIs in the ServiceLoader sense -- they are utility interfaces. Mixing them with real SPIs (`FlagProvider`, `ContextAccessor`) dilutes the SPI package's purpose.
- **Rejected because**: conceptual mismatch; parsers/formatters are configuration types, not service provider contracts.

## Consequences

### Positive

- Future providers reuse key-mapping without depending on flagzen-env
- flagzen-key-mapping has zero dependencies -- usable outside FlagZen entirely
- Clean single responsibility per module
- Consumers of flagzen-env get flagzen-key-mapping transitively (no extra dependency declaration)

### Negative

- Two modules to maintain instead of one (small overhead for a single developer)
- Two `build.gradle.kts` files, two test suites
- Consumers see `com.flagzen.keymapping` and `com.flagzen.env` packages (minor complexity in import statements)
