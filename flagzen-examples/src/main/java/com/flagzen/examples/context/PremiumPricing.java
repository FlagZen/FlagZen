package com.flagzen.examples.context;

import com.flagzen.Variant;

@Variant(value = "PREMIUM", of = PricingTier.class)
public class PremiumPricing implements PricingTier {
    @Override
    public double discount() { return 0.20; }
}
