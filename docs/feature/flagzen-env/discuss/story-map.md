# Story Map: flagzen-env (Environment Variable Provider)

## User: Kenji Tanaka (backend developer, 12-factor apps, Kubernetes)

## Secondary User: Mei-Lin Chen (platform engineer, shared Kubernetes cluster)

## Goal: Source feature flag values from environment variables with sensible defaults, clean parse/format separation, and configurable conflict handling

## Backbone

|      Add Dependency       |                          Set Env Vars                           |        Configure Provider        |                 Resolve Flags                 |
| ------------------------- | --------------------------------------------------------------- | -------------------------------- | --------------------------------------------- |
| Add flagzen-env to Gradle | Default FLAGZEN_SCREAMING_SNAKE convention                      | Zero-config create()             | getString() from immutable map                |
|                           | screamingSnakeCase parser (with/without prefix)                 | ServiceLoader auto-discovery     | Typed resolution (int, boolean, long, double) |
|                           | camelCase parser (with/without prefix)                          | Builder with custom parser       | Missing key returns empty                     |
|                           | Custom lambda parser                                            | Builder with custom formatter    | Context-aware passthrough                     |
|                           | kebabCase formatter (default)                                   | Builder with multiple parsers    | Conflict warning on first access              |
|                           | snakeCase, camelCase, pascalCase, dotCase, colonCase formatters | Builder with multiple formatters |                                               |
|                           | Custom lambda formatter                                         | ConflictStrategy (WARN/ERROR)    |                                               |

---

### Walking Skeleton

The thinnest end-to-end slice delivering working behavior:

- **Add Dependency**: flagzen-env Gradle module exists with transitive dep on flagzen-core
- **Set Env Vars**: Default parse-format pipeline (FLAGZEN_ prefix parser + kebab formatter)
- **Configure Provider**: `create()` factory + ServiceLoader registration with default config
- **Resolve Flags**: getString() reads from immutable map, returns `Optional.empty()` when absent

Stories: US-ENV-01, US-ENV-02, US-ENV-03, US-ENV-05 (parsers), US-ENV-06 (formatters)

Note: US-ENV-05 and US-ENV-06 are structural dependencies -- the zero-config default uses `FlagKeyParsers.screamingSnakeCase("FLAGZEN_")` and `FlagKeyFormats.kebabCase()`. The walking skeleton only needs the default parser and default formatter, not all built-in variants. The remaining variants in US-ENV-05 and US-ENV-06 are exercised in R1 but only the defaults are blocking.

### Release 1: Sensible Defaults (Walking Skeleton)

The core module with zero-config behavior. A developer adds the dependency, sets env vars following `FLAGZEN_SCREAMING_SNAKE` convention, and flags resolve automatically via immutable map.

- US-ENV-01: Zero-config default (FLAGZEN_ prefix + kebab formatter)
- US-ENV-02: Eager loading with immutable map
- US-ENV-03: ServiceLoader registration
- US-ENV-05: Built-in parsers (screamingSnakeCase, camelCase -- at minimum screamingSnakeCase with FLAGZEN_ prefix)
- US-ENV-06: Built-in formatters (all 6 -- at minimum kebabCase)

Target outcome: Flags resolve from env vars end-to-end with zero configuration.

### Release 2: Custom Configuration

Adds customization for teams with different conventions. Each story is independently useful once R1 is delivered.

- US-ENV-04: Custom parser configuration (custom prefix, custom lambda parser)

Target outcome: Developers can match any env var naming convention via builder.

### Release 3: Multi-Convention Support

Adds multiple parsers, multiple formatters, and conflict handling.

- US-ENV-07: Multiple parsers (legacy migration)
- US-ENV-08: Multiple formatters (multi-convention codebase)
- US-ENV-09: ConflictStrategy (WARN vs ERROR, default rules)
- US-ENV-10: Conflict warning on first access

Target outcome: One provider instance handles complex multi-convention environments with explicit conflict control.

## Module Delivery

This feature ships as two modules:

- **flagzen-key-mapping**: FlagKeyParser, FlagKeyParsers, FlagKeyFormat, FlagKeyFormats, ConflictStrategy. Reusable key-mapping infrastructure for any provider (env vars, files, vault, etc.). Package: `com.flagzen.keymapping`.
- **flagzen-env**: EnvironmentVariableFlagProvider + builder + ServiceLoader registration. Depends on flagzen-key-mapping and flagzen-core. Package: `com.flagzen.env`.

Stories US-ENV-05 (parsers), US-ENV-06 (formatters), and US-ENV-09 (ConflictStrategy) live in flagzen-key-mapping. All other stories live in flagzen-env.

## Scope Assessment: PASS -- 10 stories, 2 modules (flagzen-key-mapping + flagzen-env), estimated 6-8 days
