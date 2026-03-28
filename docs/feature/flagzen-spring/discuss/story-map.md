# Story Map: flagzen-spring

## User: Rafael Oliveira (senior backend developer, Spring Boot microservices)

## Goal: Inject @Feature proxies via @Autowired with zero manual wiring

## Backbone

| Add Dependency | Configure Provider | Auto-Configure Dispatcher | Register Feature Beans | Use in Application |
|---|---|---|---|---|
| Add flagzen-spring to build | Define FlagProvider @Bean | Auto-create FeatureDispatcher bean | Register proxy bean per @Feature | @Autowired injection in services |
| Transitive flagzen-core | InMemoryFlagProvider fallback | @ConditionalOnMissingBean guard | ServiceLoader FeatureMetadata discovery | Dynamic dispatch through proxy |
| | @Primary for multiple providers | Startup logging of config | Startup logging of discovered features | |

---

### Walking Skeleton

Thinnest end-to-end slice connecting all activities:

1. **Add Dependency** -- Add flagzen-spring to build.gradle.kts
2. **Configure Provider** -- Define a single FlagProvider @Bean
3. **Auto-Configure Dispatcher** -- FlagZenAutoConfiguration creates FeatureDispatcher bean using the FlagProvider
4. **Register Feature Beans** -- For each FeatureMetadata on classpath, register a proxy bean
5. **Use in Application** -- @Autowired CheckoutFlow into a service, call a method, correct variant executes

### Release 1 (Walking Skeleton): "Feature proxies injectable via @Autowired"

| Story | Activity | Priority |
|---|---|---|
| Auto-configure FeatureDispatcher from FlagProvider bean | Auto-Configure Dispatcher | P1 |
| Register feature proxy beans from FeatureMetadata | Register Feature Beans | P1 |
| FlagProvider bean detection from ApplicationContext | Configure Provider | P1 |

### Release 2: "Safe defaults and diagnostics"

| Story | Activity | Priority |
|---|---|---|
| InMemoryFlagProvider fallback with warning | Configure Provider | P2 |
| @ConditionalOnMissingBean for FeatureDispatcher | Auto-Configure Dispatcher | P2 |
| Startup logging of discovered features and provider | Auto-Configure Dispatcher | P2 |

### Release 3 (Deferred -- not v1.1.0): "Spring-managed variant instances"

| Story | Activity | Priority |
|---|---|---|
| Resolve variant instances from ApplicationContext | Register Feature Beans | P3 |
| @Autowired inside @Variant classes | Use in Application | P3 |

## Scope Assessment: PASS -- 6 stories (R1+R2), 1 bounded context (Spring DI integration), estimated 4-5 days
