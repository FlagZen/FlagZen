package com.flagzen.togglz;

import com.flagzen.spi.FlagProvider;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;

import java.util.Optional;

/**
 * A {@link FlagProvider} that delegates flag resolution to the Togglz feature toggle library.
 *
 * <p>This adapter bridges FlagZen's typed flag resolution with Togglz's feature management.
 * Boolean flags are resolved directly via {@code FeatureState.isEnabled()}.
 *
 * <p>Feature key lookup is case-insensitive. Keys are matched against registered Togglz
 * feature names using locale-independent upper-case comparison.
 *
 * <p>Usage:
 * <pre>{@code
 * FeatureManager manager = ...;
 * FlagProvider provider = TogglzFlagProvider.create(manager);
 * Optional<Boolean> enabled = provider.getBoolean("DARK_MODE");
 * }</pre>
 */
public final class TogglzFlagProvider implements FlagProvider {

    private final FeatureManager featureManager;
    private final FeatureLookup featureLookup;

    private TogglzFlagProvider(FeatureManager featureManager) {
        this.featureManager = featureManager;
        this.featureLookup = FeatureLookup.create(featureManager);
    }

    /**
     * Creates a provider using the given Togglz feature manager.
     *
     * @param featureManager the Togglz feature manager to delegate to
     * @return a new {@link TogglzFlagProvider}
     */
    public static TogglzFlagProvider create(FeatureManager featureManager) {
        return new TogglzFlagProvider(featureManager);
    }

    @Override
    public Optional<String> getString(String key) {
        Optional<Feature> feature = featureLookup.resolve(key);
        if (feature.isEmpty()) {
            return Optional.empty();
        }
        FeatureState state = featureManager.getFeatureState(feature.get());
        if (state == null) {
            return Optional.empty();
        }
        String parameter = state.getParameter("value");
        if (parameter != null && !parameter.isEmpty()) {
            return Optional.of(parameter);
        }
        return Optional.of(String.valueOf(state.isEnabled()));
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        Optional<Feature> feature = featureLookup.resolve(key);
        if (feature.isEmpty()) {
            return Optional.empty();
        }
        FeatureState state = featureManager.getFeatureState(feature.get());
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(state.isEnabled());
    }
}
