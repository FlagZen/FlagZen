package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant for bulk pricing tier.
 */
public class BulkPricing implements PricingTier {

    @Override
    public String execute() {
        return "BulkPricing";
    }
}
