# Modules Reference

Complete index of all FlagZen modules with Maven coordinates and descriptions.

## Core

### `flagzen-core`

The foundation module. Contains annotations, annotation processor, proxy generation, `FeatureDispatcher`, and `FlagProvider` SPI.

|  Attribute   |               Value               |
| ------------ | --------------------------------- |
| Group ID     | `com.flagzen`                     |
| Artifact ID  | `flagzen-core`                    |
| Version      | `1.2.0`                           |
| Dependencies | None (zero external dependencies) |
| Java Version | 17+                               |

**What it provides**:

- `@Feature` annotation
- `@Variant` annotation with `@Repeatable` support
- `@DefaultVariant` annotation
- `@WhenTrue` / `@WhenFalse` convenience annotations
- `@CloseTo` annotation for approximate double matching
- `@Condition` annotation for predicate-based dispatch (v1.2.0)
- `FeatureType` enum (STRING, INT, LONG, BOOLEAN, DOUBLE)
- `FallbackStrategy` enum (EXCEPTION, NOOP)
- `FeatureDispatcher` interface (with default factory method)
- `FlagProvider` SPI interface
- `EvaluationContext` for targeted flag resolution
- `ContextAccessor` SPI for ambient context sources
- Annotation processor for code generation (value-based + condition-based dispatch)
- Unified ordered dispatch: exact matches and conditions coexist with `@Variant(order = N)`
- `InMemoryFlagProvider` for dev/test
- Generated proxy classes (`{Feature}_FlagZenProxy`)

**When to use**: Always. This is the base dependency.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.2.0")
    annotationProcessor("com.flagzen:flagzen-core:1.2.0")
}
```

## Testing

### `flagzen-test`

JUnit 5 extension with `@PinFlag`, `@FlagSource`, and `TestFlagContext` for testing.

|  Attribute   |                Value                |
| ------------ | ----------------------------------- |
| Group ID     | `com.flagzen`                       |
| Artifact ID  | `flagzen-test`                      |
| Version      | `1.1.0`                             |
| Depends On   | `flagzen-core`, `junit-jupiter-api` |
| Java Version | 17+                                 |

**What it provides**:

- `FlagZenExtension` (JUnit 5 extension)
- `@PinFlag` annotation for declarative flag pinning
- `@FlagSource` annotation for loading flags from properties files
- `TestFlagContext` for programmatic flag setup

**When to use**: In test suites (`testImplementation`). Not needed in production.

**Gradle**:

```gradle
dependencies {
    testImplementation("com.flagzen:flagzen-test:1.1.0")
}
```

## Key Mapping

### `flagzen-key-mapping`

Reusable parser/formatter infrastructure for converting source names (env vars, config properties) to flag keys.

|  Attribute   |               Value               |
| ------------ | --------------------------------- |
| Group ID     | `com.flagzen`                     |
| Artifact ID  | `flagzen-key-mapping`             |
| Version      | `1.1.0`                           |
| Dependencies | None (zero external dependencies) |
| Java Version | 17+                               |

**What it provides**:

- `FlagKeyParser` interface
- `FlagKeyParsers` factory (screamingSnakeCase, camelCase)
- `FlagKeyFormat` interface
- `FlagKeyFormats` factory (kebabCase, snakeCase, camelCase, pascalCase, dotCase, colonCase)
- `ConflictStrategy` enum (WARN, ERROR)

**When to use**: When implementing custom `FlagProvider`s that parse source names (env vars, config files, etc.). Used transitively by `flagzen-env`.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-key-mapping:1.1.0")
}
```

## Providers

### `flagzen-env`

Environment variable `FlagProvider` with configurable key mapping.

|  Attribute   |                 Value                 |
| ------------ | ------------------------------------- |
| Group ID     | `com.flagzen`                         |
| Artifact ID  | `flagzen-env`                         |
| Version      | `1.1.0`                               |
| Depends On   | `flagzen-core`, `flagzen-key-mapping` |
| Java Version | 17+                                   |

**What it provides**:

- `EnvironmentVariableFlagProvider` — reads flags from `System.getenv()`
- Configurable parsers/formatters for mapping env var names to flag keys
- Eager loading of all env vars at construction time
- Conflict detection and strategy handling

**When to use**: When you want FlagZen flags to be driven by environment variables (common in cloud deployments).

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-env:1.1.0")
}
```

**Example**:

```java
FlagProvider provider = new EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
    .format(FlagKeyFormats.kebabCase())
    .build();

