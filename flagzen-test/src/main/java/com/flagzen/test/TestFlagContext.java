package com.flagzen.test;

import com.flagzen.FeatureDispatcher;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;

/**
 * Programmatic API for pinning flag values in tests.
 * Wraps an {@link InMemoryFlagProvider} and a test-scoped {@link FeatureDispatcher}.
 *
 * <p>Use {@link #pin(String, String)} to set flag values programmatically,
 * or rely on {@link PinFlag} annotations processed by {@link FlagZenExtension}.</p>
 */
public class TestFlagContext {

    private final InMemoryFlagProvider flagProvider;
    private final FeatureDispatcher dispatcher;

    TestFlagContext(InMemoryFlagProvider flagProvider) {
        this.flagProvider = flagProvider;
        this.dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    /**
     * Pins a flag key to a specific value for the duration of the test.
     *
     * @param key the flag key
     * @param value the variant value
     */
    public void pin(String key, String value) {
        flagProvider.set(key, value);
    }

    /**
     * Resolves a feature interface to its active variant proxy.
     *
     * @param featureType the feature interface class
     * @param <T> the feature type
     * @return the resolved proxy
     */
    public <T> T resolve(Class<T> featureType) {
        return dispatcher.resolve(featureType);
    }
}
