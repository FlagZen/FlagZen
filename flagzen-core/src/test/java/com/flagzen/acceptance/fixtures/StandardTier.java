package com.flagzen.acceptance.fixtures;

/**
 * Default variant for pricing tier (fallback when no match).
 */
public class StandardTier implements PricingTier {

    @Override
    public String tierName() {
        return "Standard";
    }
}
