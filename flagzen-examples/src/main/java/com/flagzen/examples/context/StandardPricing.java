package com.flagzen.examples.context;

import com.flagzen.Variant;

@Variant(value = "STANDARD", of = PricingTier.class)
public class StandardPricing implements PricingTier {
    @Override
    public double discount() { return 0.0; }
}
