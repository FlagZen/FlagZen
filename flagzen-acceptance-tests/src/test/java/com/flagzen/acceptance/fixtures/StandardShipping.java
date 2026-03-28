package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: STANDARD variant of ShippingMethod.
 */
public class StandardShipping implements ShippingMethod {
    @Override
    public String execute() {
        return "StandardShipping";
    }
}
