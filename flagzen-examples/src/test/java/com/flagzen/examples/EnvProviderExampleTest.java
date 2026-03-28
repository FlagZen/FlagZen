package com.flagzen.examples;

import com.flagzen.env.EnvironmentVariableFlagProvider;
import com.flagzen.examples.basic.PaymentMethod;
import com.flagzen.internal.DefaultFeatureDispatcher;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EnvProviderExampleTest {
    @Test
    void readsFromEnvironmentVariables() {
        // Simulate environment variables for testing
        var provider = EnvironmentVariableFlagProvider.builder()
                .environmentSource(() -> Map.of("FLAGZEN_PAYMENT_METHOD", "PAYPAL"))
                .build();

        var dispatcher = new DefaultFeatureDispatcher(provider);
        PaymentMethod payment = dispatcher.resolve(PaymentMethod.class);

        assertThat(payment.process(42.0)).contains("PayPal");
    }
}
