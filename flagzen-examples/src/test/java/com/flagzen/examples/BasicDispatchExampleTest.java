package com.flagzen.examples;

import com.flagzen.examples.basic.PaymentMethod;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BasicDispatchExampleTest {
    @Test
    void dispatchesToActiveVariant() {
        var provider = new InMemoryFlagProvider();
        provider.set("payment-method", "PAYPAL");
        var dispatcher = new DefaultFeatureDispatcher(provider);

        PaymentMethod payment = dispatcher.resolve(PaymentMethod.class);

        assertThat(payment.process(99.99)).contains("PayPal");
    }

    @Test
    void dynamicallyFollowsFlagChanges() {
        var provider = new InMemoryFlagProvider();
        provider.set("payment-method", "CREDIT_CARD");
        var dispatcher = new DefaultFeatureDispatcher(provider);
        PaymentMethod payment = dispatcher.resolve(PaymentMethod.class);

        assertThat(payment.process(50.0)).contains("credit card");

        provider.set("payment-method", "PAYPAL");
        assertThat(payment.process(50.0)).contains("PayPal");
    }
}
