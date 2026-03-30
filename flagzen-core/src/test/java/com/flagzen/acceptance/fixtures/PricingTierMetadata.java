package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for condition dispatch acceptance testing.
 * Simulates what the annotation processor generates for an ordered dispatch feature.
 * The createProxy() method ignores the variants map and constructs the ordered proxy directly.
 */
public class PricingTierMetadata implements FeatureMetadata<PricingTier> {

    @Override
    public Class<PricingTier> featureType() {
        return PricingTier.class;
    }

    @Override
    public String flagKey() {
        return "pricing-tier";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<PricingTier>> variantSuppliers() {
        return Map.of(
                "free", FreeTier::new,
                "enterprise", EnterpriseTier::new
        );
    }

    @Override
    public Supplier<PricingTier> defaultVariantSupplier() {
        return StandardTier::new;
    }

    @Override
    public PricingTier createProxy(FlagProvider flagProvider,
                                   Map<String, Supplier<PricingTier>> variants,
                                   Supplier<PricingTier> defaultVariant) {
        // Ordered dispatch: ignore variants map, construct directly with 3-arg constructor
        return new PricingTierProxy(flagProvider, defaultVariant, FallbackStrategy.EXCEPTION);
    }
}