// With env var: FLAGZEN_CHECKOUT_FLOW=PREMIUM
// provider.getString("checkout-flow") returns Optional.of("PREMIUM")
```

### `flagzen-launchdarkly`

LaunchDarkly Java Server SDK adapter with native typed resolution.

|  Attribute   |                     Value                      |
| ------------ | ---------------------------------------------- |
| Group ID     | `com.flagzen`                                  |
| Artifact ID  | `flagzen-launchdarkly`                         |
| Version      | `1.2.0`                                        |
| Depends On   | `flagzen-core`, `launchdarkly-java-server-sdk` |
| Java Version | 17+                                            |

**What it provides**:

- `LaunchDarklyFlagProvider` — adapter using native typed `*VariationDetail` methods (no string round-tripping)
- `EvaluationContext` mapping to `LDContext` (single-kind "user" context)
- Reason-kind-based absence detection (ERROR, PREREQUISITE_FAILED = absent; OFF = resolved)
- Long value support via `jsonValueVariationDetail` (full `long` range)
- Anonymous `LDContext` singleton for non-context calls

**When to use**: When you manage flags via LaunchDarkly's control plane.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-launchdarkly:1.2.0")
}
```

### `flagzen-togglz`

Togglz feature toggle adapter with boolean and string resolution.

|  Attribute   |             Value             |
| ------------ | ----------------------------- |
| Group ID     | `com.flagzen`                 |
| Artifact ID  | `flagzen-togglz`              |
| Version      | `1.2.0`                       |
| Depends On   | `flagzen-core`, `togglz-core` |
| Java Version | 17+                           |

**What it provides**:

- `TogglzFlagProvider` — adapter using `FeatureState.isEnabled()` for boolean, strategy parameter `"value"` for string
- `FeatureLookup` — case-insensitive string-to-Feature cache
- Numeric types via default `FlagProvider` string parsing
- One-time INFO log when `EvaluationContext` is passed (context not supported by Togglz)

**When to use**: When you manage flags via Togglz.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-togglz:1.2.0")
}
```

### `flagzen-openfeature`

OpenFeature SDK adapter.

|  Attribute   |               Value               |
| ------------ | --------------------------------- |
| Group ID     | `com.flagzen`                     |
| Artifact ID  | `flagzen-openfeature`             |
| Version      | `1.1.0`                           |
| Depends On   | `flagzen-core`, `openfeature-sdk` |
| Java Version | 17+                               |

**What it provides**:

- `OpenFeatureFlagProvider` — adapter to OpenFeature SDK
- Reason-based detection for absent flags

**When to use**: When you use OpenFeature-compliant flag providers (vendor-neutral approach).

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-openfeature:1.1.0")
}
```

## Framework Integration

### `flagzen-spring`

Spring Boot auto-configuration for feature proxy injection.

|      Attribute      |                    Value                    |
| ------------------- | ------------------------------------------- |
| Group ID            | `com.flagzen`                               |
| Artifact ID         | `flagzen-spring`                            |
| Version             | `1.1.0`                                     |
| Depends On          | `flagzen-core`, `spring-boot-autoconfigure` |
| Java Version        | 17+                                         |
| Spring Boot Version | 2.7+                                        |

**What it provides**:

- `FlagZenAutoConfiguration` — Spring Boot auto-configuration
- `FeatureDispatcher` bean registration
- Feature proxy beans for `@Autowired` injection
- `FeatureProxyRegistrar` for dynamic bean discovery

**When to use**: When building Spring Boot applications (recommended for all Spring Boot projects using FlagZen).

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
}
```

### `flagzen-reactor`

Reactor `ContextAccessor` for reactive context propagation.

|  Attribute   |             Value              |
| ------------ | ------------------------------ |
| Group ID     | `com.flagzen`                  |
| Artifact ID  | `flagzen-reactor`              |
| Version      | `1.3.0` (planned)             |
| Depends On   | `flagzen-core`, `reactor-core` |
| Java Version | 17+                            |

**What it provides**:

- `ReactorContextAccessor` — provides `EvaluationContext` from Reactor's `Context`
- Integration with reactive streams

**When to use**: When using Project Reactor for reactive processing and want flags to be context-aware in reactive pipelines.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-reactor:1.3.0") // not yet released
}
```

### `flagzen-mutiny`

