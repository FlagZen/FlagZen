package com.flagzen.togglz;

import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Case-insensitive lookup cache for Togglz {@link Feature} instances.
 *
 * <p>Builds a mapping lazily on first call from {@code FeatureManager.getFeatures()},
 * keyed by upper-cased feature name for case-insensitive resolution.
 */
final class FeatureLookup {

    private final FeatureManager featureManager;
    private volatile ConcurrentHashMap<String, Feature> cache;

    private FeatureLookup(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    /**
     * Creates a new lookup backed by the given feature manager.
     *
     * @param featureManager the Togglz feature manager providing registered features
     * @return a new {@link FeatureLookup}
     */
    static FeatureLookup create(FeatureManager featureManager) {
        return new FeatureLookup(featureManager);
    }

    /**
     * Resolves a feature by key, case-insensitively.
     *
     * @param key the feature key to look up
     * @return the matching feature, or empty if not registered
     */
    Optional<Feature> resolve(String key) {
        return Optional.ofNullable(ensureCache().get(key.toUpperCase(Locale.ROOT)));
    }

    private ConcurrentHashMap<String, Feature> ensureCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    ConcurrentHashMap<String, Feature> map = new ConcurrentHashMap<>();
                    for (Feature feature : featureManager.getFeatures()) {
                        map.put(feature.name().toUpperCase(Locale.ROOT), feature);
                    }
                    cache = map;
                }
            }
        }
        return cache;
    }
}
