package com.flagzen.test.fixtures;

/**
 * Test fixture: PREMIUM variant.
 */
public class PremiumCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "PremiumCheckout";
    }
}
