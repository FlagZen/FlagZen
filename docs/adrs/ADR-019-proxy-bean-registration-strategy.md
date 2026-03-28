# ADR-019: Proxy Bean Registration Strategy

## Status

Proposed

## Context

flagzen-spring needs to register one Spring bean per `@Feature` interface discovered via `ServiceLoader<FeatureMetadata>`. The number and types of these beans are not known at compile time -- they depend on which `@Feature` interfaces the consumer's annotation processor generated. The registration mechanism must:

1. Register beans with the correct feature interface type (so `@Autowired CheckoutFlow` works)
2. Resolve beans lazily via `FeatureDispatcher.resolve(featureType)` after the `FeatureDispatcher` bean is available
3. Integrate cleanly with `@AutoConfiguration` and `@ConditionalOnMissingBean` patterns
4. Handle zero metadata gracefully (no error)

The quality attributes driving this decision are **maintainability** (standard Spring patterns, minimal custom code) and **testability** (beans must be overridable in tests via `@TestConfiguration`).

## Decision

Use `ImportBeanDefinitionRegistrar` implemented by `FeatureProxyRegistrar`, imported via `@Import` from `FlagZenAutoConfiguration`.

`FeatureProxyRegistrar` implements `ImportBeanDefinitionRegistrar.registerBeanDefinitions()`:

- Calls `ServiceLoader.load(FeatureMetadata.class)` to discover all feature metadata
- For each metadata entry, creates a `GenericBeanDefinition` (or `RootBeanDefinition`) with:
  - Bean class set to the feature interface type
  - Instance supplier that resolves the bean via `BeanFactory.getBean(FeatureDispatcher.class).resolve(featureType)`
  - Singleton scope, lazy-init enabled
- Registers the bean definition with decapitalized interface name as bean name

## Alternatives Considered

### Alternative A: `BeanDefinitionRegistryPostProcessor`

A `BeanDefinitionRegistryPostProcessor` runs after all `@Configuration` classes are processed, allowing dynamic bean definition registration.

- **Pro**: Full access to `BeanDefinitionRegistry`, runs at the right lifecycle phase
- **Pro**: Can inspect existing bean definitions for conflict detection
- **Con**: Requires a separate `@Bean` method in `FlagZenAutoConfiguration` to register the post-processor, adding indirection
- **Con**: Post-processors run in a complex ordering phase; interaction with other post-processors can be fragile
- **Con**: `@ConditionalOnMissingBean` on the post-processor itself does not prevent individual feature bean registration -- additional conditional logic needed inside the post-processor
- **Rejected because**: More complex lifecycle, harder to test, no advantage over `ImportBeanDefinitionRegistrar` for this use case

### Alternative B: `FactoryBean<T>` per feature (FeatureFactoryBean)

Register a `FactoryBean<T>` `@Bean` method per discovered feature type. The `FactoryBean.getObject()` calls `FeatureDispatcher.resolve(featureType)`.

- **Pro**: Well-known Spring pattern, clear semantics
- **Pro**: `getObjectType()` returns the feature interface type, enabling proper autowiring
- **Con**: Cannot register `@Bean` methods dynamically -- the number of features is unknown at compile time
- **Con**: Would require a `BeanDefinitionRegistryPostProcessor` or registrar anyway to create the `FactoryBean` instances dynamically, combining two mechanisms
- **Con**: `FactoryBean` adds an extra layer of indirection (factory bean vs. the actual bean) that complicates debugging
- **Rejected because**: Cannot solve the dynamic discovery problem alone; requires combining with a registrar, negating any simplicity benefit

### Alternative C: Programmatic `GenericApplicationContext.registerBean()`

Use a `ApplicationContextInitializer` or `BeanFactoryPostProcessor` to call `context.registerBean()` directly.

- **Pro**: Most direct API
- **Con**: `ApplicationContextInitializer` runs before auto-configuration; `ServiceLoader` results are available but `FeatureDispatcher` is not yet a bean
- **Con**: Does not participate in `@AutoConfiguration` conditional processing
- **Con**: Non-standard for Spring Boot starters; surprising to Spring developers
- **Rejected because**: Bypasses auto-configuration lifecycle, does not respect conditional patterns

## Consequences

### Positive

- `ImportBeanDefinitionRegistrar` is the standard Spring mechanism for dynamic bean registration from `@Configuration` classes
- Tied to `FlagZenAutoConfiguration` via `@Import`, so it only runs when auto-configuration is active
- Bean types are correctly set, enabling `@Autowired` by type
- Lazy initialization avoids startup ordering issues
- Clean separation: `FlagZenAutoConfiguration` handles `FlagProvider`/`FeatureDispatcher`, `FeatureProxyRegistrar` handles per-feature beans

### Negative

- `ImportBeanDefinitionRegistrar.registerBeanDefinitions()` does not have direct access to the `ApplicationContext` or other beans -- bean resolution must be deferred to the supplier lambda
- `ServiceLoader` in the registrar runs during bean definition registration (early in context lifecycle) -- ClassLoader must have `FeatureMetadata` implementations available at that point (standard for compiled annotation processor output)
- Individual feature proxy beans are not individually guarded by `@ConditionalOnMissingBean` -- if a developer manually registers a bean of the same type, Spring will fail with duplicate bean error. This is acceptable for v1.1.0 and matches behavior of other Spring Boot starters
