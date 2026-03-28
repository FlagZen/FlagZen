# Journey: OpenFeature Adapter Integration

## Persona

**Ricardo Alves** -- senior Java developer at a mid-size fintech. His team uses OpenFeature with Flagd for feature flags. He wants FlagZen's polymorphic dispatch (`@Feature`/`@Variant`) without ripping out their existing OpenFeature infrastructure.

## Goal

Add `flagzen-openfeature` as a dependency, configure it (or let ServiceLoader auto-discover it), and have FlagZen's generated proxies resolve flag values through OpenFeature.

## Journey Flow

```
[Add Dependency]  -->  [Configure Provider]  -->  [Resolve Flags]  -->  [Resolve with Context]
  Feels: Hopeful       Feels: Confident           Feels: Satisfied      Feels: Empowered
  Sees: build.gradle   Sees: 1-2 lines of code    Sees: dispatch works  Sees: targeting works
```

## Emotional Arc

- **Start**: Hopeful -- "Can I keep my OpenFeature setup and get FlagZen's type safety?"
- **Middle**: Confident -- "Configuration is minimal, it just works with ServiceLoader"
- **End**: Satisfied/Empowered -- "Polymorphic dispatch resolves through my existing Flagd provider, including targeting context"

## Step Details

### Step 1: Add Dependency

Ricardo adds `flagzen-openfeature` to his `build.gradle.kts`:

```
implementation("com.flagzen:flagzen-openfeature:1.1.0")
```

No other dependencies needed -- the module transitively brings `flagzen-core` and `dev.openfeature:sdk`.

### Step 2: Configure Provider

**Path A (ServiceLoader -- zero config):** Ricardo already has an OpenFeature provider registered globally. The no-arg constructor picks up `OpenFeatureAPI.getInstance().getClient()`. Nothing to configure.

**Path B (Explicit construction):**

```java
Client client = OpenFeatureAPI.getInstance().getClient("my-domain");
FlagProvider provider = OpenFeatureFlagProvider.create(client);
FeatureDispatcher dispatcher = FlagZen.dispatcher(provider);
```

**Path C (Spring Boot):** Ricardo declares `OpenFeatureFlagProvider` as a `@Bean`. FlagZen Spring auto-config picks it up.

### Step 3: Resolve String Flags

Ricardo's `@Feature` interface resolves through OpenFeature:

```java
@Feature("checkout-flow")
interface CheckoutFlow {
    String renderPage();
}

@Variant(of = CheckoutFlow.class, value = "CLASSIC")
class ClassicCheckout implements CheckoutFlow { ... }

@Variant(of = CheckoutFlow.class, value = "EXPRESS")
class ExpressCheckout implements CheckoutFlow { ... }
```

When `dispatcher.feature(CheckoutFlow.class).renderPage()` is called, the proxy calls `OpenFeatureFlagProvider.getString("checkout-flow")`, which delegates to `client.getStringDetails("checkout-flow", "")`.

### Step 4: Resolve with Evaluation Context

Ricardo passes targeting context for per-user flag resolution:

```java
EvaluationContext ctx = EvaluationContext.builder()
    .targetingKey("user-7291")
    .attribute("plan", "enterprise")
    .build();
```

The adapter maps this to OpenFeature's `EvaluationContext` and passes it to `client.getStringDetails(key, default, ofContext)`.

## Error Paths

| Error | What Ricardo Sees | Recovery |
|-------|-------------------|----------|
| No OpenFeature provider registered | `getString` returns `Optional.empty()` for all keys; FlagZen fallback strategy activates (EXCEPTION/NOOP/DEFAULT) | Register an OpenFeature provider (`OpenFeatureAPI.getInstance().setProviderAndWait(...)`) |
| OpenFeature returns ERROR reason | `getString` returns `Optional.empty()` | Check OpenFeature provider logs; flag key may not exist upstream |
| Attribute type not convertible to OpenFeature `Value` | Adapter logs warning, skips unconvertible attribute | Use supported types: String, Boolean, Integer, Long, Double, List, Map |
