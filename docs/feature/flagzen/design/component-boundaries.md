# Component Boundaries -- FlagZen

## Module Decomposition

Each module is a Gradle submodule, published as a separate JAR to Maven Central. The decomposition follows the principle of **one concern per module** with **dependency inversion** at the core.

### flagzen-core

**Responsibility**: Defines the programming model (annotations), processes annotations at compile time (validation + code generation), provides the runtime dispatch API (FeatureDispatcher), and defines extension SPIs (FlagProvider, ContextAccessor).

**Contains**:

- Annotations: `@Feature`, `@Variant`, `@DefaultVariant`, `FallbackStrategy`
- Annotation processor: `FlagZenProcessor` (validation, model extraction)
- Code generator: `ProxyGenerator` (JavaPoet-based proxy generation)
- Runtime API: `FeatureDispatcher` (interface), `FlagZen` (factory/entry point)
- SPIs: `FlagProvider`, `ContextAccessor`
- Built-in: `InMemoryFlagProvider` (dev/test convenience)
- Exceptions: `FlagZenException`, `UnmatchedVariantException`

**External dependencies**: None at runtime. JavaPoet at compile time (annotation processor classpath only).

**Boundary rules**:

- All SPIs are in `com.flagzen.spi` -- stable public contract
- All internals are in `com.flagzen.internal` -- package-private, not part of public API
- Annotation processor is in `com.flagzen.processor` -- compile-time only, not loaded at runtime
- No class in flagzen-core may import from any extension module

### flagzen-test

**Responsibility**: JUnit 5 integration for testing flag-dependent code with minimal setup.

**Contains**:

- `@PinFlag` annotation (pins a variant for a test method)
- `@FlagSource` annotation (loads flags from properties/JSON/YAML files)
- `FlagZenExtension` (JUnit 5 extension: `BeforeEachCallback`, `AfterEachCallback`, `ParameterResolver`)
- `TestFlagContext` (programmatic pinning API, injectable as JUnit parameter)

**External dependencies**: junit-jupiter-api, flagzen-core

**Boundary rules**:

- Must not depend on any provider module or DI module
- Must not require a FlagProvider implementation beyond `InMemoryFlagProvider` from core
- `TestFlagContext` uses thread-local isolation for parallel test safety

### flagzen-env

**Responsibility**: Reads flag values from environment variables.

**Contains**:

- `EnvironmentVariableFlagProvider` (implements `FlagProvider`)
- SPI registration: `META-INF/services/com.flagzen.spi.FlagProvider`

**External dependencies**: flagzen-core

**Boundary rules**:

- Single class + SPI registration. Minimal module.
- Naming convention for env vars: configurable (default: uppercase, dots to underscores, e.g., `checkout-flow` -> `CHECKOUT_FLOW`)

### flagzen-spring

**Responsibility**: Spring Boot auto-configuration so that `@Autowired` injection of `@Feature` interfaces works with zero configuration.

**Contains**:

- `FlagZenAutoConfiguration` (Spring Boot auto-configuration class)
- `FeatureFactoryBean<T>` (creates proxy beans per @Feature interface)
- Auto-configuration metadata

**External dependencies**: flagzen-core, spring-boot-autoconfigure, spring-context

**Boundary rules**:

- Auto-configuration is conditional on flagzen-core being on classpath
- Must not require any specific `FlagProvider` -- discovers from `ApplicationContext`
- Must not depend on flagzen-test, flagzen-env, or any provider module

### flagzen-reactor

**Responsibility**: Reads `EvaluationContext` from Reactor's subscriber `Context` for reactive context propagation in Spring WebFlux applications.

**Contains**:

- `ReactorContextAccessor` (implements `ContextAccessor`)
- SPI registration: `META-INF/services/com.flagzen.spi.ContextAccessor`

**External dependencies**: flagzen-core, reactor-core

### flagzen-mutiny

**Responsibility**: Reads `EvaluationContext` from Mutiny's `Context` for reactive context propagation in Quarkus Reactive applications.

**Contains**:

- `MutinyContextAccessor` (implements `ContextAccessor`)
- SPI registration: `META-INF/services/com.flagzen.spi.ContextAccessor`

