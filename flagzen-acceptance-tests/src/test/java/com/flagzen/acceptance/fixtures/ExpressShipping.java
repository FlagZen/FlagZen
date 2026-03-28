package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: EXPRESS variant of ShippingMethod.
 */
public class ExpressShipping implements ShippingMethod {
    @Override
    public String execute() {
        return "ExpressShipping";
    }
}
