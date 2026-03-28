# Tutorial: Your First Real Feature Flag

Build a realistic checkout experience where the flag value controls which checkout variant runs. You'll see how the same dispatcher code handles multiple variants, and how adding a new variant doesn't require changes to your dispatch logic.

This tutorial assumes you've completed [Getting Started](getting-started.md). If you're new to FlagZen, start there first.

## What You'll Build

A checkout flow with three variants:

- **CLASSIC**: The existing checkout (what you have today)
- **STREAMLINED**: A newer, faster checkout
- **PREMIUM**: A checkout optimized for premium users

By the end, you'll flip the flag to activate variants without changing any dispatch code.

## Step 1: Set Up Your Project

If you have the getting-started project from the previous tutorial, use that. Otherwise, create a new Gradle project:

```bash
mkdir flagzen-checkout && cd flagzen-checkout
```

Use the same `build.gradle` as before:

```gradle
plugins {
    id 'java'
}

group = 'com.example'
version = '1.0.0'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.flagzen:flagzen-core:1.1.0")
    annotationProcessor("com.flagzen:flagzen-core:1.1.0")
}
```

Create the directory structure:

```bash
mkdir -p src/main/java/com/example/checkout
```

## Step 2: Define the CheckoutFlow Interface

Create `src/main/java/com/example/checkout/CheckoutFlow.java`:

```java
package com.example.checkout;

import com.flagzen.Feature;

@Feature("checkout-flow")
public interface CheckoutFlow {
    /**
     * Process a checkout for the given cart total.
     * Returns a summary of what happened.
     */
    String processCheckout(double cartTotal);
}
```

This interface defines what a checkout flow must do: accept a cart total and return a summary.

The `@Feature("checkout-flow")` annotation means: "The value of the 'checkout-flow' flag determines which implementation runs."

## Step 3: Implement the CLASSIC Variant

Create `src/main/java/com/example/checkout/ClassicCheckout.java`:

```java
package com.example.checkout;

import com.flagzen.Variant;

@Variant(value = "CLASSIC", of = CheckoutFlow.class)
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String processCheckout(double cartTotal) {
        // Classic checkout: collect email, address, payment
        // Lots of form fields, many steps
        double taxRate = 0.08;
        double tax = cartTotal * taxRate;
        double total = cartTotal + tax;

        return "CLASSIC CHECKOUT: "
            + "Cart $" + cartTotal
            + " + tax $" + String.format("%.2f", tax)
            + " = Total $" + String.format("%.2f", total)
            + " | Sent confirmation to email";
    }
}
```

## Step 4: Implement the STREAMLINED Variant

Create `src/main/java/com/example/checkout/StreamlinedCheckout.java`:

```java
package com.example.checkout;

import com.flagzen.Variant;

@Variant(value = "STREAMLINED", of = CheckoutFlow.class)
public class StreamlinedCheckout implements CheckoutFlow {
    @Override
    public String processCheckout(double cartTotal) {
        // Streamlined checkout: fewer steps, uses saved address
        // Assumes user is logged in and has a saved address
        double taxRate = 0.08;
        double tax = cartTotal * taxRate;
        double total = cartTotal + tax;

        return "STREAMLINED CHECKOUT: "
            + "Cart $" + cartTotal
            + " + tax $" + String.format("%.2f", tax)
            + " = Total $" + String.format("%.2f", total)
            + " | Using saved address | One-click approval";
    }
}
```

## Step 5: Implement the PREMIUM Variant

Create `src/main/java/com/example/checkout/PremiumCheckout.java`:

```java
package com.example.checkout;

import com.flagzen.Variant;

@Variant(value = "PREMIUM", of = CheckoutFlow.class)
public class PremiumCheckout implements CheckoutFlow {
    @Override
    public String processCheckout(double cartTotal) {
        // Premium checkout: no tax, free shipping, concierge
        double premiumDiscount = cartTotal * 0.10; // 10% loyalty discount
        double total = cartTotal - premiumDiscount;

        return "PREMIUM CHECKOUT: "
            + "Cart $" + cartTotal
            + " - loyalty discount $" + String.format("%.2f", premiumDiscount)
            + " = Total $" + String.format("%.2f", total)
            + " | Free shipping | Concierge support available";
    }
}
```

## Step 6: Create the Main Program

Create `src/main/java/com/example/checkout/Main.java`:

```java
package com.example.checkout;

import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;

public class Main {
    public static void main(String[] args) {
        // Create a provider to hold flag values
        InMemoryFlagProvider provider = new InMemoryFlagProvider();

        // Create a dispatcher
        DefaultFeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // Resolve the feature interface
        // This returns a proxy that will route to the active variant
        CheckoutFlow checkout = dispatcher.resolve(CheckoutFlow.class);

        // Simulate a $100 cart
        double cartTotal = 100.00;

        // Try each variant by changing the flag
        System.out.println("=== CLASSIC ===");
        provider.set("checkout-flow", "CLASSIC");
        System.out.println(checkout.processCheckout(cartTotal));

        System.out.println("\n=== STREAMLINED ===");
        provider.set("checkout-flow", "STREAMLINED");
        System.out.println(checkout.processCheckout(cartTotal));

        System.out.println("\n=== PREMIUM ===");
        provider.set("checkout-flow", "PREMIUM");
        System.out.println(checkout.processCheckout(cartTotal));
    }
}
```

