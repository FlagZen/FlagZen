package com.flagzen.examples;

import com.flagzen.examples.basic.PaymentMethod;
import com.flagzen.test.FlagZenExtension;
import com.flagzen.test.PinFlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FlagZenExtension.class)
class TestingExampleTest {

    @Test
    @PinFlag(feature = "payment-method", variant = "CREDIT_CARD")
    void pinFlagSelectsVariant(PaymentMethod payment) {
        assertThat(payment.process(100.0)).contains("credit card");
    }

    @Test
    @PinFlag(feature = "payment-method", variant = "PAYPAL")
    void differentPinSelectsDifferentVariant(PaymentMethod payment) {
        assertThat(payment.process(100.0)).contains("PayPal");
    }
}
