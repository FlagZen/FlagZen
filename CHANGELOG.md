# Changelog

All notable changes to FlagZen are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-03-28

### Added

#### Core (`flagzen-core`)

- `@Feature` annotation for marking feature flag interfaces
- `@Variant` annotation with support for string, int, long, boolean, and double values
- `@DefaultVariant` for fallback implementations
- `@WhenTrue` / `@WhenFalse` convenience annotations for boolean features
- `@CloseTo` annotation for approximate double matching with configurable tolerance
- `FeatureType` enum: STRING, INT, LONG, BOOLEAN, DOUBLE
- `FallbackStrategy` enum: REQUIRED, EXCEPTION, NOOP
- Compile-time annotation processor (`FlagZenProcessor`) with zero runtime reflection
- Generated proxy classes (`*_FlagZenProxy`) for polymorphic dispatch
- `FeatureDispatcher` interface with `resolve()` and context-aware `resolve()` methods
- `FlagProvider` SPI with typed accessor methods (`getBoolean`, `getInt`, `getLong`, `getDouble`)
- Context-aware typed methods delegate through `getString(key, context)` for proper context propagation
- `EvaluationContext` model with targeting key and attributes
- `FlagContext` for block-scoped and thread-local context management
- `ContextAccessor` SPI for ambient context sources
- Multi-value variant mapping: `@Variant(value = {"A", "B"})`, `@Variant(intValue = {3, 5})`
- Compile-time duplicate detection across multi-value arrays
- `@CloseTo` overlap detection (inter-variant and intra-variant)
- `InMemoryFlagProvider` for development and testing

#### Testing (`flagzen-test`)

- `FlagZenExtension` JUnit 5 extension
- `@PinFlag` for declarative flag pinning in tests
- `@FlagSource` for loading flags from properties files
- `TestFlagContext` for programmatic pinning
- Feature interface parameter injection in test methods
- Priority order: `@PinFlag` > `@FlagSource` > provider

#### Key Mapping (`flagzen-key-mapping`)

- `FlagKeyParser` SAM interface for parsing source key names into segments
- `FlagKeyParsers` with built-in parsers: `screamingSnakeCase`, `camelCase` (with/without prefix)
- `FlagKeyFormat` SAM interface for formatting segments into flag keys
- `FlagKeyFormats` with built-in formatters: kebab, snake, camel, pascal, dot, colon case
- `ConflictStrategy` enum (WARN, ERROR) with cardinality-based defaults

#### Environment Variable Provider (`flagzen-env`)

- `EnvironmentVariableFlagProvider` implementing `FlagProvider`
- Eager loading at construction into immutable map
- Configurable parsers, formatters, and conflict strategy via builder API
- `Supplier<Map<String, String>>` injection for testability
- First-access conflict warning for ambiguous key mappings
- ServiceLoader auto-discovery with zero-config defaults (`FLAGZEN_` prefix, kebab-case output)

#### Spring Integration (`flagzen-spring`)

- `FlagZenAutoConfiguration` for Spring Boot 3.x
- Automatic `FeatureDispatcher` bean creation from `FlagProvider` bean
- `@Feature` proxy injection via `@Autowired`
- `@ConditionalOnMissingBean` guards for custom overrides
- `InMemoryFlagProvider` fallback with warning log
- Startup diagnostics logging

#### OpenFeature Adapter (`flagzen-openfeature`)

- `OpenFeatureFlagProvider` bridging OpenFeature SDK to FlagZen `FlagProvider`
- Reason-based absence detection via details API
- Native typed delegation (`getBoolean`, `getInt`, `getDouble` via OpenFeature typed methods)
- `EvaluationContext` mapping (FlagZen context to OpenFeature context)
- Dual constructors: no-arg (global client) and parameterized (injected client)

#### Documentation

- Project README with quick start guide
- Per-module READMEs with API overview and cross-references
- Documentation site (flagzen.com) with DIVIO structure: tutorials, how-to guides, reference, explanation
- `flagzen-examples` module with runnable examples and tests
- CONTRIBUTING.md, SECURITY.md, CODE_OF_CONDUCT.md
- GitHub issue templates and PR template

[1.1.0]: https://github.com/FlagZen/FlagZen/releases/tag/v1.1.0
