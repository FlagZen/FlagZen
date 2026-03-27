# Journey: Typed Dispatch -- Visual

## Persona

**Carlos Mendes** -- Senior Java developer at a fintech company. Uses FlagZen for polymorphic dispatch on string-typed features in a payments service. Needs to dispatch on integer, boolean, long, and double flag values without manual parsing. Expects compile-time safety for type mismatches.

**Mei Chen** -- Java developer maintaining a multi-tenant SaaS platform. Uses boolean flags for feature toggles and double flags for sampling ratios. Wants convenience annotations (`@WhenTrue`/`@WhenFalse`) and approximate double matching for JS backend imprecision.

## Developer Experience Arc

```
[Define Feature]  --> [Annotate Variants] --> [Compile]       --> [Dispatch]       --> [Test]
  Feels:                Feels:                 Feels:              Feels:              Feels:
  Intentional           Expressive             Confident           Trusting            Assured
  "I declare the        "Typed attributes      "Processor          "Proxy handles      "Typed values
   type I expect"        match my intent"       catches errors"     type conversion"     round-trip"
```

## Step 1: Define Feature with Type

Carlos declares a feature interface with an explicit type.

```java
// FeatureType enum: STRING (default), INT, LONG, BOOLEAN, DOUBLE

// INT feature
@Feature(value = "max-retries", type = FeatureType.INT)
interface RetryStrategy {
    void execute(Request req);
}

// BOOLEAN feature
@Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
interface DarkMode {
    void apply(UI ui);
}

// LONG feature
@Feature(value = "rate-limit", type = FeatureType.LONG)
interface RateLimiter {
    long maxRequests();
}

// DOUBLE feature
@Feature(value = "sampling-ratio", type = FeatureType.DOUBLE)
interface SamplingStrategy {
    void sample(Event event);
}

// STRING feature (default, backward compatible)
@Feature("checkout-flow")
interface CheckoutFlow {
    void execute(Cart cart);
}
```

**Shared artifacts**: `FeatureType` enum, `@Feature.type` attribute

## Step 2: Annotate Variants with Typed Values

Carlos and Mei annotate implementation classes with typed variant attributes.

```java
// --- INT feature: exact value matching ---
@Variant(intValue = 3)
class ConservativeRetry implements RetryStrategy { /* ... */ }

@Variant(intValue = 10)
class AggressiveRetry implements RetryStrategy { /* ... */ }

// --- BOOLEAN feature: @Variant(booleanValue = ...) ---
@Variant(booleanValue = true)
class DarkModeOn implements DarkMode { /* ... */ }

@Variant(booleanValue = false)
class DarkModeOff implements DarkMode { /* ... */ }

// --- BOOLEAN feature: @WhenTrue / @WhenFalse convenience ---
@WhenTrue
class DarkModeOn implements DarkMode { /* ... */ }

@WhenFalse
class DarkModeOff implements DarkMode { /* ... */ }

// --- BOOLEAN multi-feature: @WhenTrue(of = ...) ---
@WhenTrue(of = DarkMode.class)
@WhenFalse(of = MaintenanceMode.class)
class DarkOnMaintenanceOff implements DarkMode, MaintenanceMode { /* ... */ }

// --- LONG feature: exact value matching ---
@Variant(longValue = 1000)
class StandardLimit implements RateLimiter { /* ... */ }

@Variant(longValue = 50000)
class HighVolumeLimit implements RateLimiter { /* ... */ }

// --- DOUBLE feature: approximate matching via @CloseTo ---
@Variant(doubleValue = @CloseTo(value = 0.1))
class LowSampling implements SamplingStrategy { /* ... */ }

@Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01))
class MediumSampling implements SamplingStrategy { /* ... */ }
```

**Shared artifacts**: `@Variant` typed attributes (`intValue`, `booleanValue`, `longValue`, `doubleValue`), `@WhenTrue`, `@WhenFalse`, `@CloseTo`

## Step 3: Compile -- Processor Validates Type Consistency

The annotation processor catches type mismatches at compile time.

```
# Happy path -- compiles successfully
$ javac -processor com.flagzen.processor.FlagZenProcessor ...
Note: Generated RetryStrategy_FlagZenProxy
Note: Generated DarkMode_FlagZenProxy
Note: Generated RateLimiter_FlagZenProxy
Note: Generated SamplingStrategy_FlagZenProxy

# Error: attribute does not match @Feature type
@Feature(value = "max-retries", type = FeatureType.INT)
interface RetryStrategy { ... }

@Variant(booleanValue = true)  // WRONG: expected intValue for INT feature
class BadVariant implements RetryStrategy { ... }

error: [FlagZen] @Variant attribute 'booleanValue' does not match
       @Feature type INT on RetryStrategy. Use 'intValue' instead.

# Error: duplicate typed value
@Variant(intValue = 3)
class RetryA implements RetryStrategy { ... }
@Variant(intValue = 3)
class RetryB implements RetryStrategy { ... }

error: [FlagZen] Duplicate intValue 3 on feature 'max-retries'.
       RetryA and RetryB both claim intValue = 3.

# Error: incomplete BOOLEAN coverage with REQUIRED strategy
@Feature(value = "dark-mode", type = FeatureType.BOOLEAN,
         fallback = FallbackStrategy.REQUIRED)
interface DarkMode { ... }

@WhenTrue
class DarkModeOn implements DarkMode { ... }
// Missing @WhenFalse or @DefaultVariant

error: [FlagZen] REQUIRED strategy on BOOLEAN feature 'dark-mode'
       requires variants for both true and false, or a @DefaultVariant.
```

## Step 4: Runtime Typed Dispatch

Generated proxies dispatch on typed flag values at runtime. Each type has a specific dispatch strategy.

