package com.flagzen.internal;

import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagZenException;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Default implementation of {@link FeatureDispatcher}.
 * Discovers generated {@link FeatureMetadata} via {@link ServiceLoader}
 * and creates dispatch proxies cached as singletons per feature type.
 */
public class DefaultFeatureDispatcher implements FeatureDispatcher {

    private final FlagProvider flagProvider;
    private final ConcurrentMap<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, FeatureMetadata<?>> metadataByType;

    public DefaultFeatureDispatcher(FlagProvider flagProvider) {
        this.flagProvider = flagProvider;
        this.metadataByType = discoverMetadata();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> featureType) {
        return (T) proxyCache.computeIfAbsent(featureType, this::createProxy);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object createProxy(Class<?> featureType) {
        FeatureMetadata metadata = metadataByType.get(featureType);
        if (metadata == null) {
            throw new FlagZenException(
                    "No FeatureMetadata found for " + featureType.getName()
                            + ". Ensure the @Feature annotation is processed and the metadata is registered.");
        }
        return metadata.createProxy(flagProvider, metadata.variantSuppliers(), metadata.defaultVariantSupplier());
    }

    private static Map<Class<?>, FeatureMetadata<?>> discoverMetadata() {
        ConcurrentMap<Class<?>, FeatureMetadata<?>> map = new ConcurrentHashMap<>();
        @SuppressWarnings("rawtypes")
        ServiceLoader<FeatureMetadata> loader = ServiceLoader.load(FeatureMetadata.class);
        for (FeatureMetadata<?> metadata : loader) {
            map.put(metadata.featureType(), metadata);
        }
        return map;
    }
}
