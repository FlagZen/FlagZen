# Data Models -- flagzen-spring (M4)

## Bean Definitions

flagzen-spring registers the following beans in the Spring `ApplicationContext`. No new data types, DTOs, or entities are introduced.

### FlagProvider Fallback Bean

| Property | Value |
|---|---|
| Bean name | `inMemoryFlagProvider` |
| Bean type | `com.flagzen.internal.InMemoryFlagProvider` (implements `FlagProvider`) |
| Scope | Singleton |
| Conditional | `@ConditionalOnMissingBean(FlagProvider.class)` -- only created when no `FlagProvider` bean exists |
| Construction | `new InMemoryFlagProvider()` (empty, no pre-set flags) |
| Side effect | WARN log: "No FlagProvider bean found. Using InMemoryFlagProvider (dev/test only)." |

### FeatureDispatcher Bean

| Property | Value |
|---|---|
| Bean name | `featureDispatcher` |
| Bean type | `com.flagzen.FeatureDispatcher` (concrete: `DefaultFeatureDispatcher`) |
| Scope | Singleton |
| Conditional | `@ConditionalOnMissingBean(FeatureDispatcher.class)` |
| Construction | `new DefaultFeatureDispatcher(flagProvider)` where `flagProvider` is the resolved `FlagProvider` bean |
| Dependencies | `FlagProvider` bean (user-defined or fallback) |

### Feature Proxy Beans (one per `FeatureMetadata`)

| Property | Value |
|---|---|
| Bean name | Decapitalized simple name of feature interface (e.g., `checkoutFlow`) |
| Bean type | Feature interface `Class<T>` from `FeatureMetadata.featureType()` |
| Scope | Singleton |
| Lazy | Yes |
| Construction | Supplier: `beanFactory.getBean(FeatureDispatcher.class).resolve(featureType)` |
| Discovery | `ServiceLoader.load(FeatureMetadata.class)` |

## Configuration Properties

**None for v1.1.0.** Auto-configuration is fully convention-based. No `flagzen.*` properties are read from `application.yml` or `application.properties`.

Future releases may introduce:

- `flagzen.flags.<key>: <value>` for `InMemoryFlagProvider` pre-population (mentioned in US-SPRING-04 as possible R2/R3 enhancement)
- `flagzen.enabled: false` to disable auto-configuration entirely

## Registration Resource

| File | Content |
|---|---|
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | `com.flagzen.spring.FlagZenAutoConfiguration` |

## Logging Output

### INFO Level (always)

Single summary line at startup:

```
INFO  c.f.s.FlagZenAutoConfiguration : FlagZen auto-configured: provider={ProviderClassName}, features=[{FeatureName1}, {FeatureName2}] ({N} feature proxies registered)
```

### WARN Level (fallback only)

```
WARN  c.f.s.FlagZenAutoConfiguration : No FlagProvider bean found. Using InMemoryFlagProvider (dev/test only). Define a FlagProvider @Bean for production use.
```

### DEBUG Level (per-feature)

```
DEBUG c.f.s.FeatureProxyRegistrar : Registering feature proxy bean: {FeatureName} (flag-key={flag-key})
```

### INFO Level (no metadata)

```
INFO  c.f.s.FeatureProxyRegistrar : No @Feature metadata found on classpath. No feature proxy beans registered.
```
