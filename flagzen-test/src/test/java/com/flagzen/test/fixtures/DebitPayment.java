package com.flagzen.test.fixtures;

/**
 * Test fixture: DEBIT variant.
 */
public class DebitPayment implements PaymentMethod {
    @Override
    public String execute() {
        return "DebitPayment";
    }
}
