# FlagZen

A feature flagging library for Java introducing polymorphic dispatch and an
abstraction over existing flagging libraries.

## Goals

- Multiple flag providers (Launch Darkly, Togglz, OpenFeature, env vars, ...) as
  "backends"
- Regular conditional-based flagging with a unified syntax hiding the actual
  flagging implementation behind

  ```java
  FlagProvider flags = ...
  if (flags.getBoolean("FEATURE_X_ENABLED")) {
    // do feature X
  }
  ```

- Annotation-based configuration for polymorphic dispatch:
  
  ```java
  @Feature("dark-mode")
  interface DarkMode { void apply(UI ui); }

  @Variant("on")
  class DarkModeOn implements DarkMode { ... }

  @Variant("off")
  class DarkModeOff implements DarkMode { ... }

  class Ui {
    void configureTheme() {
      // depending the value of `dark-mode` flag, DarkModeOn or DarkModeOff
      // instance is returned
      DarkMode darkMode = FeatureDispatcher.resolve(DarkMode.class);
    }
  }
  ```

- Polymorphic dispatch is proxy-based to support flag value changes at runtime
  - So it doesn't move the conditionals to compile time. They will be present
    at runtime, just hidden, so developers don't have to write them.
- Type-safe flag values for polymorphic dispatch on non-string types

  ```java
  // Feature declares expected flag type
  @Feature(value = "max-retries", type = FeatureType.INT)
  interface RetryStrategy { void execute(Request req); }

  @Variant(intValue = 3)
  class ConservativeRetry implements RetryStrategy { ... }

  @Variant(intValue = 10)
  class AggressiveRetry implements RetryStrategy { ... }

  // Boolean feature (exactly 2 variants)
  @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
  interface DarkMode { void apply(UI ui); }

  @Variant(booleanValue = true)  // or @WhenTrue
  class DarkModeOn implements DarkMode { ... }

  @Variant(booleanValue = false)  // or @WhenFalse
  class DarkModeOff implements DarkMode { ... }

  // Convenience annotations for boolean features
  @WhenTrue
  class DarkModeOn implements DarkMode { ... }

  @WhenFalse
  class DarkModeOff implements DarkMode { ... }

  // Multi-feature boolean (with of= argument)
  @WhenTrue(of = DarkMode.class)
  @WhenFalse(of = MaintenanceMode.class)
  class DarkOnMaintenanceOff implements DarkMode, MaintenanceMode { ... }
  ```

  // Long and double dispatch
  @Feature(value = "rate-limit", type = FeatureType.LONG)
  interface RateLimiter { long maxRequests(); }

  @Variant(longValue = 1000)
  class StandardLimit implements RateLimiter { ... }

  // Double dispatch with approximate matching (@CloseTo handles
  // floating-point imprecision from JS backends, etc.)
  @Feature(value = "sampling-ratio", type = FeatureType.DOUBLE)
  interface SamplingStrategy { void sample(Event event); }

  @Variant(doubleValue = @CloseTo(value = 0.1))              // default delta = 1e-10
  @Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01)) // explicit delta
  ```

  Supported types: STRING (default), INT, LONG, BOOLEAN, DOUBLE.
  The processor validates type consistency: all @Variant annotations for a
  feature must use the matching attribute. FlagProvider returns `OptionalInt`,
  `OptionalLong`, `OptionalDouble` for primitive types (avoiding boxing).

- Support for enum validation (if present)

  ```java
  @Feature("checkout-flow")
  interface CheckoutFlow {
      enum Variant { CLASSIC, STREAMLINED, PREMIUM }
      void execute(Cart cart);
  }

  @Variant("CLASSIC")  // processor validates against CheckoutFlow.Variant enum
  class ClassicCheckout implements CheckoutFlow { ... }

  @Variant("TURBO")    // COMPILE ERROR: not in CheckoutFlow.Variant
  class TurboCheckout implements CheckoutFlow { ... }
  ```
  
- Support for multiple `@Feature` implementations per class

  ```java
  // --- Multi-feature (class reference required) ---
  @Variant(of = CheckoutFlow.class, value = "PREMIUM")
  @Variant(of = PaymentMethod.class, value = "CREDIT_CARD")
  class PremiumCreditCheckout implements CheckoutFlow, PaymentMethod { ... }
  ```

- Configurable default behavior if no matching flag is found:

  ```java
  enum FallbackStrategy {
    REQUIRED,   // When enum or boolean: compilation fails if not all values
                // have a @Variant. When string: compilation fails if no
                // @DefaultVariant.

    EXCEPTION,  // throw an exception

    NOOP        // inject an empty, noop implementation (empty for void methods,
                // return sane defaults for others: false, 0, null,
                // Optional.empty(), List.of(), ...)
  }

  @Feature(value = "theme", fallback = FallbackStrategy.REQUIRED)
  interface Theme {
    void apply();
  }

  @Variant("fancy")
  class FancyTheme implements Theme { ... }

  @Variant("dark")
  class DarkTheme implements Theme { ... }

  @DefaultVariant
  // gets used when the `theme` flag is neither "fancy" or "dark"
  class DefaultTheme implements Theme { ... }
  ```

- Condition predicates for in-code variant selection without external flag providers

  ```java
  // Predicate-based dispatch: evaluated in order, first match wins
  @Feature("pricing-tier")
  interface PricingStrategy { Money calculate(Order order); }

  @Variant(when = @Condition(on = IsEnterprise.class, order = 1))
  class EnterprisePricing implements PricingStrategy { ... }

  @Variant(when = @Condition(on = IsStartup.class, order = 2))
  class StartupPricing implements PricingStrategy { ... }

  @DefaultVariant
  class StandardPricing implements PricingStrategy { ... }

  // Predicate interface
  class IsEnterprise implements FeaturePredicate {
    public boolean test(EvaluationContext ctx) {
      return "enterprise".equals(ctx.attribute("plan"));
    }
  }
  ```

  Note: when the flag provider supports server-side targeting rules
  (LaunchDarkly, OpenFeature, Togglz), those are the preferred way.
  Condition predicates are for pure in-code feature switching — a
  declarative Strategy pattern selector evaluated against the
  EvaluationContext.
- Compile-time annotation processing
- Spring, CDI, Quarkus, ... integration
  
  ```java
  // Spring example
  @Feature("dark-mode")
  interface DarkMode { void apply(UI ui); }

  @Variant("on")
  @Component
  class DarkModeOn implements DarkMode { ... }

  @Variant("off")
  @Component
  class DarkModeOff implements DarkMode { ... }

  @Component
  class Ui {
    @Autowired
    DarkMode darkMode; // appropriate bean is wired depending on flag value
  }
  ```

- Module to collect statistics about flag usage
  - Finding hotspots
  - Compile-time warnings for unused variants
  - Dead flag detection
  - Information for dashboards (only the information, not the dashboard)
  - ...
- Handle reasonable situations (package private implementations classes, ...)
- SPI-based extensibility
- Type-safety wherever possible
- Support for an evaluation context (example use-case: A/B testing)
- Comprehensive GitHub pages documentation in a submodule
- Excellent unit testing support
  - Follow best coding practices. For example, avoid static methods, follow
    SOLID principles, ...
  - First-class API for pinning flag values in tests
    - Annotation-based config for file-source (properties, JSON, YAML, ...)
    - Annotation-based config for pinning a specific flag value
    - Java config for pinning a specific flag value
    - Clear priority between different sources

    ```java
    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void premiumCheckoutAppliesDiscount() { ... }

    @FlagSource("flags-test.properties")
    class CheckoutFlowTest { ... }

    @Test
    @FlagSource("flags-test.properties") // overrides method-level
    void premiumCheckoutAppliesDiscount() { ... }

    @Test
    void premiumCheckoutAppliesDiscount() {
      // testFlagContext shouldn't be static
      testFlagContext.pin("checkout-flow", "PREMIUM");
    }
    ```

  - Test fixtures
- Reactivity support
  - Core API remains synchronous (flag resolution is a local, non-blocking
    lookup — wrapping in reactive types adds no value)
  - The problem to solve: evaluation context propagation in reactive pipelines
    where thread-local/ScopedValue doesn't work
  - Core module defines a `ContextAccessor` SPI for plugging in custom context
    sources
  - Extension libraries for specific reactive stacks:
    - `flagzen-reactor`: reads `EvaluationContext` from Reactor's
      `Context` (Spring WebFlux)
    - `flagzen-mutiny`: reads `EvaluationContext` from Mutiny's
      `Context` (Quarkus Reactive)
  - Resolution order for evaluation context:
    1. Explicit parameter (always wins)
    2. Reactive context (if reactive extension is on classpath)
    3. ScopedValue / ThreadLocal (block-scoped)
    4. Default context (if configured)
- Thread-safe
- Support for request-scoping and thread-scoping
  - Explicit evaluation context argument (custom, independent of other libs)
  - If no context passed, use what was configured
  - Block-scoped evaluation context (FlagContext.run); ScopedValue when
    available, ThreadLocal as fallback. Framework integrations use their native
    request scoping instead.
- Zero runtime reflection for the core module. The following should happen at
  compile time:
  - Config code generation
  - Proxy generation with conditional code selecting the variants depending on
    the flag value. The conditional in the proxy is executed at runtime.
  - Detecting features and variants
- Modular design for easy extensibility
  - DI framework support (Spring, Quarkus, CDI, ...)
  - Reactive support (Reactor, Mutiny, ...)
  - Flag provider support (Togglz, Launch Darkly, OpenFeature, ...)
  - Hooks (metrics, structured logging, ...)
  - Testing support (JUnit 4, JUnit 5, TestNG, ...)
- Gradle monorepo with all the submodules
- Open-source code
- Java 17+

## Non-Goals

- Introducing another feature flag manager platform
- Handle unreasonable cases (private classes, inner classes, ...)
- Remove reflection from surrounding frameworks and libraries (Spring,
  providers, ...)
- Flag-management UI/dashboard
- Flag targeting rules engine
- Backward compatibility with existing flag provider client APIs
