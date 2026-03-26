package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant implementation returning identifying string.
 */
public class StreamlinedCheckout implements CheckoutFlow {
    @Override
    public String execute() {
        return "StreamlinedCheckout";
    }
}
