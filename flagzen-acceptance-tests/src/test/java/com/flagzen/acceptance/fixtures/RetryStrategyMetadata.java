package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for INT dispatch acceptance testing.
 * Simulates what the annotation processor would generate for an INT-typed feature.
 * Supports configurable fallback strategy and default variant for different test scenarios.
 */
public class RetryStrategyMetadata implements FeatureMetadata<RetryStrategy> {

    private static volatile FallbackStrategy configuredFallback = FallbackStrategy.EXCEPTION;
    private static volatile Supplier<RetryStrategy> configuredDefault = null;

    public static void setFallbackStrategy(FallbackStrategy strategy) {
        configuredFallback = strategy;
    }

    public static void setDefaultVariant(Supplier<RetryStrategy> supplier) {
        configuredDefault = supplier;
    }

    public static void reset() {
        configuredFallback = FallbackStrategy.EXCEPTION;
        configuredDefault = null;
    }

    @Override
    public Class<RetryStrategy> featureType() {
        return RetryStrategy.class;
    }

    @Override
    public String flagKey() {
        return "max-retries";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return configuredFallback;
    }

    @Override
    public Map<String, Supplier<RetryStrategy>> variantSuppliers() {
        return Map.of(
                "3", ConservativeRetry::new,
                "10", AggressiveRetry::new
        );
    }

    @Override
    public Supplier<RetryStrategy> defaultVariantSupplier() {
        return configuredDefault;
    }

    @Override
    public RetryStrategy createProxy(FlagProvider flagProvider,
                                     Map<String, Supplier<RetryStrategy>> variants,
                                     Supplier<RetryStrategy> defaultVariant) {
        Map<Integer, Supplier<RetryStrategy>> intVariants = new HashMap<>();
        variants.forEach((k, v) -> intVariants.put(Integer.parseInt(k), v));
        return new RetryStrategyProxy(flagProvider, intVariants, defaultVariant, configuredFallback);
    }
}