Compile and run:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.checkout.Main
```

Output:

```
=== CLASSIC ===
CLASSIC CHECKOUT: Cart $100.0 + tax $8.00 = Total $108.00 | Sent confirmation to email

=== STREAMLINED ===
STREAMLINED CHECKOUT: Cart $100.0 + tax $8.00 = Total $108.00 | Using saved address | One-click approval

=== PREMIUM ===
PREMIUM CHECKOUT: Cart $100.0 - loyalty discount $10.00 = Total $90.00 | Free shipping | Concierge support available
```

## Step 7: Understand Dynamic Dispatch

Each time you call `checkout.processCheckout(cartTotal)`, the FlagZen proxy:

1. Asks the provider: "What's the current value of 'checkout-flow'?"
2. Finds the matching variant
3. Instantiates it
4. Calls `processCheckout()` on that instance

You changed the flag three times without restarting. The same `checkout` proxy object (which is actually `CheckoutFlow_FlagZenProxy`) routed to three different implementations.

Change line 5 of `Main.java`:

```java
double cartTotal = 50.00;
```

Recompile and run again. Same dispatch code, different input, each variant handles it:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.checkout.Main
```

Notice: The logic is in the variants, not in the dispatcher. The proxy just knows how to route.

## Step 8: Add a Fourth Variant (The Power of the Pattern)

Without changing a single line of dispatch code, add a new variant. Create `src/main/java/com/example/checkout/ExpressCheckout.java`:

```java
package com.example.checkout;

import com.flagzen.Variant;

@Variant(value = "EXPRESS", of = CheckoutFlow.class)
public class ExpressCheckout implements CheckoutFlow {
    @Override
    public String processCheckout(double cartTotal) {
        // Express checkout: only for logged-in users with saved payment
        // Requires prior checkout attempt
        double total = cartTotal * 1.08; // standard tax

        return "EXPRESS CHECKOUT: "
            + "Cart $" + cartTotal
            + " (with tax) = Total $" + String.format("%.2f", total)
            + " | Instant approval | 30 minute pickup";
    }
}
```

Update `Main.java` to include EXPRESS:

```java
System.out.println("\n=== EXPRESS ===");
provider.set("checkout-flow", "EXPRESS");
System.out.println(checkout.processCheckout(cartTotal));
```

Recompile and run:

```bash
./gradlew build
java -cp build/classes/java/main:build/libs/* com.example.checkout.Main
```

The new variant works immediately. No changes to the dispatch logic. The annotation processor regenerated the proxy to include EXPRESS.

## Step 9: See the Generated Proxy

FlagZen generates a proxy class for every `@Feature` interface. It's hidden in your build directory, but you can find it:

```bash
find build -name "*FlagZenProxy.class"
```

You might see:

```
build/classes/java/main/com/example/checkout/CheckoutFlow_FlagZenProxy.class
```

This is the class the dispatcher instantiated. You can't easily read `.class` files, but conceptually it looks something like this (pseudo-code):

```java
public class CheckoutFlow_FlagZenProxy implements CheckoutFlow {
    private FlagProvider provider;

    public String processCheckout(double cartTotal) {
        String flagValue = provider.getString("checkout-flow");

        return switch(flagValue) {
            case "CLASSIC" -> new ClassicCheckout().processCheckout(cartTotal);
            case "STREAMLINED" -> new StreamlinedCheckout().processCheckout(cartTotal);
            case "PREMIUM" -> new PremiumCheckout().processCheckout(cartTotal);
            case "EXPRESS" -> new ExpressCheckout().processCheckout(cartTotal);
            default -> throw new NoMatchingVariantException(...);
        };
    }
}
```

The annotation processor wrote this code at compile time. Zero reflection. Direct method calls. Type-safe.

When you call `checkout.processCheckout(100.0)`, you're calling the proxy, which checks the flag and routes to the matching variant.

## Key Insights

1. **The feature interface is your contract.** Callers use `CheckoutFlow`, never the variants directly.

2. **Variants are internal.** Only the annotation processor and dispatcher know about them. They're never instantiated by hand.

3. **The proxy is hidden.** You never see it. The dispatcher creates it and returns it as the feature interface.

4. **Dispatch happens at runtime.** The flag value is read on every method call, so changing the flag immediately changes behavior.

5. **Adding variants is safe.** The annotation processor regenerates the proxy. Old dispatch code keeps working.

## Next Steps

- **Want to test this with different variants?** See [Tutorial: Testing](testing.md) — pin variants in unit tests.
- **Need to deploy and configure flags?** See [flagzen-env](https://github.com/FlagZen/FlagZen/blob/main/flagzen-env/README.md) for environment variable support.
- **Integrating with Spring?** See [flagzen-spring](https://github.com/FlagZen/FlagZen/blob/main/flagzen-spring/README.md) to inject feature proxies.
- **Curious about the full design?** See [Architecture](https://github.com/FlagZen/FlagZen/blob/main/docs/feature/flagzen/design/architecture-design.md) for how compile-time and runtime work together.
