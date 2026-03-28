package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for ShippingMethod acceptance tests.
 */
public class ShippingMethodMetadata implements FeatureMetadata<ShippingMethod> {

    private static volatile Supplier<ShippingMethod> configuredDefaultVariant = null;

    /**
     * Configures a default variant supplier for testing.
     *
     * @param supplier the default variant supplier
     */
    public static void setDefaultVariant(Supplier<ShippingMethod> supplier) {
        configuredDefaultVariant = supplier;
    }

    /**
     * Resets any configured default variant.
     */
    public static void reset() {
        configuredDefaultVariant = null;
    }

    @Override
    public Class<ShippingMethod> featureType() {
        return ShippingMethod.class;
    }

    @Override
    public String flagKey() {
        return "shipping-method";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<ShippingMethod>> variantSuppliers() {
        return Map.of(
                "STANDARD", StandardShipping::new,
                "EXPRESS", ExpressShipping::new
        );
    }

    @Override
    public Supplier<ShippingMethod> defaultVariantSupplier() {
        return configuredDefaultVariant;
    }

    @Override
    public ShippingMethod createProxy(FlagProvider flagProvider,
                                      Map<String, Supplier<ShippingMethod>> variants,
                                      Supplier<ShippingMethod> defaultVariant) {
        return new ShippingMethodProxy(flagProvider, variants, defaultVariant);
    }
}
