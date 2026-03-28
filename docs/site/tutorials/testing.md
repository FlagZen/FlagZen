# Tutorial: Testing with FlagZen

Learn to write tests that pin feature flags to specific variants. By the end, you'll know how to test all branches of your feature flags without manual setup, and how FlagZen's test extension handles the wiring for you.

This tutorial assumes you've completed [Getting Started](getting-started.md). It uses the checkout example from [Your First Real Feature Flag](first-feature-flag.md).

## What You'll Learn

- Add the `flagzen-test` dependency
- Use `@ExtendWith(FlagZenExtension.class)` to enable testing
- Pin flags with `@PinFlag` at the class and method level
- Inject feature proxies directly into test methods
- Override pinned flags within a test
- Load flags from a properties file with `@FlagSource`

## Prerequisites

- Java 17+
- Gradle 7+
- JUnit 5
- A project with at least one `@Feature` interface (from a previous tutorial)

## Step 1: Add the Test Dependency

In your `build.gradle`, add `flagzen-test` to the test dependencies:

```gradle
dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")

    testImplementation("com.flagzen:flagzen-test:1.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}
```

Gradle will download the extension and all dependencies. The annotation processor still only runs for main code.

## Step 2: Write a Basic Test with a Pinned Flag

Create `src/test/java/com/example/checkout/CheckoutTest.java`:

```java
package com.example.checkout;

import com.flagzen.test.FlagZenExtension;
import com.flagzen.test.PinFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FlagZenExtension.class)
class CheckoutTest {

    @Test
    @PinFlag(feature = "checkout-flow", variant = "CLASSIC")
    void classicCheckoutChargesTax(CheckoutFlow checkout) {
        String result = checkout.processCheckout(100.0);
        assertThat(result).contains("CLASSIC");
        assertThat(result).contains("tax");
    }
}
```

Let's break this down:

- `@ExtendWith(FlagZenExtension.class)` tells JUnit 5 to use FlagZen's test support. The extension:
  - Creates an in-memory flag provider
  - Resolves feature proxies using that provider
  - Injects them into test method parameters
  - Cleans up after each test

- `@PinFlag(feature = "checkout-flow", variant = "CLASSIC")` sets the `checkout-flow` flag to `CLASSIC` for this test method.

- `void classicCheckoutChargesTax(CheckoutFlow checkout)` — the test method parameter `checkout` is the resolved proxy. JUnit 5 (via the extension) injects it. No setup code needed.

Run the test:

```bash
./gradlew test
```

The test passes. The `checkout` proxy was pinned to CLASSIC, so `processCheckout()` routed to `ClassicCheckout`.

## Step 3: Test Different Variants with Method-Level Pinning

Add more test methods to the same class:

```java
@Test
@PinFlag(feature = "checkout-flow", variant = "STREAMLINED")
void streamlinedCheckoutUsesQuickPath(CheckoutFlow checkout) {
    String result = checkout.processCheckout(100.0);
    assertThat(result).contains("STREAMLINED");
    assertThat(result).contains("saved address");
}

@Test
@PinFlag(feature = "checkout-flow", variant = "PREMIUM")
void premiumCheckoutAppliesDiscount(CheckoutFlow checkout) {
    String result = checkout.processCheckout(100.0);
    assertThat(result).contains("PREMIUM");
    assertThat(result).contains("loyalty discount");
}
```

Each test method gets its own pinned variant. Run all three:

```bash
./gradlew test --tests CheckoutTest
```

All three pass. Each used a different variant:

- Method 1: pinned to CLASSIC
- Method 2: pinned to STREAMLINED
- Method 3: pinned to PREMIUM

No manual dispatcher creation. No manual provider setup. The extension handled it.

## Step 4: Pin Flags at the Class Level

Sometimes you want all tests in a class to use the same flag value. Use `@PinFlag` on the class itself:

```java
@ExtendWith(FlagZenExtension.class)
@PinFlag(feature = "checkout-flow", variant = "STREAMLINED")
class StreamlinedCheckoutTests {

    @Test
    void minimalCartWorks(CheckoutFlow checkout) {
        String result = checkout.processCheckout(1.0);
        assertThat(result).contains("$1.0");
    }

    @Test
    void largeCartWorks(CheckoutFlow checkout) {
        String result = checkout.processCheckout(5000.0);
        assertThat(result).contains("$5000");
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void methodLevelOverridesClassLevel(CheckoutFlow checkout) {
        // This test uses PREMIUM, overriding the class-level STREAMLINED
        String result = checkout.processCheckout(100.0);
        assertThat(result).contains("PREMIUM");
        assertThat(result).contains("loyalty discount");
    }
}
```

