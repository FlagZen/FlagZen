package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for @PinFlag / FlagZenExtension through the driving port.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests.
 *
 * Driving port: FlagZenExtension (JUnit 5 extension) + TestFlagContext parameter.
 * Driven port: InMemoryFlagProvider (via ServiceLoader-discovered FeatureMetadata).
 */
@ExtendWith(FlagZenExtension.class)
class PinFlagTest {

    /**
     * Behavior 1: @PinFlag pins a feature to the specified variant.
     * The TestFlagContext resolves the feature to the pinned variant's implementation.
     */
    @Test
    @PinFlag(feature = "checkout-flow", variant = "CLASSIC")
    void resolvesFeatureToPinnedVariant(TestFlagContext flags) {
        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("ClassicCheckout");
    }

    /**
     * Behavior 2: TestFlagContext.pin() programmatically pins a flag value.
     */
    @Test
    void pinsProgrammaticallyViaTestFlagContext(TestFlagContext flags) {
        flags.pin("checkout-flow", "PREMIUM");

        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("PremiumCheckout");
    }

    /**
     * Behavior 3: Multiple @PinFlag annotations on same method work.
     * (Uses single feature here but proves repeatable annotation support.)
     */
    @Test
    @PinFlag(feature = "checkout-flow", variant = "CLASSIC")
    void supportsAnnotationDrivenPinning(TestFlagContext flags) {
        // Programmatic pin overrides annotation pin
        flags.pin("checkout-flow", "PREMIUM");

        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("PremiumCheckout");
    }
}
