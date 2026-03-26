package com.flagzen.test.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for testing support acceptance tests.
 * Includes CLASSIC and PREMIUM variants.
 */
public class CheckoutFlowMetadata implements FeatureMetadata<CheckoutFlow> {

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
                "PREMIUM", PremiumCheckout::new
        );
    }

    @Override
    public Supplier<CheckoutFlow> defaultVariantSupplier() {
        return null;
    }

    @Override
    public CheckoutFlow createProxy(FlagProvider flagProvider,
                                    Map<String, Supplier<CheckoutFlow>> variants,
                                    Supplier<CheckoutFlow> defaultVariant) {
        return new CheckoutFlowProxy(flagProvider, variants, defaultVariant);
    }
}
