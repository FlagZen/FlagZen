# Walking Skeleton -- FlagZen

## Definition

The walking skeleton is the thinnest end-to-end slice proving that a developer can define a feature flag as a typed interface, implement variants, get a generated proxy, resolve it at runtime, and pin values in tests.

## Skeleton Scenarios

### Skeleton 1: Compile-time feature definition and proxy generation (US-01 + US-02 + US-04)

**User goal**: "I can define a feature as a typed interface, implement variants, and the annotation processor generates a dispatch proxy."

**What it proves**: The annotation processor pipeline works end-to-end -- from annotation to code generation.

**Implementation sequence**: This is the first scenario to enable. It exercises the Java compiler driving port (annotation processor entry point).

### Skeleton 2: Runtime resolution through FeatureDispatcher (US-05 + US-06)

**User goal**: "I can configure a flag provider, resolve a feature, and the proxy dispatches to the correct variant."

**What it proves**: The runtime dispatch pipeline works -- from flag provider configuration through dispatcher resolution to proxy delegation.

**Implementation sequence**: Second scenario. Depends on skeleton 1 (needs generated proxy). Exercises `FeatureDispatcher.resolve()` and `FlagZen.configure()` driving ports.

### Skeleton 3: Pin flag values in tests (US-07)

**User goal**: "I can pin a flag value in a test with a single annotation and resolve the feature without any provider setup."

**What it proves**: The testing DX works -- minimal setup, no mock infrastructure.

**Implementation sequence**: Third scenario. Depends on skeleton 2 (needs working dispatch). Exercises `@PinFlag` / `TestFlagContext` driving port.

## Build Order

The walking skeleton follows the dependency chain from the DISCUSS story map:

```
US-01 (@Feature) -> US-02 (@Variant) -> US-04 (Proxy Gen)
                                              |
                                              v
                               US-06 (FlagProvider) -> US-05 (Dispatcher)
                                                            |
                                                            v
                                                     US-07 (@PinFlag)
```

## Stakeholder Demo Script

After the walking skeleton passes:

1. Show `CheckoutFlow` interface with `@Feature("checkout-flow")` -- "This is how you define a feature flag"
2. Show `ClassicCheckout` and `StreamlinedCheckout` with `@Variant` -- "These are the implementations"
3. Compile and show generated `CheckoutFlow_FlagZenProxy` -- "The annotation processor generates this"
4. Show runtime resolution: set flag to "STREAMLINED", resolve, call method -- "It dispatches to the right variant"
5. Show test with `@PinFlag(feature = "checkout-flow", variant = "PREMIUM")` -- "One annotation, zero setup"

This demo is fully executable from the acceptance tests.
