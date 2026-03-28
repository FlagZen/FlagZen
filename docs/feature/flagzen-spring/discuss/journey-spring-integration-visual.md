# Journey: Spring Boot Integration

## Persona

**Rafael Oliveira** -- senior backend developer at a fintech startup. Has been using FlagZen's core module for a CLI tool. Now integrating feature flags into the company's Spring Boot microservices. Expects the same zero-boilerplate experience he gets from other Spring Boot starters.

## Goal

Rafael adds `flagzen-spring` to his Spring Boot app and immediately injects `@Feature` proxies via `@Autowired` -- no manual `FlagZen.dispatcher()` calls, no factory wiring.

## Emotional Arc

- **Start**: Cautious optimism -- "Will this Just Work like other starters?"
- **Middle**: Growing confidence -- auto-config detected, proxies injected, flags dispatching
- **End**: Satisfaction -- "This is exactly how a Spring starter should behave"

## Journey Flow

```
[Add dependency]     [Configure provider]    [Inject features]     [Verify dispatch]
      |                     |                       |                      |
      v                     v                       v                      v
  build.gradle         application.yml         @Autowired             Run app
  + flagzen-spring     or @Bean FlagProvider   CheckoutFlow flow     flag changes
                                                                     -> proxy dispatches
      |                     |                       |                      |
  Feels: hopeful       Feels: familiar         Feels: "it works!"    Feels: satisfied
  Sees: dependency     Sees: standard           Sees: proxy injected  Sees: correct
       resolves            Spring config             no errors             variant
```

## Step Details

### Step 1: Add Dependency

Rafael adds `flagzen-spring` to `build.gradle.kts`:

```
dependencies {
    implementation("com.flagzen:flagzen-spring:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

**Emotional state**: Hopeful. Expects transitive dependency on `flagzen-core` to be handled.

### Step 2: Configure FlagProvider

Two paths:

**Path A -- Explicit bean** (most common in production):

```java
@Configuration
public class FlagConfig {
    @Bean
    public FlagProvider flagProvider() {
        return new EnvironmentVariableFlagProvider();
    }
}
```

**Path B -- No explicit bean** (dev/test):

Auto-configuration provides `InMemoryFlagProvider` when no `FlagProvider` bean exists, with a startup warning: "No FlagProvider bean found. Using InMemoryFlagProvider (dev/test only)."

**Emotional state**: Familiar. Standard Spring `@Bean` pattern. No FlagZen-specific ceremony.

### Step 3: Inject Feature Proxies

```java
@Service
public class CheckoutService {
    private final CheckoutFlow checkoutFlow;

    public CheckoutService(@Autowired CheckoutFlow checkoutFlow) {
        this.checkoutFlow = checkoutFlow;
    }
}
```

**Emotional state**: Delight -- "It just works." No `FeatureDispatcher.resolve()` calls needed. The proxy is a first-class Spring bean.

### Step 4: Verify Runtime Dispatch

Rafael changes the flag value in his provider. The proxy dispatches to the new variant on the next method call. No restart needed.

**Emotional state**: Satisfied. Dynamic dispatch works the same as standalone FlagZen, but with Spring DI convenience.

## Error Paths

### E1: Missing FlagProvider bean + no fallback

If `@ConditionalOnMissingBean` fallback is disabled (future config option), app fails to start with clear error:

```
***************************
APPLICATION FAILED TO START
***************************
Description:
No FlagProvider bean found in ApplicationContext.

Action:
Define a FlagProvider @Bean or add a FlagZen provider module
(flagzen-env, etc.) to your classpath.
```

### E2: No @Feature interfaces on classpath

Auto-configuration starts but registers zero feature beans. `FeatureDispatcher` bean exists but resolves nothing. No error -- this is valid (library on classpath, no features defined yet).

### E3: Duplicate FlagProvider beans

Multiple `FlagProvider` beans exist. Auto-configuration requires `@Primary` or fails with standard Spring ambiguity error. FlagZen does not invent its own resolution -- it uses Spring's.

### E4: Feature interface not processed

Developer forgot `annotationProcessor(...)` in build config. No `FeatureMetadata` on classpath. `FeatureDispatcher` bean exists but `@Autowired CheckoutFlow` fails with `NoSuchBeanDefinitionException`. Error is a standard Spring error -- but could be confusing. Consider a startup check that logs discovered features.
