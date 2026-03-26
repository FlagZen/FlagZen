package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: CREDIT_CARD variant.
 */
public class CreditCardPayment implements PaymentMethod {
    @Override
    public String execute() {
        return "CreditCardPayment";
    }
}
