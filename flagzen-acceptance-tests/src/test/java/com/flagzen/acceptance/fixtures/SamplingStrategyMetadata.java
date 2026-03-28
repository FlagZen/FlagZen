package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for DOUBLE dispatch acceptance testing.
 * Configurable variant list and fallback strategy for different test scenarios.
 */
public class SamplingStrategyMetadata implements FeatureMetadata<SamplingStrategy> {

    private static volatile FallbackStrategy configuredFallback = FallbackStrategy.EXCEPTION;
    private static volatile List<DoubleVariantEntry> configuredVariants = List.of();

    public static void setFallbackStrategy(FallbackStrategy strategy) {
        configuredFallback = strategy;
    }

    public static void setVariants(List<DoubleVariantEntry> variants) {
        configuredVariants = variants;
    }

    public static void reset() {
        configuredFallback = FallbackStrategy.EXCEPTION;
        configuredVariants = List.of();
    }

    @Override
    public Class<SamplingStrategy> featureType() {
        return SamplingStrategy.class;
    }

    @Override
    public String flagKey() {
        return "sampling-ratio";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return configuredFallback;
    }

    @Override
    public Map<String, Supplier<SamplingStrategy>> variantSuppliers() {
        // Not used directly; createProxy uses configuredVariants instead
        return Map.of();
    }

    @Override
    public Supplier<SamplingStrategy> defaultVariantSupplier() {
        return null;
    }

    @Override
    public SamplingStrategy createProxy(FlagProvider flagProvider,
                                        Map<String, Supplier<SamplingStrategy>> variants,
                                        Supplier<SamplingStrategy> defaultVariant) {
        return new SamplingStrategyProxy(flagProvider, configuredVariants, defaultVariant, configuredFallback);
    }
}
