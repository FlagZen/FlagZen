# Component Boundaries -- flagzen-spring (M4)

## Package Structure

```
com.flagzen.spring
  FlagZenAutoConfiguration.java    -- @AutoConfiguration, creates FlagProvider fallback + FeatureDispatcher
  FeatureProxyRegistrar.java       -- ImportBeanDefinitionRegistrar, registers per-feature proxy beans

META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

All public API is in the single `com.flagzen.spring` package. No sub-packages, no `internal` package -- the module is small enough that further decomposition would be overengineering.

## Component Responsibilities

### `FlagZenAutoConfiguration`

| Aspect | Detail |
|---|---|
| Type | `@AutoConfiguration` class |
| Responsibility | Creates `InMemoryFlagProvider` fallback bean (conditional) and `FeatureDispatcher` bean (conditional), logs startup summary |
| Inputs | `FlagProvider` bean from `ApplicationContext` (optional), `FeatureDispatcher` bean presence check |
| Outputs | `FlagProvider` bean (fallback only), `FeatureDispatcher` bean, INFO/WARN log messages |
| Conditional guards | `@ConditionalOnMissingBean(FlagProvider.class)` on fallback provider, `@ConditionalOnMissingBean(FeatureDispatcher.class)` on dispatcher |
| Imports | `@Import(FeatureProxyRegistrar.class)` |

### `FeatureProxyRegistrar`

| Aspect | Detail |
|---|---|
| Type | Implements `ImportBeanDefinitionRegistrar` |
| Responsibility | Discovers `FeatureMetadata` via `ServiceLoader`, registers a lazy singleton bean definition per `@Feature` interface |
| Inputs | `ServiceLoader<FeatureMetadata>` results |
| Outputs | Bean definitions in `BeanDefinitionRegistry` |
| Bean naming | Decapitalized simple name of feature interface (e.g., `CheckoutFlow` becomes `checkoutFlow`) |
| Error handling | Zero metadata found: logs INFO "No @Feature metadata found on classpath", registers no beans, does not fail |

### `AutoConfiguration.imports`

| Aspect | Detail |
|---|---|
| Type | Resource file |
| Location | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` |
| Content | Single line: `com.flagzen.spring.FlagZenAutoConfiguration` |
| Purpose | Spring Boot 3.x auto-configuration discovery |

## Public API Surface

The public API of flagzen-spring is intentionally minimal:

| Type | Visibility | Rationale |
|---|---|---|
| `FlagZenAutoConfiguration` | Public | Required by Spring's auto-configuration mechanism |
| `FeatureProxyRegistrar` | Public | Required by Spring's `@Import` mechanism |

No annotations, interfaces, or helper classes are exposed. The module's API is effectively "add to classpath and it works."

## Dependency Direction

```
Spring Boot ApplicationContext
  |
  v
FlagZenAutoConfiguration (adapter)
  |
  +-- reads --> FlagProvider bean (from context, or creates InMemoryFlagProvider)
  +-- creates --> FeatureDispatcher bean (DefaultFeatureDispatcher from flagzen-core)
  +-- @Import --> FeatureProxyRegistrar
                    |
                    +-- discovers --> FeatureMetadata (ServiceLoader, from flagzen-core SPI)
                    +-- registers --> Feature proxy bean definitions (resolved via FeatureDispatcher)
```

Dependencies flow **inward** toward `flagzen-core`. `flagzen-spring` never exposes core types as its own API -- it only wires them into Spring's container.

## Boundary Rules

1. **flagzen-spring depends only on flagzen-core and spring-boot-autoconfigure** -- no other FlagZen modules, no additional third-party libraries
2. **No domain logic** -- pure adapter code, wiring only
3. **No custom annotations** -- uses standard Spring annotations (`@AutoConfiguration`, `@Bean`, `@ConditionalOnMissingBean`, `@Import`)
4. **No configuration properties** for v1.1.0 -- auto-configuration is fully convention-based (no `flagzen.*` properties in `application.yml`)
5. **No reflection** -- `ServiceLoader` discovery and Spring DI are the only discovery mechanisms
