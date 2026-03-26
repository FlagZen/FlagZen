package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant implementation returning identifying string.
 */
public class ClassicCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "ClassicCheckout";
    }
}
