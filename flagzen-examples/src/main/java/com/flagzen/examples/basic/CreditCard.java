package com.flagzen.examples.basic;

import com.flagzen.Variant;

@Variant(value = "CREDIT_CARD", of = PaymentMethod.class)
public class CreditCard implements PaymentMethod {
    @Override
    public String process(double amount) {
        return "Charged $" + amount + " to credit card";
    }
}
