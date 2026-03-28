# How to Read Flags from Environment Variables

Use environment variables as your flag value source with zero configuration.

## Goal

Resolve feature flags from environment variables at runtime without manual setup.

## Prerequisites

- FlagZen basics: `@Feature`, `@Variant`, `FeatureDispatcher`

## Steps

### 1. Add the dependency

```gradle
dependencies {
    implementation("com.flagzen:flagzen-env:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

### 2. Create the provider (default configuration)

```java
FlagProvider provider = new EnvironmentVariableFlagProvider();
FeatureDispatcher dispatcher = FeatureDispatcher.withProvider(provider);
CheckoutFlow flow = dispatcher.resolve(CheckoutFlow.class);
```

The default provider:

- Reads from `System.getenv()`
- Expects environment variables with the `FLAGZEN_` prefix
- Parses variable names as SCREAMING_SNAKE_CASE
- Outputs flag keys as kebab-case

### 3. Set environment variables

Use the naming convention: `FLAGZEN_` + flag key in uppercase with underscores:

```bash
# For flag key "checkout-flow", set:
export FLAGZEN_CHECKOUT_FLOW=STREAMLINED

# For flag key "max-retries", set:
export FLAGZEN_MAX_RETRIES=10
```

The provider automatically maps `FLAGZEN_CHECKOUT_FLOW` → `checkout-flow` → `STREAMLINED`.

### 4. (Optional) Customize the prefix

```java
FlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.screamingSnakeCase("MYAPP_"))
    .formatter(FlagKeyFormats.kebabCase())
    .build();
```

Now the provider reads `MYAPP_CHECKOUT_FLOW` instead of `FLAGZEN_CHECKOUT_FLOW`.

### 5. (Optional) Use a custom parser or formatter

```java
FlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .parser(FlagKeyParsers.camelCase())        // MyAppCheckoutFlow
    .formatter(FlagKeyFormats.screamingSnakeCase("APP"))
    .build();
```

### 6. (Optional) Handle conflicting environment variables

If multiple environment variables map to the same flag key, the provider warns by default. To change behavior:

```java
import com.flagzen.keymapping.ConflictStrategy;

FlagProvider provider = EnvironmentVariableFlagProvider.builder()
    .onConflict(ConflictStrategy.ERROR)  // Throw on conflict
    .warningConsumer(logger::warn)        // Custom warning handler
    .build();
```

## Result

Feature flags are sourced from environment variables with no additional code. Flags evaluated at construction time; changing env vars requires provider restart.

## See Also

- [Reference: EnvironmentVariableFlagProvider](../reference/modules.md) — all builder options
- [Reference: FlagKeyParser & FlagKeyFormat](../reference/key-mapping.md#flagkeyparser) — key conversion
- [How-to: Custom Provider](custom-provider.md) — write your own flag source
- [How-to: Spring Boot Integration](spring-boot.md) — auto-register with Spring