Mutiny `ContextAccessor` for reactive context propagation.

|  Attribute   |               Value               |
| ------------ | --------------------------------- |
| Group ID     | `com.flagzen`                     |
| Artifact ID  | `flagzen-mutiny`                  |
| Version      | `1.3.0` (planned)                |
| Depends On   | `flagzen-core`, `smallrye-mutiny` |
| Java Version | 17+                               |

**What it provides**:

- `MutinyContextAccessor` — provides `EvaluationContext` from Mutiny's context
- Integration with Mutiny reactive streams

**When to use**: When using Mutiny for reactive processing and want flags to be context-aware.

**Gradle**:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-mutiny:1.3.0") // not yet released
}
```

## Examples

### `flagzen-examples`

Runnable example applications demonstrating FlagZen usage patterns.

|  Attribute  |                Value                |
| ----------- | ----------------------------------- |
| Group ID    | `com.flagzen`                       |
| Artifact ID | `flagzen-examples`                  |
| Version     | `1.1.0`                             |
| Status      | Not published to Maven Central      |
| Build       | Available in source repository only |

**What it contains**:

- Spring Boot example with environment variable provider
- Standalone example with custom provider
- Testing examples with `@PinFlag` and `@FlagSource`
- Integration with LaunchDarkly
- Reactive example with Reactor

**When to use**: Reference these when learning FlagZen or creating your own integration.

## Dependency Graph

```
flagzen-core (no external deps)
  ↑
  ├── flagzen-test (adds: junit-jupiter-api)
  ├── flagzen-key-mapping (no external deps)
  ├── flagzen-env (depends on flagzen-key-mapping)
  ├── flagzen-spring (adds: spring-boot-autoconfigure)
  ├── flagzen-reactor (adds: reactor-core)
  ├── flagzen-mutiny (adds: smallrye-mutiny)
  ├── flagzen-launchdarkly (adds: launchdarkly-java-server-sdk)
  ├── flagzen-togglz (adds: togglz-core)
  └── flagzen-openfeature (adds: openfeature-sdk)
```

All extension modules depend on `flagzen-core` only. No cross-dependencies between extensions.

## Version Management

All modules are released together with the same version number. Using `com.flagzen:*:1.1.0` ensures compatibility across modules.

**Gradle BOM (Bill of Materials)**

Currently, no official BOM is published. Specify versions explicitly:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    implementation("com.flagzen:flagzen-spring:1.1.0")
    implementation("com.flagzen:flagzen-env:1.1.0")
    testImplementation("com.flagzen:flagzen-test:1.1.0")
}
```

## Support Matrix

|        Module        | Java | Spring Boot |             Notes              | Javadoc                                                              |
| -------------------- | ---- | ----------- | ------------------------------ | -------------------------------------------------------------------- |
| flagzen-core         | 17+  | N/A         | Zero external dependencies     | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-core)          |
| flagzen-test         | 17+  | N/A         | JUnit 5 only                   | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-test)          |
| flagzen-key-mapping  | 17+  | N/A         | Zero external dependencies     | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-key-mapping)   |
| flagzen-env          | 17+  | N/A         | Pure Java, no Spring required  | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-env)           |
| flagzen-spring       | 17+  | 2.7+        | Tested with 2.7 LTS and 3.x    | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-spring)        |
| flagzen-reactor      | 17+  | N/A         | v1.3.0 (planned)              | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-reactor)       |
| flagzen-mutiny       | 17+  | N/A         | v1.3.0 (planned)              | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-mutiny)        |
| flagzen-launchdarkly | 17+  | N/A         | LaunchDarkly SDK 7.0+          | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-launchdarkly)  |
| flagzen-togglz       | 17+  | N/A         | Togglz 4.x                     | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-togglz)        |
| flagzen-openfeature  | 17+  | N/A         | OpenFeature SDK 1.0+           | [API docs](https://javadoc.io/doc/com.flagzen/flagzen-openfeature)   |

## Minimal Setup

For a Spring Boot application with environment variables:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    implementation("com.flagzen:flagzen-spring:1.1.0")
    implementation("com.flagzen:flagzen-env:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")

    testImplementation("com.flagzen:flagzen-test:1.1.0")
}
```

## Related

- [Architecture Overview](../explanation/architecture.md) — module dependency graph and roles
- [Design Decisions](../explanation/design-decisions.md) — rationale behind module split
