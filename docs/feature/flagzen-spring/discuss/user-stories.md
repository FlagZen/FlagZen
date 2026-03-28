<!-- markdownlint-disable MD024 -->

# User Stories: flagzen-spring (M4)

---

## US-SPRING-01: Auto-Configure FeatureDispatcher from FlagProvider Bean

### Problem

Rafael Oliveira is a senior backend developer at a fintech startup who maintains Spring Boot microservices. He finds it tedious to manually create a `FeatureDispatcher` via `FlagZen.dispatcher()` in every service, wire it into Spring's context, and ensure it uses the right `FlagProvider`. He wants the dispatcher to exist automatically when a `FlagProvider` bean is available.

### Who

- Spring Boot developer | Production microservice | Wants zero-boilerplate feature flag setup

### Solution

`FlagZenAutoConfiguration` detects a `FlagProvider` bean in the `ApplicationContext` and creates a `FeatureDispatcher` bean from it. Registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### Domain Examples

#### 1: Happy Path -- Rafael's payment service

Rafael defines a `LaunchDarklyFlagProvider` `@Bean` in his payment service. On startup, `FlagZenAutoConfiguration` detects it, creates a `DefaultFeatureDispatcher` with that provider, and registers it as a singleton bean. Rafael can now `@Autowired FeatureDispatcher` anywhere.

#### 2: Edge Case -- Rafael already has a custom FeatureDispatcher

Rafael's checkout service has a custom `FeatureDispatcher` `@Bean` with special context accessor configuration. `FlagZenAutoConfiguration` sees the existing `FeatureDispatcher` bean and backs off (`@ConditionalOnMissingBean`). Rafael's custom dispatcher is used.

#### 3: Error -- No FlagProvider bean defined

Rafael's new service has `flagzen-spring` on the classpath but no `FlagProvider` `@Bean`. Auto-configuration creates an `InMemoryFlagProvider` as fallback and logs a warning: "No FlagProvider bean found. Using InMemoryFlagProvider (dev/test only)."

### UAT Scenarios (BDD)

#### Scenario: FeatureDispatcher auto-configured from FlagProvider bean

```gherkin
Given Rafael's Spring Boot application defines a FlagProvider @Bean "launchDarklyProvider"
When the application context starts
Then a FeatureDispatcher bean exists in the context
And the FeatureDispatcher uses the "launchDarklyProvider" FlagProvider
```

#### Scenario: Auto-configuration backs off when FeatureDispatcher already exists

```gherkin
Given Rafael's application defines a custom FeatureDispatcher @Bean
And a FlagProvider @Bean also exists
When the application context starts
Then only Rafael's custom FeatureDispatcher bean exists
And FlagZenAutoConfiguration does not create a second dispatcher
```

#### Scenario: InMemoryFlagProvider fallback when no provider bean exists

```gherkin
Given Rafael's application has flagzen-spring on the classpath
And no FlagProvider @Bean is defined
When the application context starts
Then an InMemoryFlagProvider bean is created
And a FeatureDispatcher bean is created using the InMemoryFlagProvider
And a WARN-level log message contains "No FlagProvider bean found"
```

#### Scenario: Auto-configuration registered via Spring Boot imports file

```gherkin
Given the flagzen-spring JAR is on the classpath
When Spring Boot scans for auto-configuration classes
Then it discovers FlagZenAutoConfiguration via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### Acceptance Criteria

- [ ] `FlagZenAutoConfiguration` creates `FeatureDispatcher` bean from `FlagProvider` bean in context
- [ ] `@ConditionalOnMissingBean(FeatureDispatcher.class)` prevents duplicate dispatcher creation
- [ ] When no `FlagProvider` bean exists, `InMemoryFlagProvider` is created with WARN log
- [ ] Auto-configuration discovered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### Outcome KPIs

- **Who**: Spring Boot developer
- **Does what**: Gets a working FeatureDispatcher bean without manual `FlagZen.dispatcher()` calls
- **By how much**: 100% automatic -- zero lines of dispatcher wiring code
- **Measured by**: Integration test verifying FeatureDispatcher bean exists when FlagProvider bean present
- **Baseline**: Manual `FlagZen.dispatcher()` call required in every service

### Technical Notes

- Depends on Spring Boot 3.x (`spring-boot-autoconfigure`)
- Uses `@AutoConfiguration` (not `@Configuration`) for proper ordering
- `DefaultFeatureDispatcher` constructor is public -- can be instantiated directly
- Must use `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3.x), not `spring.factories` (deprecated)

