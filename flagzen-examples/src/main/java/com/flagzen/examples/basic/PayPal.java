package com.flagzen.examples.basic;

import com.flagzen.Variant;

@Variant(value = "PAYPAL", of = PaymentMethod.class)
public class PayPal implements PaymentMethod {
    @Override
    public String process(double amount) {
        return "Sent $" + amount + " via PayPal";
    }
}
