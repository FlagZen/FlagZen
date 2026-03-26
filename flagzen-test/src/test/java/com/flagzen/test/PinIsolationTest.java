package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for pin value isolation between test contexts.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: Two separate TestFlagContext instances pinning the same flag
 * to different values resolve independently without cross-contamination.
 */
class PinIsolationTest {

    @Test
    void pinnedValuesAreIsolatedBetweenTestContexts() {
        TestFlagContext contextA = TestFlagContext.create();
        TestFlagContext contextB = TestFlagContext.create();

        contextA.pin("checkout-flow", "PREMIUM");
        contextB.pin("checkout-flow", "CLASSIC");

        CheckoutFlow proxyA = contextA.resolve(CheckoutFlow.class);
        CheckoutFlow proxyB = contextB.resolve(CheckoutFlow.class);

        assertThat(proxyA.execute()).isEqualTo("PremiumCheckout");
        assertThat(proxyB.execute()).isEqualTo("ClassicCheckout");
    }
}