Key points:

- The first two tests inherit the class-level `@PinFlag`, so both use STREAMLINED.
- The third test has its own `@PinFlag`, which overrides the class level.
- Priority: method-level `@PinFlag` > class-level `@PinFlag` > provider default

Run these tests:

```bash
./gradlew test --tests StreamlinedCheckoutTests
```

All three pass.

## Step 5: Programmatic Control with TestFlagContext

If you need to change a pinned flag within a single test, use `TestFlagContext`:

```java
import com.flagzen.test.TestFlagContext;

@ExtendWith(FlagZenExtension.class)
class DynamicPinTest {

    @Test
    void changeVariantWithinTest(CheckoutFlow checkout) {
        // No @PinFlag, so start with the provider default (undefined)
        // Let's set it programmatically

        TestFlagContext.pin("checkout-flow", "CLASSIC");
        String classic = checkout.processCheckout(100.0);
        assertThat(classic).contains("CLASSIC");

        TestFlagContext.pin("checkout-flow", "PREMIUM");
        String premium = checkout.processCheckout(100.0);
        assertThat(premium).contains("PREMIUM");

        // Within the test, the same checkout proxy returned different
        // implementations because we changed the pinned value
    }
}
```

`TestFlagContext.pin(key, value)` programmatically sets a flag value for the current test. It overrides `@PinFlag` annotations.

This is useful when you want to test state transitions: "First call CLASSIC, then switch to STREAMLINED, then verify the behavior changed."

Run it:

```bash
./gradlew test --tests DynamicPinTest
```

Passes. You changed the flag twice in a single test method, and the proxy routed to the correct variant each time.

## Step 6: Load Flags from a Properties File

For parameterized tests, you can load flag values from a `.properties` file. Create `src/test/resources/checkout-variants.properties`:

```properties
checkout-flow=CLASSIC
```

Create a parameterized test that uses it:

```java
import org.junit.jupiter.params.ParameterizedTest;
import com.flagzen.test.FlagSource;

@ExtendWith(FlagZenExtension.class)
class ParameterizedCheckoutTest {

    @ParameterizedTest(name = "Cart ${0} with {1}")
    @FlagSource("classpath:checkout-variants.properties")
    void worksWithMultipleCartAmounts(
        double cartTotal,
        CheckoutFlow checkout
    ) {
        String result = checkout.processCheckout(cartTotal);
        assertThat(result).isNotBlank();
    }
}
```

Actually, let me show a more practical example. `@FlagSource` is powerful for testing all variants:

Create `src/test/resources/all-variants.properties`:

```properties
# File 1: CLASSIC variant
checkout-flow=CLASSIC

# File 2: STREAMLINED variant
checkout-flow=STREAMLINED

# File 3: PREMIUM variant
checkout-flow=PREMIUM
```

Create separate property files for clarity:

`src/test/resources/classic-checkout.properties`:
```properties
checkout-flow=CLASSIC
```

`src/test/resources/streamlined-checkout.properties`:
```properties
checkout-flow=STREAMLINED
```

`src/test/resources/premium-checkout.properties`:
```properties
checkout-flow=PREMIUM
```

Then parameterize:

```java
import org.junit.jupiter.params.ParameterizedTest;
import com.flagzen.test.FlagSource;

@ExtendWith(FlagZenExtension.class)
class AllVariantsTest {

    @ParameterizedTest(name = "{0}")
    @FlagSource({
        "classpath:classic-checkout.properties",
        "classpath:streamlined-checkout.properties",
        "classpath:premium-checkout.properties"
    })
    void allVariantsWork(String variantName, CheckoutFlow checkout) {
        String result = checkout.processCheckout(100.0);
        assertThat(result).isNotBlank();
    }
}
```

This runs the same test three times, once for each variant. Perfect for regression testing — every variant gets exercised.

## Step 7: Test Flag Priority

Here's a test that verifies the priority order: method-level > class-level > provider default.

Create `src/test/java/com/example/checkout/FlagPriorityTest.java`:

