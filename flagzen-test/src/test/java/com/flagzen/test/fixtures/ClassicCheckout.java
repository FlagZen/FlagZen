package com.flagzen.test.fixtures;

/**
 * Test fixture: CLASSIC variant.
 */
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "ClassicCheckout";
    }
}
