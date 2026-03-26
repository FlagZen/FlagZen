package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-03: Pin flag values in tests with @PinFlag.
 *
 * Scenario: Developer pins a flag value in a test with a single annotation.
 * Port-to-port: @PinFlag/FlagZenExtension (driving port) -> InMemoryFlagProvider (driven port).
 *
 * The test resolves CheckoutFlow and asserts it delegates to PremiumCheckout
 * with NO explicit flag provider setup in the test method.
 */
@ExtendWith(FlagZenExtension.class)
class PinFlagAcceptanceTest {

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void resolvesFeatureToPinnedVariantWithoutProviderSetup(TestFlagContext flags) {
        // When: the test resolves "CheckoutFlow"
        CheckoutFlow checkoutFlow = flags.resolve(CheckoutFlow.class);

        // Then: the resolved proxy delegates to "PremiumCheckout"
        String result = checkoutFlow.execute();
        assertThat(result).isEqualTo("PremiumCheckout");

        // And: no flag provider setup was needed in the test
        // (proven by the absence of any InMemoryFlagProvider / FlagZen.dispatcher() calls above)
    }
}
