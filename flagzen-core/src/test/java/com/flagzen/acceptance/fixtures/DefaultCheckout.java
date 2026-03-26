package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: default variant for CheckoutFlow.
 */
public class DefaultCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "DefaultCheckout";
    }
}
