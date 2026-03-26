package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import com.flagzen.test.fixtures.PaymentMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test: pin always takes priority over file source.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Property: For any test with both a file-based flag source and a pin annotation
 * for the same flag, the pinned value is always used regardless of configuration order.
 *
 * Driving port: TestFlagContext (programmatic API simulating FlagZenExtension behavior).
 * Driven port: InMemoryFlagProvider populated from properties + pin override.
 */
class PinPriorityPropertyTest {

    /**
     * Property 1: Pin always overrides file source value for CheckoutFlow.
     * Parametrized across all valid variant combinations to prove the invariant.
     * File source provides checkout-flow=CLASSIC; pin overrides to each variant.
     */
    @ParameterizedTest(name = "pin {0} overrides file CLASSIC -> delegates to {1}")
    @CsvSource({
            "PREMIUM, PremiumCheckout",
            "CLASSIC, ClassicCheckout",
    })
    void pinAlwaysWinsOverFileSource(String pinVariant, String expectedDelegate) {
        TestFlagContext context = TestFlagContext.createFromProperties("flags-test.properties");
        context.pin("checkout-flow", pinVariant);

        CheckoutFlow flow = context.resolve(CheckoutFlow.class);

        assertThat(flow.execute())
                .as("Pin '%s' must override file value for checkout-flow", pinVariant)
                .isEqualTo(expectedDelegate);
    }

    /**
     * Property 1 (variation): Pin priority holds for different feature types.
     * Uses PaymentMethod with a properties file that does not contain payment-method,
     * proving pin works even when the file has no entry for the flag.
     */
    @ParameterizedTest(name = "pin {0} for payment-method -> delegates to {1}")
    @CsvSource({
            "CREDIT_CARD, CreditCardPayment",
            "DEBIT, DebitPayment",
    })
    void pinWinsForAnyFeatureType(String pinVariant, String expectedDelegate) {
        TestFlagContext context = TestFlagContext.createFromProperties("flags-test.properties");
        context.pin("payment-method", pinVariant);

        PaymentMethod payment = context.resolve(PaymentMethod.class);

        assertThat(payment.execute())
                .as("Pin '%s' must be used for payment-method", pinVariant)
                .isEqualTo(expectedDelegate);
    }
}
