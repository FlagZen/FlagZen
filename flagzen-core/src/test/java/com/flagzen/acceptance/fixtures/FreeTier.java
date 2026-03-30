package com.flagzen.acceptance.fixtures;

/**
 * Exact-match variant for "free" tier (order=1).
 */
public class FreeTier implements PricingTier {

    @Override
    public String tierName() {
        return "Free";
    }
}
