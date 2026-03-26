package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for DarkMode acceptance tests.
 */
public class DarkModeMetadata implements FeatureMetadata<DarkMode> {

    @Override
    public Class<DarkMode> featureType() {
        return DarkMode.class;
    }

    @Override
    public String flagKey() {
        return "dark-mode";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<DarkMode>> variantSuppliers() {
        return Map.of(
                "ENABLED", DarkModeEnabled::new
        );
    }

    @Override
    public Supplier<DarkMode> defaultVariantSupplier() {
        return null;
    }

    @Override
    public DarkMode createProxy(FlagProvider flagProvider,
                                Map<String, Supplier<DarkMode>> variants,
                                Supplier<DarkMode> defaultVariant) {
        return new DarkModeProxy(flagProvider, variants, defaultVariant);
    }
}
