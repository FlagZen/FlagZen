package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for step 04-04: Feature interface injected as resolved proxy in test parameter.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Driving port: FlagZenExtension (JUnit 5 ParameterResolver).
 * Behavior: A test method declaring a feature interface as a parameter receives
 * a resolved proxy matching the pinned variant.
 */
@ExtendWith(FlagZenExtension.class)
class ParameterInjectionTest {

    /**
     * Behavior 1: Feature interface parameter is resolved to a proxy matching the pinned variant.
     */
    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    void resolvesFeatureInterfaceParameterToProxy(CheckoutFlow checkoutFlow) {
        assertThat(checkoutFlow.execute()).isEqualTo("PremiumCheckout");
    }
}