---

## US-SPRING-02: Register Feature Proxy Beans from FeatureMetadata

### Problem

Rafael Oliveira builds Spring Boot services where every dependency is injected via `@Autowired`. After auto-configuring `FeatureDispatcher`, he still has to call `dispatcher.resolve(CheckoutFlow.class)` manually and somehow make the result available for injection. He wants `@Feature` proxies to be injectable beans automatically.

### Who

- Spring Boot developer | Service consuming feature flags | Wants @Autowired for feature proxies like any other bean

### Solution

During auto-configuration, discover all `FeatureMetadata` implementations via `ServiceLoader`, and for each one, register a bean definition in the Spring context. The bean is created by calling `FeatureDispatcher.resolve(featureType)`.

### Domain Examples

#### 1: Happy Path -- Single feature interface

Rafael's payment service has `CheckoutFlow` as a `@Feature` interface. The annotation processor generated `CheckoutFlow_FlagZenMetadata`. On startup, auto-configuration discovers this metadata, registers a `CheckoutFlow` bean, and Rafael injects it via `@Autowired CheckoutFlow checkoutFlow` in `PaymentService`.

#### 2: Multiple features -- Two feature interfaces

Rafael's order service has both `CheckoutFlow` and `ShippingMethod` as `@Feature` interfaces. Auto-configuration discovers both metadata classes and registers both proxy beans. Rafael injects them independently into different services.

#### 3: Edge Case -- No FeatureMetadata on classpath

Rafael adds `flagzen-spring` to a service that has no `@Feature` interfaces (or forgot the annotation processor). `ServiceLoader` finds zero `FeatureMetadata` entries. Auto-configuration logs "No @Feature metadata found on classpath. No feature proxy beans registered." and proceeds without error.

### UAT Scenarios (BDD)

#### Scenario: Feature proxy bean registered for discovered FeatureMetadata

```gherkin
Given CheckoutFlow is a @Feature interface with generated CheckoutFlow_FlagZenMetadata
And FlagZenAutoConfiguration has created a FeatureDispatcher bean
When the Spring application context finishes starting
Then a bean of type CheckoutFlow exists in the ApplicationContext
And the bean is the proxy returned by FeatureDispatcher.resolve(CheckoutFlow.class)
```

#### Scenario: Multiple feature proxy beans registered

```gherkin
Given CheckoutFlow and ShippingMethod are @Feature interfaces with generated metadata
And FlagZenAutoConfiguration has created a FeatureDispatcher bean
When the Spring application context finishes starting
Then beans of type CheckoutFlow and ShippingMethod both exist
And each is a distinct proxy instance
```

#### Scenario: Constructor injection of feature proxy

```gherkin
Given a CheckoutFlow proxy bean is registered in the ApplicationContext
When Rafael's PaymentService class declares a constructor parameter of type CheckoutFlow
Then Spring injects the FlagZen dispatch proxy via constructor injection
And PaymentService can call methods on the injected CheckoutFlow
```

#### Scenario: No metadata found -- graceful degradation

```gherkin
Given no FeatureMetadata implementations exist on the classpath
When the Spring application context starts
Then no feature proxy beans are registered
And an INFO-level log message contains "No @Feature metadata found"
And the application starts successfully
```

#### Scenario: Proxy dispatches dynamically after injection

```gherkin
Given Rafael's PaymentService has an injected CheckoutFlow proxy
And the FlagProvider returns "CLASSIC" for key "checkout-flow"
When PaymentService calls process() on the CheckoutFlow proxy
Then ClassicCheckout.process() executes
When the FlagProvider value changes to "EXPRESS"
And PaymentService calls process() again
Then ExpressCheckout.process() executes
```

