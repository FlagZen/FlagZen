package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for LONG dispatch acceptance testing.
 * Simulates what the annotation processor would generate for a LONG-typed feature.
 */
public class RateLimiterMetadata implements FeatureMetadata<RateLimiter> {

    private static volatile Map<String, Supplier<RateLimiter>> multiValueVariants = null;

    /**
     * Configures multi-value variant mappings for acceptance test scenarios.
     */
    public static void setMultiValueVariants(Map<String, Supplier<RateLimiter>> variants) {
        multiValueVariants = variants;
    }

    public static void reset() {
        multiValueVariants = null;
    }

    @Override
    public Class<RateLimiter> featureType() {
        return RateLimiter.class;
    }

    @Override
    public String flagKey() {
        return "rate-limit";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<RateLimiter>> variantSuppliers() {
        if (multiValueVariants != null) {
            return multiValueVariants;
        }
        return Map.of(
                "1000", LowVolumeLimit::new,
                "50000", HighVolumeLimit::new
        );
    }

    @Override
    public Supplier<RateLimiter> defaultVariantSupplier() {
        return null;
    }

    @Override
    public RateLimiter createProxy(FlagProvider flagProvider,
                                   Map<String, Supplier<RateLimiter>> variants,
                                   Supplier<RateLimiter> defaultVariant) {
        Map<Long, Supplier<RateLimiter>> longVariants = new HashMap<>();
        variants.forEach((k, v) -> longVariants.put(Long.parseLong(k), v));
        return new RateLimiterProxy(flagProvider, longVariants, defaultVariant);
    }
}
