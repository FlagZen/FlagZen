package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for INT multi-value dispatch acceptance testing.
 * Supports configurable multi-value variant mappings for test scenarios.
 */
public class PricingTierMetadata implements FeatureMetadata<PricingTier> {

    private static volatile Map<String, Supplier<PricingTier>> multiValueVariants = null;

    /**
     * Configures multi-value variant mappings for acceptance test scenarios.
     */
    public static void setMultiValueVariants(Map<String, Supplier<PricingTier>> variants) {
        multiValueVariants = variants;
    }

    public static void reset() {
        multiValueVariants = null;
    }

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
        if (multiValueVariants != null) {
            return multiValueVariants;
        }
        return Map.of(
                "1", StandardPricing::new,
                "3", BulkPricing::new,
                "5", BulkPricing::new
        );
    }

    @Override
    public Supplier<PricingTier> defaultVariantSupplier() {
        return null;
    }

    @Override
    public PricingTier createProxy(FlagProvider flagProvider,
                                   Map<String, Supplier<PricingTier>> variants,
                                   Supplier<PricingTier> defaultVariant) {
        Map<Integer, Supplier<PricingTier>> intVariants = new HashMap<>();
        variants.forEach((k, v) -> intVariants.put(Integer.parseInt(k), v));
        return new PricingTierProxy(flagProvider, intVariants, defaultVariant);
    }
}
