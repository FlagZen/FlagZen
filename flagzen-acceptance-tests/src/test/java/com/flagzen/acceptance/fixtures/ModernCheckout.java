package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: MODERN variant for multi-value dispatch scenarios.
 */
public class ModernCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "ModernCheckout";
    }
}
