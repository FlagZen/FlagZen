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
    private static volatile Map<String, Supplier<CheckoutFlow>> multiValueVariants = null;

    public static void setDefaultVariant(Supplier<CheckoutFlow> supplier) {
        configuredDefaultVariant = supplier;
    }

    /**
     * Configures multi-value variant mappings for acceptance test scenarios.
     * When set, overrides the default variantSuppliers().
     */
    public static void setMultiValueVariants(Map<String, Supplier<CheckoutFlow>> variants) {
        multiValueVariants = variants;
    }

    public static void reset() {
        configuredDefaultVariant = null;
        multiValueVariants = null;
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
        if (multiValueVariants != null) {
            return multiValueVariants;
        }
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
