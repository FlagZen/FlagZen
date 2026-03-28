package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: EXPRESS variant.
 */
public class ExpressCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "ExpressCheckout";
    }
}
