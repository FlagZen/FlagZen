package com.flagzen.acceptance.fixtures;

/**
 * Condition-based variant for enterprise tier (order=2, predicate=IsEnterpriseTier).
 */
public class EnterpriseTier implements PricingTier {

    @Override
    public String tierName() {
        return "Enterprise";
    }
}
