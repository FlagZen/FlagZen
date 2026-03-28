package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant for standard pricing tier.
 */
public class StandardPricing implements PricingTier {

    @Override
    public String execute() {
        return "StandardPricing";
    }
}
