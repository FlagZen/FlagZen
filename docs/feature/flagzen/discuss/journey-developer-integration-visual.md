# Journey: Java Developer Integrates FlagZen

## Persona

**Marco Pellegrini** -- Senior Java developer at a fintech company. 8 years Java experience. Uses LaunchDarkly for product flags and env vars for infrastructure flags. Frustrated by testing complexity and if/else sprawl. Values type safety and clean architecture. Skeptical of "magic" but appreciates well-designed annotations (loves MapStruct, respects Lombok).

## Emotional Arc

```text
Curious ──> Intrigued ──> Impressed ──> Confident ──> Delighted ──> Evangelical
  |            |              |             |             |              |
  v            v              v             v             v              v
"What's     "Oh, it       "No more      "This        "I can't      "Let me
 this?"     maps to       if/else?"     compiles     go back to     show my
            Strategy                    cleanly"     if/else"       team"
            pattern"
```

## Journey Flow

```text
[DISCOVER]        [ADD DEP]         [DEFINE]          [IMPLEMENT]
 README /         build.gradle      @Feature          @Variant
 blog post        + annotation      interface         classes
    |                 |                 |                  |
    v                 v                 v                  v
 Curious          Cautious          Intrigued         Impressed
 "Is this         "One more         "Clean.           "No if/else.
  real?"           dep..."          Just an           Just types."
                                   interface."

        [RESOLVE]          [TEST]             [INTEGRATE]
        Feature            @PinFlag           Spring /
        Dispatcher         @FlagSource        CDI auto
           |                  |                  |
           v                  v                  v
        Confident          Delighted          Evangelical
        "Proxy             "2 lines           "This is how
         just works"        of setup!"         flags should
                                              work"
```

## Step-by-Step Detail

### Step 1: Discover FlagZen

**Trigger**: Marco reads a blog post titled "Feature Flags Without If/Else in Java" or finds the library via GitHub/Maven search.

**What Marco sees**:

