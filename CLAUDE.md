# FlagZen -- Project Instructions

## Development Paradigm

**OOP (Java)**. This is a Java 17+ library using object-oriented programming with dependency inversion (ports-and-adapters). Zero runtime reflection in the core module. Compile-time annotation processing for code generation.

## Project Structure

Gradle monorepo with submodules:

- `flagzen-core` -- annotations, annotation processor, proxy generation, FeatureDispatcher, FlagProvider SPI
- `flagzen-test` -- JUnit 5 extension, @PinFlag, @FlagSource, TestFlagContext
- `flagzen-spring` -- Spring Boot auto-configuration
- `flagzen-env` -- Environment variable FlagProvider
- Provider modules: `flagzen-launchdarkly`, `flagzen-togglz`, `flagzen-openfeature`
- Reactive modules: `flagzen-reactor`, `flagzen-mutiny`

## Conventions

- Group ID: `com.flagzen`
- Package root: `com.flagzen`
- Generated proxies: `{Feature}_FlagZenProxy` in same package as `@Feature` interface
- SPI registration: `META-INF/services/`
- Java 17+ required
- No runtime reflection in flagzen-core
- All public API types must have Javadoc

## Architecture

See `docs/feature/flagzen/design/architecture-design.md` for full architecture.
See `docs/adrs/` for architectural decision records.

## Key Design Decisions

1. One proxy class generated per @Feature interface (not a registry)
2. FlagProvider contract: `Optional<String> getString(String key)` (string-only for Release 1)
3. FeatureDispatcher is an interface with default factory method; concrete implementation is internal
4. Generated proxies: public class, package-private constructor
5. Zero runtime reflection in core -- all dispatch via compile-time generated code

## Mutation Testing Strategy

This project uses **per-feature** mutation testing. PITest runs in CI after the build job succeeds, scoped to `com.flagzen.*` classes in flagzen-core. Kill rate gate: >= 80%. Current baseline: 84% kill rate.
