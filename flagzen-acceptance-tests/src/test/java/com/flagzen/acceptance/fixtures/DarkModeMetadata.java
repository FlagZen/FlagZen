package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for DarkMode acceptance tests.
 * Supports configurable fallback strategy and boolean dispatch mode for testing different scenarios.
 */
public class DarkModeMetadata implements FeatureMetadata<DarkMode> {

    private static volatile FallbackStrategy configuredFallback = FallbackStrategy.EXCEPTION;
    private static volatile boolean booleanDispatchMode = false;

    public static void setFallbackStrategy(FallbackStrategy strategy) {
        configuredFallback = strategy;
    }

    /**
     * Enables boolean dispatch mode for typed variant scenarios.
     * When enabled, DarkModeOn (true) and DarkModeOff (false) are used as variants,
     * and the proxy dispatches using FlagProvider.getBoolean().
     */
    public static void enableBooleanDispatch() {
        booleanDispatchMode = true;
    }

    public static void reset() {
        configuredFallback = FallbackStrategy.EXCEPTION;
        booleanDispatchMode = false;
    }

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
        return configuredFallback;
    }

    @Override
    public Map<String, Supplier<DarkMode>> variantSuppliers() {
        if (booleanDispatchMode) {
            return Map.of(
                    "true", DarkModeOn::new,
                    "false", DarkModeOff::new
            );
        }
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
        if (booleanDispatchMode) {
            Map<Boolean, Supplier<DarkMode>> boolVariants = new HashMap<>();
            variants.forEach((k, v) -> boolVariants.put(Boolean.parseBoolean(k), v));
            return new DarkModeBooleanProxy(flagProvider, boolVariants, defaultVariant, configuredFallback);
        }
        return new DarkModeProxy(flagProvider, variants, defaultVariant, configuredFallback);
    }
}
