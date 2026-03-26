package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying @PinFlag takes priority over @FlagSource.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: When both @FlagSource and @PinFlag are present on a test,
 * the pinned value wins for that flag while unpinned flags resolve from file.
 *
 * Driving port: FlagZenExtension (JUnit 5 extension).
 * Driven port: InMemoryFlagProvider populated from properties + pin override.
 */
@ExtendWith(FlagZenExtension.class)
@FlagSource("flags-test.properties")
class PinOverridesSourceTest {

    /**
     * Behavior 1: @PinFlag overrides the value loaded from @FlagSource properties file.
     * flags-test.properties contains checkout-flow=CLASSIC, but @PinFlag sets PREMIUM.
     */
    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void pinOverridesFileSourceValue(TestFlagContext flags) {
        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("PremiumCheckout");
    }

    /**
     * Behavior 1 (variation): Without @PinFlag, the file source value is used.
     * Verifies that @FlagSource still works for tests without pins.
     */
    @Test
    void fileSourceValueUsedWithoutPin(TestFlagContext flags) {
        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("ClassicCheckout");
    }
}
