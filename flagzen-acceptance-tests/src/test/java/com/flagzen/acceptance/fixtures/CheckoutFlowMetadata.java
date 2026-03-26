package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for Cucumber acceptance tests.
 * Includes all variants used across walking skeleton scenarios.
 */
public class CheckoutFlowMetadata implements FeatureMetadata<CheckoutFlow> {

    private static volatile Supplier<CheckoutFlow> configuredDefaultVariant = null;

    public static void setDefaultVariant(Supplier<CheckoutFlow> supplier) {
        configuredDefaultVariant = supplier;
    }

    public static void reset() {
        configuredDefaultVariant = null;
    }

    @Override
    public Class<CheckoutFlow> featureType() {
        return CheckoutFlow.class;
    }

    @Override
    public String flagKey() {
        return "checkout-flow";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<CheckoutFlow>> variantSuppliers() {
        return Map.of(
                "CLASSIC", ClassicCheckout::new,
                "STREAMLINED", StreamlinedCheckout::new,
                "PREMIUM", PremiumCheckout::new
        );
    }

    @Override
    public Supplier<CheckoutFlow> defaultVariantSupplier() {
        return configuredDefaultVariant;
    }

    @Override
    public CheckoutFlow createProxy(FlagProvider flagProvider,
                                    Map<String, Supplier<CheckoutFlow>> variants,
                                    Supplier<CheckoutFlow> defaultVariant) {
        return new CheckoutFlowProxy(flagProvider, variants, defaultVariant);
    }
}