**External dependencies**: flagzen-core, smallrye-mutiny

### Provider Adapter Modules

**Pattern**: Each adapter module follows the same structure:

|        Module        |            Wraps             |                   Contains                    |
| -------------------- | ---------------------------- | --------------------------------------------- |
| flagzen-launchdarkly | LaunchDarkly Java Server SDK | `LaunchDarklyFlagProvider` + SPI registration |
| flagzen-togglz       | Togglz Core                  | `TogglzFlagProvider` + SPI registration       |
| flagzen-openfeature  | OpenFeature SDK              | `OpenFeatureFlagProvider` + SPI registration  |

**Boundary rules** (all adapters):

- Implements `FlagProvider` only
- Depends on flagzen-core + the external SDK
- No cross-adapter dependencies
- Adapter translates `getString(key)` to the provider-specific API call

## Boundary Enforcement

### Gradle-level Enforcement

Each module's `build.gradle.kts` declares only its allowed dependencies. Gradle dependency constraints prevent accidental cross-module coupling.

```
// Example: flagzen-env/build.gradle.kts
dependencies {
    implementation(project(":flagzen-core"))
    // NO other flagzen modules allowed
}
```

### ArchUnit Enforcement (in flagzen-core test suite)

|                                        Rule                                         |               What it prevents               |
| ----------------------------------------------------------------------------------- | -------------------------------------------- |
| No `java.lang.reflect` in `com.flagzen..` (excluding `com.flagzen.processor..`)     | Runtime reflection in core                   |
| Classes in `com.flagzen.internal..` must not be public                              | Internal API leakage                         |
| No cycles in `com.flagzen.(*)..` slices                                             | Circular dependencies                        |
| `com.flagzen.processor..` must not be referenced from `com.flagzen` (non-processor) | Runtime dependency on compile-time-only code |

## Dependency Direction

All dependencies point inward toward flagzen-core. No outward or lateral dependencies between extension modules.

```
                 flagzen-core (center)
               /    |     |      \     \
            test   env  spring reactor  mutiny
                          |
                    (Spring Boot)

            launchdarkly  togglz  openfeature
                 |           |         |
             (LD SDK)    (Togglz)   (OF SDK)
```

## Public API Surface

The public API of FlagZen (what consumers directly interact with) is intentionally minimal:

### Consumer-facing types (flagzen-core)

|            Type             |    Kind    |                  Purpose                  |
| --------------------------- | ---------- | ----------------------------------------- |
| `@Feature`                  | Annotation | Marks an interface as a feature flag      |
| `@Variant`                  | Annotation | Marks a class as a variant implementation |
| `@DefaultVariant`           | Annotation | Marks a class as the fallback variant     |
| `FallbackStrategy`          | Enum       | REQUIRED, EXCEPTION, NOOP                 |
| `FeatureDispatcher`         | Interface  | Resolves @Feature to proxy                |
| `FlagZen`                   | Class      | Factory: `dispatcher()`, `configure()`    |
| `FlagZenException`          | Class      | Base exception                            |
| `UnmatchedVariantException` | Class      | Runtime mismatch exception                |

### SPI types (com.flagzen.spi)

|       Type        |   Kind    |                                Purpose                                |
| ----------------- | --------- | --------------------------------------------------------------------- |
| `FlagProvider`    | Interface | `Optional<String> getString(String key)`                              |
| `ContextAccessor` | Interface | `Optional<EvaluationContext> getContext()`                            |
| `FeatureMetadata` | Interface | Generated per @Feature; proxy factory for cross-package instantiation |

### Test types (flagzen-test)

|        Type        |    Kind    |          Purpose          |
| ------------------ | ---------- | ------------------------- |
| `@PinFlag`         | Annotation | Pins a variant in a test  |
| `@FlagSource`      | Annotation | Loads flags from a file   |
| `FlagZenExtension` | Class      | JUnit 5 extension         |
| `TestFlagContext`  | Class      | Programmatic test pinning |

**Target**: <20 public types, <50 public methods across all consumer-facing API.
