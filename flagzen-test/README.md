# flagzen-test

JUnit 5 extension for testing FlagZen feature flags with pinned values and parameterized flag sources.

## Installation

```gradle
dependencies {
    testImplementation("com.flagzen:flagzen-test:1.1.0")
}
```

Requires Java 17+.

## Usage

```java
@ExtendWith(FlagZenExtension.class)
@PinFlag(key = "checkout-flow", value = "STREAMLINED")
class CheckoutTest {
    @Test
    void usesStreamlinedFlow(CheckoutFlow flow) {
        assertEquals("streamlined", flow.execute());
    }
}
```

Use `@FlagSource` for parameterized tests that exercise multiple variants. See [flagzen-examples](../flagzen-examples) for complete testing examples.

## API Overview

| Type | Description |
| --- | --- |
| `FlagZenExtension` | JUnit 5 extension -- registers a test `FlagProvider` and resolves `@Feature` parameters |
| `@PinFlag` | Pins a flag key to a fixed value for the test class or method |
| `@PinFlags` | Repeatable container for multiple `@PinFlag` annotations |
| `@FlagSource` | Parameterized flag source for `@ParameterizedTest` |
| `TestFlagContext` | Programmatic access to the test flag provider within a test |

## See Also

- [flagzen-core](../flagzen-core/README.md) -- core annotations and dispatcher
- [flagzen-examples](../flagzen-examples/README.md) -- runnable testing examples