### Acceptance Criteria

- [ ] For each `FeatureMetadata` discovered via `ServiceLoader`, a bean of the feature interface type is registered
- [ ] Feature proxy beans are created via `FeatureDispatcher.resolve(featureType)`
- [ ] Feature proxy beans are singleton-scoped (consistent with FeatureDispatcher's proxy cache)
- [ ] When no `FeatureMetadata` found, INFO log emitted, no error, app starts normally
- [ ] Injected proxies dispatch dynamically (flag value changes reflected without restart)

### Outcome KPIs

- **Who**: Spring Boot developer
- **Does what**: Injects @Feature proxies via @Autowired without manual resolve() calls
- **By how much**: 100% of @Feature interfaces with generated metadata become injectable beans
- **Measured by**: Integration test: count of registered feature beans equals count of FeatureMetadata on classpath
- **Baseline**: Manual `dispatcher.resolve()` + `@Bean` method required per feature

### Technical Notes

- `ServiceLoader.load(FeatureMetadata.class)` for discovery (same mechanism as `DefaultFeatureDispatcher`)
- Bean definitions registered via `BeanDefinitionRegistryPostProcessor` or programmatic `GenericApplicationContext` registration
- Feature proxy beans should be lazy-initialized to avoid startup ordering issues
- Depends on US-SPRING-01 (FeatureDispatcher bean must exist first)

---

## US-SPRING-03: FlagProvider Bean Detection from ApplicationContext

### Problem

Rafael Oliveira configures flag providers differently per environment -- LaunchDarkly in production, `InMemoryFlagProvider` in local dev, OpenFeature in staging. He expects Spring to pick up whichever `FlagProvider` `@Bean` he defines, without FlagZen-specific configuration properties or custom annotations.

### Who

- Spring Boot developer | Multi-environment deployment | Wants standard Spring @Bean pattern for provider config

### Solution

`FlagZenAutoConfiguration` takes `FlagProvider` as a constructor parameter (Spring auto-injects it). Standard Spring bean resolution applies -- `@Primary`, `@Qualifier`, profiles all work as expected.

### Domain Examples

#### 1: Happy Path -- LaunchDarkly in production

Rafael defines `@Bean @Profile("prod") FlagProvider launchDarkly()` and `@Bean @Profile("dev") FlagProvider inMemory()`. In production, Spring activates the `prod` profile, LaunchDarkly provider is injected into auto-configuration.

#### 2: Edge Case -- Two providers without @Primary

Rafael accidentally defines two `FlagProvider` beans without `@Primary` or profile separation. Spring raises `NoUniqueBeanDefinitionException` at startup. The error is standard Spring -- FlagZen does not add custom resolution logic.

#### 3: Provider from another starter

Rafael uses `flagzen-openfeature` which registers its own `FlagProvider` `@Bean` via auto-configuration. `FlagZenAutoConfiguration` detects it and uses it. Rafael writes zero provider configuration.

### UAT Scenarios (BDD)

#### Scenario: FlagProvider detected via standard Spring bean resolution

```gherkin
Given Rafael defines a FlagProvider @Bean named "launchDarklyProvider"
When FlagZenAutoConfiguration is processed
Then it receives the "launchDarklyProvider" bean as its FlagProvider dependency
And uses it to create the FeatureDispatcher
```

#### Scenario: Profile-specific FlagProvider selection

```gherkin
Given Rafael defines @Bean @Profile("prod") FlagProvider returning LaunchDarklyFlagProvider
And @Bean @Profile("dev") FlagProvider returning InMemoryFlagProvider
When the application starts with active profile "prod"
Then FlagZenAutoConfiguration uses the LaunchDarklyFlagProvider
```

#### Scenario: FlagProvider from another FlagZen provider module

```gherkin
Given "flagzen-openfeature" is on the classpath
And flagzen-openfeature's auto-configuration registers an OpenFeatureFlagProvider @Bean
When FlagZenAutoConfiguration is processed
Then it uses the OpenFeatureFlagProvider for the FeatureDispatcher
And Rafael has written zero FlagProvider configuration
```

#### Scenario: Ambiguous FlagProvider beans fail clearly

```gherkin
Given Rafael defines two FlagProvider @Bean methods without @Primary
When the application context starts
Then Spring raises NoUniqueBeanDefinitionException
And the error message identifies both conflicting FlagProvider beans
```

### Acceptance Criteria

- [ ] `FlagProvider` injected via standard Spring dependency injection (constructor or parameter)
- [ ] Standard Spring resolution applies: `@Primary`, `@Qualifier`, `@Profile` all work
- [ ] No FlagZen-specific provider resolution logic -- delegate entirely to Spring
- [ ] Ambiguous providers fail with standard Spring error (no custom handling needed)

### Outcome KPIs

- **Who**: Spring Boot developer
- **Does what**: Configures FlagProvider using standard Spring @Bean pattern
- **By how much**: Zero FlagZen-specific configuration properties or annotations needed
- **Measured by**: Integration test with profile-based provider switching
- **Baseline**: Manual `FlagZen.dispatcher(config -> config.provider(...))` per service

### Technical Notes

- `FlagProvider` as constructor parameter of `FlagZenAutoConfiguration` (or `@Bean` method parameter)
- `@ConditionalOnMissingBean(FlagProvider.class)` on fallback `InMemoryFlagProvider` bean
- Auto-configuration ordering: provider module auto-configs should run before `FlagZenAutoConfiguration` (`@AutoConfigureAfter` or `@AutoConfigureBefore`)
- Consider `@AutoConfigureOrder` if multiple FlagZen modules are on classpath

---

## US-SPRING-04: InMemoryFlagProvider Fallback with Warning

### Problem

Priya Sharma is a junior developer onboarding onto Rafael's team. She clones the repo, adds `flagzen-spring` to her new service, but has not configured a `FlagProvider` yet. She wants the app to start so she can develop iteratively, but needs a clear signal that she is running with a dev-only provider.

### Who

- Junior Spring Boot developer | Local development | Wants app to start without full provider config

### Solution

When no `FlagProvider` bean exists, auto-configuration creates an `InMemoryFlagProvider` and logs a WARN message. The app starts, features dispatch to default variants, and Priya sees the warning.

### Domain Examples

#### 1: Happy Path -- New service, no provider yet

Priya creates a new Spring Boot service with `flagzen-spring` on the classpath. She has not defined any `FlagProvider` `@Bean`. On startup, she sees: `WARN  c.f.s.FlagZenAutoConfiguration : No FlagProvider bean found. Using InMemoryFlagProvider (dev/test only).` Her `@Feature` proxies dispatch to `@DefaultVariant` implementations.

#### 2: Edge Case -- InMemoryFlagProvider with pre-set values

Priya uses `application.yml` to set flag values for local dev: `flagzen.flags.checkout-flow: EXPRESS`. The InMemoryFlagProvider reads these properties. (Note: this is a possible enhancement, not required for v1.1.0.)

#### 3: Boundary -- FlagProvider bean exists, no fallback needed

Rafael's production service has a LaunchDarkly `FlagProvider` `@Bean`. The `@ConditionalOnMissingBean` check finds it, so no `InMemoryFlagProvider` is created and no warning is logged.

### UAT Scenarios (BDD)

#### Scenario: InMemoryFlagProvider created when no provider bean exists

```gherkin
Given Priya's Spring Boot application has flagzen-spring on the classpath
And no FlagProvider @Bean is defined in any configuration class
When the application context starts
Then an InMemoryFlagProvider bean is registered in the context
And a FeatureDispatcher bean is created using the InMemoryFlagProvider
```

#### Scenario: Warning logged for fallback provider

```gherkin
Given no FlagProvider @Bean is defined
When the application context starts and InMemoryFlagProvider is created
Then a WARN-level log message is emitted by FlagZenAutoConfiguration
And the message contains "No FlagProvider bean found"
And the message contains "InMemoryFlagProvider"
And the message contains "dev/test only"
```

#### Scenario: No fallback when explicit provider exists

```gherkin
Given Rafael defines a FlagProvider @Bean returning LaunchDarklyFlagProvider
When the application context starts
Then no InMemoryFlagProvider bean is created
And no "No FlagProvider bean found" warning is logged
```

### Acceptance Criteria

- [ ] `InMemoryFlagProvider` `@Bean` created with `@ConditionalOnMissingBean(FlagProvider.class)`
- [ ] WARN log emitted when fallback provider created
- [ ] Warning message is actionable: names the fallback and suggests defining a provider bean
- [ ] Fallback provider does not activate when any `FlagProvider` bean exists

### Outcome KPIs

- **Who**: Developer without provider configuration
- **Does what**: Starts application and develops iteratively without provider setup
- **By how much**: Zero-config startup works on first attempt
- **Measured by**: Integration test: app starts with only flagzen-spring, no explicit provider
- **Baseline**: Application fails to start without explicit FlagProvider configuration

### Technical Notes

- `@ConditionalOnMissingBean(FlagProvider.class)` on the `InMemoryFlagProvider` `@Bean` method
- `InMemoryFlagProvider` is in `com.flagzen.internal` (package-private constructor). Auto-config may need to use `InMemoryFlagProvider.create()` factory or the class needs to be accessible.
- Reading flag values from `application.yml` properties is a possible enhancement for R2/R3

---

## US-SPRING-05: ConditionalOnMissingBean Guards for Safe Composition

### Problem

Rafael Oliveira sometimes needs custom `FeatureDispatcher` configuration -- for example, with specific `ContextAccessor` instances or a custom default `EvaluationContext`. He does not want auto-configuration to fight his custom beans. He expects the standard Spring Boot "back-off" pattern.

### Who

- Senior Spring Boot developer | Custom FeatureDispatcher config | Wants auto-config to back off when overridden

### Solution

All beans created by `FlagZenAutoConfiguration` are guarded with `@ConditionalOnMissingBean`. If the developer defines their own bean of the same type, auto-configuration backs off for that specific bean.

### Domain Examples

#### 1: Happy Path -- Custom FeatureDispatcher

Rafael defines a `@Bean FeatureDispatcher` with a custom `ContextAccessor`. `FlagZenAutoConfiguration` sees the existing `FeatureDispatcher` and does not create another. Feature proxy beans still register using Rafael's custom dispatcher.

#### 2: Custom FlagProvider only

Rafael defines a custom `FlagProvider` but no custom `FeatureDispatcher`. Auto-configuration creates the `FeatureDispatcher` using Rafael's provider. The `InMemoryFlagProvider` fallback does not activate.

#### 3: Edge Case -- Everything custom

Rafael defines custom `FlagProvider`, custom `FeatureDispatcher`, and registers feature proxy beans manually. Auto-configuration backs off entirely. No duplicate beans.

### UAT Scenarios (BDD)

#### Scenario: Auto-config backs off for custom FeatureDispatcher

```gherkin
Given Rafael defines a custom FeatureDispatcher @Bean with a ContextAccessor
And a FlagProvider @Bean also exists
When the application context starts
Then only one FeatureDispatcher bean exists (Rafael's custom one)
And FlagZenAutoConfiguration did not create a dispatcher bean
And feature proxy beans are registered using Rafael's custom dispatcher
```

#### Scenario: Auto-config backs off for custom FlagProvider

```gherkin
Given Rafael defines a custom FlagProvider @Bean
When the application context starts
Then no InMemoryFlagProvider fallback bean is created
And the FeatureDispatcher bean uses Rafael's custom FlagProvider
```

#### Scenario: Full override -- all custom beans

```gherkin
Given Rafael defines custom FlagProvider, FeatureDispatcher, and feature proxy @Bean methods
When the application context starts
Then auto-configuration creates no beans
And Rafael's custom beans are used throughout
```

### Acceptance Criteria

- [ ] `@ConditionalOnMissingBean(FeatureDispatcher.class)` on dispatcher bean creation
- [ ] `@ConditionalOnMissingBean(FlagProvider.class)` on InMemoryFlagProvider fallback
- [ ] Feature proxy registration uses whatever FeatureDispatcher is in context (custom or auto-configured)
- [ ] No bean conflicts when developer overrides any or all auto-configured beans

### Outcome KPIs

- **Who**: Senior Spring Boot developer with custom requirements
- **Does what**: Overrides auto-configured beans using standard Spring patterns
- **By how much**: 100% of auto-configured beans respect @ConditionalOnMissingBean
- **Measured by**: Integration tests with custom bean overrides
- **Baseline**: N/A (module does not exist)

### Technical Notes

- Standard Spring Boot starter pattern
- Test with `@SpringBootTest` + `@TestConfiguration` providing overrides
- Ensure `@ConditionalOnMissingBean` checks are on the `@Bean` methods, not the `@Configuration` class

---

## US-SPRING-06: Startup Diagnostics Logging

### Problem

When something goes wrong with FlagZen auto-configuration, Rafael and Priya waste time debugging because there is no visibility into what FlagZen detected at startup. They want to see a summary of what was auto-configured.

### Who

- Spring Boot developer | Debugging/verifying setup | Wants visibility into auto-configuration results

### Solution

`FlagZenAutoConfiguration` logs an INFO-level summary at startup: which `FlagProvider` was used, how many `FeatureMetadata` entries were discovered, and the names of registered feature proxy beans.

### Domain Examples

#### 1: Happy Path -- Everything configured

Rafael starts his payment service. At INFO level he sees:
`INFO  c.f.s.FlagZenAutoConfiguration : FlagZen auto-configured: provider=LaunchDarklyFlagProvider, features=[CheckoutFlow, ShippingMethod] (2 feature proxies registered)`

#### 2: No features found

Priya starts a service with no `@Feature` interfaces. She sees:
`INFO  c.f.s.FlagZenAutoConfiguration : FlagZen auto-configured: provider=InMemoryFlagProvider, features=[] (0 feature proxies registered)`

#### 3: DEBUG-level detail

At DEBUG level, Rafael sees individual bean registration events:
`DEBUG c.f.s.FlagZenAutoConfiguration : Registering feature proxy bean: CheckoutFlow (flag-key=checkout-flow)`

### UAT Scenarios (BDD)

#### Scenario: INFO summary logged at startup

```gherkin
Given Rafael's application has a FlagProvider and two @Feature interfaces
When the application context starts
Then an INFO log message from FlagZenAutoConfiguration contains the provider class name
And the message lists the feature interface names
And the message includes the count of registered proxy beans
```

#### Scenario: DEBUG detail for individual feature registration

```gherkin
Given Rafael's application has CheckoutFlow as a @Feature interface
And logging for "com.flagzen.spring" is set to DEBUG
When the application context starts
Then a DEBUG log message contains "Registering feature proxy bean: CheckoutFlow"
And the message includes the flag key "checkout-flow"
```

#### Scenario: Zero features logged clearly

```gherkin
Given no FeatureMetadata exists on the classpath
When the application context starts
Then an INFO log message contains "0 feature proxies registered"
```

### Acceptance Criteria

- [ ] INFO-level startup summary includes: provider class name, feature names, proxy count
- [ ] DEBUG-level logs individual feature proxy registration with flag key
- [ ] Zero features scenario logs clearly (not silently)
- [ ] Log messages use SLF4J via standard Spring logging

### Outcome KPIs

- **Who**: Developer debugging FlagZen integration
- **Does what**: Identifies misconfiguration from startup logs without stepping through code
- **By how much**: Misconfiguration diagnosed from logs alone (no debugger needed)
- **Measured by**: INFO log contains all three data points (provider, features, count)
- **Baseline**: Zero visibility into auto-configuration results

### Technical Notes

- Use SLF4J `Logger` (standard Spring Boot logging)
- Keep INFO output to a single summary line
- DEBUG for per-feature detail
- No TRACE level needed for v1.1.0
