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
 */
public class RetryStrategyMetadata implements FeatureMetadata<RetryStrategy> {

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
        return FallbackStrategy.EXCEPTION;
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
        return null;
    }

    @Override
    public RetryStrategy createProxy(FlagProvider flagProvider,
                                     Map<String, Supplier<RetryStrategy>> variants,
                                     Supplier<RetryStrategy> defaultVariant) {
        Map<Integer, Supplier<RetryStrategy>> intVariants = new HashMap<>();
        variants.forEach((k, v) -> intVariants.put(Integer.parseInt(k), v));
        return new RetryStrategyProxy(flagProvider, intVariants, defaultVariant);
    }
}