```java
package com.example.checkout;

import com.flagzen.test.FlagZenExtension;
import com.flagzen.test.PinFlag;
import com.flagzen.test.TestFlagContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FlagZenExtension.class)
@PinFlag(feature = "checkout-flow", variant = "CLASSIC")
class FlagPriorityTest {

    @Test
    void classLevelPinIsActive(CheckoutFlow checkout) {
        // Class-level @PinFlag is active
        String result = checkout.processCheckout(100.0);
        assertThat(result).contains("CLASSIC");
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void methodLevelOverridesClass(CheckoutFlow checkout) {
        // Method-level @PinFlag overrides class level
        String result = checkout.processCheckout(100.0);
        assertThat(result).contains("PREMIUM");
        assertThat(result).doesNotContain("CLASSIC");
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "STREAMLINED")
    void contextOverridesAll(CheckoutFlow checkout) {
        // TestFlagContext overrides all @PinFlag annotations
        TestFlagContext.pin("checkout-flow", "PREMIUM");

        String result = checkout.processCheckout(100.0);
        assertThat(result).contains("PREMIUM");
        assertThat(result).doesNotContain("STREAMLINED");
    }
}
```

Run these:

```bash
./gradlew test --tests FlagPriorityTest
```

All three pass. This demonstrates the complete priority chain.

## Step 8: Verify Your Test Setup

Here's a final comprehensive test that exercises the full feature:

```java
package com.example.checkout;

import com.flagzen.test.FlagZenExtension;
import com.flagzen.test.PinFlag;
import com.flagzen.test.TestFlagContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FlagZenExtension.class)
class CheckoutComprehensiveTest {

    @Test
    @PinFlag(feature = "checkout-flow", variant = "CLASSIC")
    void classicCheckoutFullFlow(CheckoutFlow checkout) {
        // Test a realistic checkout flow
        double cartTotal = 99.99;
        String result = checkout.processCheckout(cartTotal);

        assertThat(result)
            .contains("CLASSIC CHECKOUT")
            .contains("tax")
            .contains("$99.99")
            .contains("confirmation to email");
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "STREAMLINED")
    void streamlinedCheckoutQuicker(CheckoutFlow checkout) {
        double cartTotal = 99.99;
        String result = checkout.processCheckout(cartTotal);

        assertThat(result)
            .contains("STREAMLINED CHECKOUT")
            .contains("saved address")
            .contains("one-click");
    }

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void premiumCheckoutHighValue(CheckoutFlow checkout) {
        double cartTotal = 99.99;
        String result = checkout.processCheckout(cartTotal);

        assertThat(result)
            .contains("PREMIUM CHECKOUT")
            .contains("loyalty discount")
            .contains("free shipping");
    }

    @Test
    void multipleVariantsInOneTest(CheckoutFlow checkout) {
        // Test transitions without @PinFlag
        // Use TestFlagContext to change within the test

        TestFlagContext.pin("checkout-flow", "CLASSIC");
        String classic = checkout.processCheckout(50.0);
        assertThat(classic).contains("CLASSIC");

        TestFlagContext.pin("checkout-flow", "PREMIUM");
        String premium = checkout.processCheckout(50.0);
        assertThat(premium).contains("PREMIUM");

        // Verify that changing the flag changed the behavior
        assertThat(classic).doesNotContain("loyalty discount");
        assertThat(premium).contains("loyalty discount");
    }
}
```

Run the full test suite:

```bash
./gradlew test
```

All tests pass. You're now testing multiple variants of a single feature with minimal boilerplate.

## Key Takeaways

1. **@ExtendWith(FlagZenExtension.class)** enables all FlagZen testing features. It's the entry point.

2. **@PinFlag** at class or method level sets a flag value for the test. Method level overrides class level.

3. **Feature injection** — declare a feature parameter in your test method, and the extension injects the resolved proxy. No manual setup.

4. **TestFlagContext** lets you change flags programmatically within a test. Useful for testing state transitions.

5. **@FlagSource** loads flags from properties files. Perfect for parameterized tests that exercise all variants.

6. **Priority** — TestFlagContext > method @PinFlag > class @PinFlag > provider default.

## What's Next

- **Need to deploy and control flags in production?** See [flagzen-env](https://github.com/FlagZen/FlagZen/blob/main/flagzen-env/README.md) for environment variable support.
- **Using Spring?** See [flagzen-spring](https://github.com/FlagZen/FlagZen/blob/main/flagzen-spring/README.md) to integrate with Spring Boot.
- **Want to understand the runtime dispatch?** See [Architecture](https://github.com/FlagZen/FlagZen/blob/main/docs/feature/flagzen/design/architecture-design.md) for how compile-time proxy generation enables zero-reflection dispatch.
- **Testing multiple features?** Use multiple `@PinFlag` annotations or `@PinFlags` (the repeatable container).

Happy testing!