```text
+-- GitHub README (first 30 seconds) -----------------------------------+
|                                                                        |
|  # FlagZen                                                             |
|                                                                        |
|  Type-safe, polymorphic feature flags for Java.                        |
|  Zero runtime reflection. Compile-time validation.                     |
|  Pluggable backends. First-class testing.                              |
|                                                                        |
|  ## Quick Start                                                        |
|                                                                        |
|  // Define a feature as a type                                         |
|  @Feature("checkout-flow")                                             |
|  interface CheckoutFlow {                                              |
|      void execute(Cart cart);                                          |
|  }                                                                     |
|                                                                        |
|  // Implement variants as classes                                      |
|  @Variant("CLASSIC")                                                   |
|  class ClassicCheckout implements CheckoutFlow { ... }                 |
|                                                                        |
|  @Variant("STREAMLINED")                                               |
|  class StreamlinedCheckout implements CheckoutFlow { ... }             |
|                                                                        |
|  // Resolve -- no if/else, no switch                                   |
|  CheckoutFlow flow = FeatureDispatcher.resolve(CheckoutFlow.class);    |
|  flow.execute(cart);                                                   |
|                                                                        |
|  // Test with one annotation                                           |
|  @Test                                                                 |
|  @PinFlag(feature = "checkout-flow", variant = "STREAMLINED")          |
|  void streamlinedCheckoutAppliesDiscount() { ... }                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Curious/Skeptical --> Exit: Intrigued
**Design lever**: Value proposition in <30 seconds. Code speaks louder than prose.

### Step 2: Add Dependency

**Action**: Marco adds FlagZen to his Gradle (or Maven) build.

```text
+-- build.gradle.kts ---------------------------------------------------+
|                                                                        |
|  dependencies {                                                        |
|      implementation("com.flagzen:flagzen-core:${flagzenVersion}")       |
|      annotationProcessor("com.flagzen:flagzen-core:${flagzenVersion}") |
|      testImplementation("com.flagzen:flagzen-test:${flagzenVersion}")  |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Intrigued/Cautious --> Exit: Neutral (low friction)
**Design lever**: Standard dependency declaration. No plugins required. Annotation processor auto-discovered.

### Step 3: Define a @Feature Interface

**Action**: Marco creates his first feature flag as a Java interface.

```text
+-- CheckoutFlow.java --------------------------------------------------+
|                                                                        |
|  @Feature("checkout-flow")                                             |
|  interface CheckoutFlow {                                              |
|      enum Variant { CLASSIC, STREAMLINED, PREMIUM }                    |
|      void execute(Cart cart);                                          |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Neutral --> Exit: Intrigued
**Design lever**: Familiar pattern (interface + enum). The @Feature annotation is the only new concept. The enum is optional but enables compile-time validation.
**What the annotation processor does** (invisible to Marco):

- Registers CheckoutFlow as a feature with flag key "checkout-flow"
- Prepares to validate @Variant classes against the Variant enum
- Will generate a proxy class for dispatch

### Step 4: Implement @Variant Classes

**Action**: Marco implements each variant as a separate class.

```text
+-- ClassicCheckout.java -----------------------------------------------+
|                                                                        |
|  @Variant("CLASSIC")                                                   |
|  class ClassicCheckout implements CheckoutFlow {                       |
|      @Override                                                         |
|      public void execute(Cart cart) {                                  |
|          // standard checkout logic                                    |
|      }                                                                 |
|  }                                                                     |
|                                                                        |
+-- StreamlinedCheckout.java -------------------------------------------+
|                                                                        |
|  @Variant("STREAMLINED")                                               |
|  class StreamlinedCheckout implements CheckoutFlow {                   |
|      @Override                                                         |
|      public void execute(Cart cart) {                                  |
|          // streamlined checkout with fewer steps                      |
|      }                                                                 |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Compile-time validation** (annotation processor output on build):

```
+-- Compiler output (happy path) ---------------------------------------+
|                                                                        |
|  [flagzen] Registered variant CLASSIC for feature checkout-flow        |
|  [flagzen] Registered variant STREAMLINED for feature checkout-flow    |
|  [flagzen] Generated proxy: CheckoutFlow_FlagZenProxy                 |
|                                                                        |
+------------------------------------------------------------------------+
```

**Compile-time validation** (error path -- typo in variant name):

```
+-- Compiler output (error) --------------------------------------------+
|                                                                        |
|  ERROR: @Variant("TURBO") on TurboCheckout does not match any value   |
|         in CheckoutFlow.Variant enum.                                  |
|         Valid values: CLASSIC, STREAMLINED, PREMIUM                    |
|         Location: TurboCheckout.java:3                                 |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Intrigued --> Exit: Impressed
**Design lever**: Compile-time errors catch mistakes before runtime. This is the "Java way" -- the type system has your back.

### Step 5: Resolve via FeatureDispatcher

**Action**: Marco uses the resolved feature in his code.

```
+-- OrderService.java --------------------------------------------------+
|                                                                        |
|  class OrderService {                                                  |
|      private final CheckoutFlow checkoutFlow;                          |
|                                                                        |
|      OrderService(FeatureDispatcher dispatcher) {                      |
|          this.checkoutFlow = dispatcher.resolve(CheckoutFlow.class);   |
|      }                                                                 |
|                                                                        |
|      void processOrder(Cart cart) {                                    |
|          checkoutFlow.execute(cart); // dispatches to active variant   |
|      }                                                                 |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**What happens at runtime**:

- `dispatcher.resolve()` returns the generated proxy
- Proxy consults the configured FlagProvider for the "checkout-flow" flag value
- Proxy delegates to the matching @Variant implementation
- If flag value changes at runtime, the next call dispatches to the new variant

**Emotional state**: Entry: Impressed --> Exit: Confident
**Design lever**: No if/else. No switch. The code reads like normal OOP. The flag decision is invisible.

### Step 6: Test with @PinFlag

**Action**: Marco writes tests for each variant.

```
+-- CheckoutFlowTest.java ---------------------------------------------+
|                                                                        |
|  @ExtendWith(FlagZenExtension.class)                                   |
|  class CheckoutFlowTest {                                              |
|                                                                        |
|      @Test                                                             |
|      @PinFlag(feature = "checkout-flow", variant = "PREMIUM")          |
|      void premiumCheckoutAppliesDiscount(CheckoutFlow flow) {          |
|          Cart cart = new Cart(Item.of("Widget", 100_00));              |
|          flow.execute(cart);                                           |
|          assertThat(cart.total()).isEqualTo(90_00); // 10% discount    |
|      }                                                                 |
|                                                                        |
|      @Test                                                             |
|      @PinFlag(feature = "checkout-flow", variant = "CLASSIC")          |
|      void classicCheckoutNoDiscount(CheckoutFlow flow) {               |
|          Cart cart = new Cart(Item.of("Widget", 100_00));              |
|          flow.execute(cart);                                           |
|          assertThat(cart.total()).isEqualTo(100_00);                   |
|      }                                                                 |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Alternative: programmatic pinning**:

```
+-- Programmatic test --------------------------------------------------+
|                                                                        |
|  @Test                                                                 |
|  void switchVariantMidTest(TestFlagContext flags, CheckoutFlow flow) { |
|      flags.pin("checkout-flow", "CLASSIC");                            |
|      flow.execute(cart);                                               |
|      assertThat(cart.total()).isEqualTo(100_00);                       |
|                                                                        |
|      flags.pin("checkout-flow", "PREMIUM");                            |
|      flow.execute(cart);                                               |
|      assertThat(cart.total()).isEqualTo(90_00);                        |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Confident --> Exit: Delighted
**Design lever**: 2 lines of setup vs. 15-30 with LaunchDarkly. This is the "I can't go back" moment.

### Step 7: Integrate with DI Framework (Spring)

**Action**: Marco adds the Spring Boot starter.

```
+-- build.gradle.kts (addition) ----------------------------------------+
|                                                                        |
|  implementation("com.flagzen:flagzen-spring:${flagzenVersion}")        |
|                                                                        |
+------------------------------------------------------------------------+
```

```
+-- OrderService.java (Spring version) ---------------------------------+
|                                                                        |
|  @Service                                                              |
|  class OrderService {                                                  |
|      @Autowired                                                        |
|      private CheckoutFlow checkoutFlow; // auto-wired proxy            |
|                                                                        |
|      void processOrder(Cart cart) {                                    |
|          checkoutFlow.execute(cart);                                   |
|      }                                                                 |
|  }                                                                     |
|                                                                        |
+------------------------------------------------------------------------+
```

**Emotional state**: Entry: Delighted --> Exit: Evangelical
**Design lever**: Zero configuration. Add dependency, it works. Same pattern as Spring Data repositories or Feign clients. Marco knows this pattern.

## Error Paths

### E1: Missing @Variant for REQUIRED fallback strategy

```
+-- Compiler output (error) --------------------------------------------+
|                                                                        |
|  ERROR: Feature "checkout-flow" uses REQUIRED fallback strategy        |
|         but variant PREMIUM has no @Variant implementation.            |
|         All values in CheckoutFlow.Variant must have implementations.  |
|         Missing: PREMIUM                                               |
|         Location: CheckoutFlow.java:1                                  |
|                                                                        |
+------------------------------------------------------------------------+
```

### E2: No flag provider configured

```
+-- Runtime output (error) ----------------------------------------------+
|                                                                        |
|  FlagZenException: No FlagProvider configured.                         |
|                                                                        |
|  FlagZen needs at least one flag provider to resolve feature flags.    |
|                                                                        |
|  Quick fixes:                                                          |
|    1. Add flagzen-env for environment variable flags:                  |
|       implementation("com.flagzen:flagzen-env:1.1.0")                  |
|    2. Configure a custom provider:                                     |
|       FlagZen.configure(provider -> provider.add(myProvider))          |
|                                                                        |
|  Docs: https://flagzen.com/getting-started#providers                   |
|                                                                        |
+------------------------------------------------------------------------+
```

### E3: Flag value has no matching variant (EXCEPTION strategy)

```
+-- Runtime output (error) ----------------------------------------------+
|                                                                        |
|  UnmatchedVariantException: Flag "checkout-flow" resolved to "BETA"    |
|  but no @Variant("BETA") implementation exists for CheckoutFlow.       |
|                                                                        |
|  Known variants: CLASSIC, STREAMLINED, PREMIUM                         |
|  Consider:                                                             |
|    1. Add a @Variant("BETA") implementation                            |
|    2. Add a @DefaultVariant implementation                             |
|    3. Use FallbackStrategy.NOOP to silently ignore unknown variants    |
|                                                                        |
+------------------------------------------------------------------------+
```

### E4: Duplicate variant value

```
+-- Compiler output (error) ---------------------------------------------+
|                                                                        |
|  ERROR: Duplicate @Variant("CLASSIC") for feature "checkout-flow".     |
|         Found on: ClassicCheckout.java:3 and LegacyCheckout.java:3     |
|         Each variant value must have exactly one implementation.       |
|                                                                        |
+------------------------------------------------------------------------+
```

## Integration Points

|   From Step   |     To Step     |                Data Flowing                 |                       Risk                       |
| ------------- | --------------- | ------------------------------------------- | ------------------------------------------------ |
| 3 (Define)    | 4 (Implement)   | Feature interface type, Variant enum values | Medium -- enum must match variant strings        |
| 4 (Implement) | AP (Compile)    | @Feature + @Variant metadata                | Low -- annotation processor handles this         |
| AP (Compile)  | 5 (Resolve)     | Generated proxy class                       | Low -- generated code is correct by construction |
| 5 (Resolve)   | FlagProvider    | Flag key string, evaluation context         | Medium -- key must match provider config         |
| 6 (Test)      | TestFlagContext | Pinned flag values                          | Low -- in-memory, no external deps               |
| 7 (Spring)    | Spring Context  | FactoryBean for proxy registration          | Low -- proven pattern                            |