```
INT dispatch (map lookup):
  flagProvider.getInt("max-retries") --> OptionalInt.of(3)
  proxy: Map<Integer, Supplier<T>>.get(3) --> ConservativeRetry

LONG dispatch (map lookup):
  flagProvider.getLong("rate-limit") --> OptionalLong.of(1000)
  proxy: Map<Long, Supplier<T>>.get(1000L) --> StandardLimit

BOOLEAN dispatch (map lookup):
  flagProvider.getBoolean("dark-mode") --> Optional<Boolean>.of(true)
  proxy: Map<Boolean, Supplier<T>>.get(true) --> DarkModeOn

DOUBLE dispatch (iterate + approximate match):
  flagProvider.getDouble("sampling-ratio") --> OptionalDouble.of(0.0999999999)
  proxy iterates variants:
    Math.abs(0.0999999999 - 0.1) = 9.99e-11 <= 1e-10 (default delta) --> MATCH
  --> LowSampling
```

**Dynamic dispatch**: proxy re-evaluates on every method call, so flag value changes are reflected immediately.

## Step 5: Typed Dispatch with Evaluation Context

Typed dispatch integrates with the existing M1 evaluation context resolution chain. All context resolution patterns work identically for typed features.

```java
// --- Explicit EvaluationContext ---
EvaluationContext ctx = EvaluationContext.builder()
    .targetingKey("user-42")
    .attribute("plan", "premium")
    .build();

// INT dispatch with explicit context
RetryStrategy retry = dispatcher.resolve(RetryStrategy.class, ctx);
// proxy calls flagProvider.getInt("max-retries", ctx)

// BOOLEAN dispatch with explicit context
DarkMode mode = dispatcher.resolve(DarkMode.class, ctx);
// proxy calls flagProvider.getBoolean("dark-mode", ctx)

// --- Block-scoped context ---
FlagContext.run(ctx, () -> {
    RetryStrategy retry = dispatcher.resolve(RetryStrategy.class);
    // proxy calls flagProvider.getInt("max-retries", ctx)

    RateLimiter limiter = dispatcher.resolve(RateLimiter.class);
    // proxy calls flagProvider.getLong("rate-limit", ctx)
});

// --- Explicit overrides scoped ---
FlagContext.run(freeCtx, () -> {
    // explicit context wins over scoped context
    RetryStrategy retry = dispatcher.resolve(RetryStrategy.class, premiumCtx);
    // proxy calls flagProvider.getInt("max-retries", premiumCtx)
});

// --- Context accessor (reactive) ---
// ContextAccessor provides context from Reactor/Mutiny pipeline
// proxy calls flagProvider.getInt("max-retries", accessorCtx)
```

## Step 6: Conditional API (Non-Polymorphic)

Carlos uses typed accessors directly for simple conditional checks without polymorphic dispatch.

```java
FlagProvider flags = ...;

// Boolean check
if (flags.getBoolean("maintenance-mode").orElse(false)) {
    showMaintenancePage();
}

// Int check
int maxRetries = flags.getInt("max-retries").orElse(5);

// Long check
long rateLimit = flags.getLong("rate-limit").orElse(1000L);

// Double check
double samplingRatio = flags.getDouble("sampling-ratio").orElse(0.1);

// Context-aware conditional check
OptionalInt retries = flags.getInt("max-retries", ctx);
```

**Default methods**: Typed accessors are default methods on `FlagProvider` that parse from `getString()`. Providers override for native typed support (e.g., LaunchDarkly SDK returns integers natively).

## Step 7: Test Typed Dispatch

Carlos pins typed flag values in tests using existing test infrastructure.

```java
@Test
@PinFlag(feature = "max-retries", variant = "3")
void conservativeRetryUsed() {
    RetryStrategy strategy = dispatcher.resolve(RetryStrategy.class);
    // proxy parses "3" via getInt default method --> ConservativeRetry
}

@Test
@PinFlag(feature = "dark-mode", variant = "true")
void darkModeEnabled() {
    DarkMode mode = dispatcher.resolve(DarkMode.class);
    // proxy parses "true" via getBoolean default method --> DarkModeOn
}

@Test
void programmaticPin() {
    testFlagContext.pin("rate-limit", "50000");
    RateLimiter limiter = dispatcher.resolve(RateLimiter.class);
    // proxy parses "50000" via getLong default method --> HighVolumeLimit
}
```

## Integration Points

| From | To | Data | Risk |
| ---- | -- | ---- | ---- |
| `@Feature.type` | Annotation processor | `FeatureType` enum value (STRING, INT, LONG, BOOLEAN, DOUBLE) | LOW -- compile-time validated |
| `@Variant` typed attributes | Annotation processor | `intValue` / `booleanValue` / `longValue` / `doubleValue` | MEDIUM -- type matching rules |
| `@WhenTrue` / `@WhenFalse` | Annotation processor | Treated as `@Variant(booleanValue = true/false)` | LOW -- syntactic sugar |
| `@CloseTo` | Annotation processor + proxy | `value` (double) + `delta` (double, default 1e-10) | MEDIUM -- floating-point semantics |
| Annotation processor | Generated proxy | Dispatch strategy selection (map vs iterate) | MEDIUM -- code generation correctness |
| `FlagProvider` typed methods | Generated proxy | `OptionalInt`, `OptionalLong`, `OptionalDouble`, `Optional<Boolean>` | MEDIUM -- parsing from string fallback |
| Evaluation context | Typed dispatch | Context passed to typed `FlagProvider` methods | LOW -- extends existing M1 chain |
| `@PinFlag` / `TestFlagContext` | Typed proxies | String values parsed by FlagProvider default methods | LOW -- existing infrastructure |
