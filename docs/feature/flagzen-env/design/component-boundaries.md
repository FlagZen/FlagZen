# Component Boundaries -- flagzen-env

## Module: flagzen-key-mapping

### Package: `com.flagzen.keymapping`

| Type | Visibility | Responsibility |
| --- | --- | --- |
| `FlagKeyParser` | public interface | `@FunctionalInterface` SAM. Parses a source system key name into segments. Returns `Optional<List<String>>` (empty = does not match). |
| `FlagKeyParsers` | public final class | Static factory companion. Provides built-in parsers: `screamingSnakeCase(String prefix)`, `screamingSnakeCase()`, `camelCase(String prefix)`, `camelCase()`, `snakeCase(String prefix)`, `snakeCase()`. Not instantiable. |
| `FlagKeyFormat` | public interface | `@FunctionalInterface` SAM. Formats a `List<String>` of segments into a flag key string. |
| `FlagKeyFormats` | public final class | Static factory companion. Provides built-in formatters: `kebabCase()`, `snakeCase()`, `camelCase()`, `pascalCase()`, `dotCase()`, `colonCase()`. Not instantiable. |
| `ConflictStrategy` | public enum | Two values: `WARN`, `ERROR`. Controls behavior when multiple source keys map to the same flag key. |

### API Surface

- 5 public types, all in `com.flagzen.keymapping`
- No internal/package-private types (the module is small enough to be entirely public)
- All types require Javadoc (CLAUDE.md requirement)
- `FlagKeyParsers` and `FlagKeyFormats` are `final` with private constructor (utility classes)

### Dependencies

- None. Zero compile-time and runtime dependencies. Not even flagzen-core.
- Test scope only: JUnit 5, AssertJ

## Module: flagzen-env

### Package: `com.flagzen.env`

| Type | Visibility | Responsibility |
| --- | --- | --- |
| `EnvironmentVariableFlagProvider` | public class | Implements `FlagProvider`. Eagerly loads env vars at construction via parse/format pipeline. `getString()` is a pure map lookup. Contains nested `Builder` class. |
| `EnvironmentVariableFlagProvider.Builder` | public static class | Fluent builder for configuring parser(s), formatter(s), conflict strategy, and environment source. Validates and constructs the provider. |

### Internal Types (package-private)

The crafter decides whether internal helper types are needed. Possible candidates (crafter's discretion):

- Conflict tracking data structure for first-access warnings
- Pipeline execution helper

These are implementation details, not architectural decisions.

### API Surface

- 1 public class + 1 public nested builder class
- ServiceLoader registration file: `META-INF/services/com.flagzen.spi.FlagProvider`
- All public types require Javadoc (CLAUDE.md requirement)

### Dependencies

| Dependency | Scope | Rationale |
| --- | --- | --- |
| `flagzen-core` | implementation | `FlagProvider` SPI interface |
| `flagzen-key-mapping` | api | Parser/formatter types exposed in builder API (callers import `FlagKeyParsers`, `FlagKeyFormats`) |
| JUnit 5 | test | Testing |
| AssertJ | test | Assertions |

### Key Dependency Decision: `api` vs `implementation` for flagzen-key-mapping

`flagzen-key-mapping` is declared as `api` (not `implementation`) because the builder's `.parser(FlagKeyParser)` and `.formatter(FlagKeyFormat)` methods expose key-mapping types in the public API. Consumers who call the builder need `FlagKeyParser` and `FlagKeyFormat` on their compile classpath.

Consumers who use only `create()` (zero-config) do not directly reference key-mapping types, but Gradle's `api` scope ensures they are available if needed.

## Module Boundary Rules

1. **flagzen-key-mapping is provider-agnostic.** It must not import anything from `com.flagzen.env` or `com.flagzen.spi` or `com.flagzen.internal`.
2. **flagzen-env depends inward.** It depends on `flagzen-core` (SPI) and `flagzen-key-mapping` (pipeline). It does not depend on any other FlagZen extension module.
3. **No circular dependencies.** `flagzen-key-mapping` does not know about `flagzen-env`.
4. **No cross-module package sharing.** `com.flagzen.keymapping` lives only in `flagzen-key-mapping`. `com.flagzen.env` lives only in `flagzen-env`.

## Package Structure (Full)

```
flagzen-key-mapping/
  src/main/java/
    com/flagzen/keymapping/
      FlagKeyParser.java
      FlagKeyParsers.java
      FlagKeyFormat.java
      FlagKeyFormats.java
      ConflictStrategy.java

flagzen-env/
  src/main/java/
    com/flagzen/env/
      EnvironmentVariableFlagProvider.java  (contains nested Builder)
  src/main/resources/
    META-INF/services/
      com.flagzen.spi.FlagProvider
```
