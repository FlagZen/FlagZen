package com.flagzen.internal;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.FlagZenException;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import com.flagzen.spi.ContextAccessor;

import java.util.Comparator;
import java.util.List;
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
    private final List<ContextAccessor> contextAccessors;

    public DefaultFeatureDispatcher(FlagProvider flagProvider) {
        this(flagProvider, List.of());
    }

    /**
     * Creates a dispatcher with explicit context accessors.
     *
     * @param flagProvider the flag provider
     * @param contextAccessors the context accessors, consulted in priority order
     */
    public DefaultFeatureDispatcher(FlagProvider flagProvider, ContextAccessor... contextAccessors) {
        this(flagProvider, List.of(contextAccessors));
    }

    private DefaultFeatureDispatcher(FlagProvider flagProvider, List<ContextAccessor> contextAccessors) {
        this.flagProvider = flagProvider;
        this.contextAccessors = contextAccessors.stream()
                .sorted(Comparator.comparingInt(ContextAccessor::priority))
                .toList();
        this.metadataByType = discoverMetadata();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> featureType) {
        resolveContextFromAccessors();
        return (T) proxyCache.computeIfAbsent(featureType, this::createProxy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> featureType, EvaluationContext context) {
        FlagContext.set(context);
        return (T) proxyCache.computeIfAbsent(featureType, this::createProxy);
    }

    /**
     * Consults registered {@link ContextAccessor}s in priority order.
     * If any accessor provides a context, sets it in {@link FlagContext},
     * overriding any scoped context. If no accessor provides context,
     * the existing scoped context (if any) is left untouched.
     */
    private void resolveContextFromAccessors() {
        for (ContextAccessor accessor : contextAccessors) {
            var context = accessor.getContext();
            if (context.isPresent()) {
                FlagContext.set(context.get());
                return;
            }
        }
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
