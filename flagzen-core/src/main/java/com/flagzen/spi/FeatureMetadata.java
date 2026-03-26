package com.flagzen.spi;

import com.flagzen.FallbackStrategy;

import java.util.Map;
import java.util.function.Supplier;

/**
 * SPI for compile-time generated feature metadata.
 * One implementation is generated per {@code @Feature} interface,
 * discovered at runtime via {@link java.util.ServiceLoader}.
 *
 * @param <T> the feature interface type
 */
public interface FeatureMetadata<T> {

    /**
     * Returns the feature interface type.
     */
    Class<T> featureType();

    /**
     * Returns the flag key used to resolve the active variant.
     */
    String flagKey();

    /**
     * Returns the fallback strategy for unmatched variants.
     */
    FallbackStrategy fallbackStrategy();

    /**
     * Returns the variant suppliers keyed by variant value.
     */
    Map<String, Supplier<T>> variantSuppliers();

    /**
     * Returns the supplier for the default variant, or null if none.
     */
    Supplier<T> defaultVariantSupplier();

    /**
     * Creates a dispatch proxy wired to the given flag provider.
     *
     * @param flagProvider the provider supplying flag values
     * @param variants map of variant value to variant supplier
     * @param defaultVariant supplier for the default variant, or null if none
     * @return a proxy instance that dispatches to the active variant
     */
    T createProxy(FlagProvider flagProvider, Map<String, Supplier<T>> variants, Supplier<T> defaultVariant);
}
