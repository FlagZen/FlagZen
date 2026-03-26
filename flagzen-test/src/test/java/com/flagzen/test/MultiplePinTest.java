package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import com.flagzen.test.fixtures.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for multiple flags pinned in a single test.
 * Test Budget: 1 behavior x 2 = 2 max unit tests. Using 1.
 *
 * Behavior: Multiple @PinFlag annotations resolve each feature to its respectively pinned variant.
 * Driving port: FlagZenExtension + TestFlagContext.
 * Driven port: InMemoryFlagProvider (via ServiceLoader-discovered FeatureMetadata).
 */
@ExtendWith(FlagZenExtension.class)
class MultiplePinTest {

    @Test
    @PinFlag(feature = "checkout-flow", variant = "PREMIUM")
    @PinFlag(feature = "payment-method", variant = "CREDIT_CARD")
    void resolvesEachFeatureToItsPinnedVariant(TestFlagContext flags) {
        CheckoutFlow checkoutFlow = flags.resolve(CheckoutFlow.class);
        PaymentMethod paymentMethod = flags.resolve(PaymentMethod.class);

        assertThat(checkoutFlow.execute()).isEqualTo("PremiumCheckout");
        assertThat(paymentMethod.execute()).isEqualTo("CreditCardPayment");
    }
}
