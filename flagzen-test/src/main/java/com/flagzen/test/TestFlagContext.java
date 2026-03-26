package com.flagzen.test;

import com.flagzen.FeatureDispatcher;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
     * Creates a new TestFlagContext for programmatic use.
     * Prefer {@link PinFlag} annotations with {@link FlagZenExtension} for declarative pinning.
     *
     * @return a new context with an empty in-memory flag provider
     */
    public static TestFlagContext create() {
        return new TestFlagContext(new InMemoryFlagProvider());
    }

    /**
     * Creates a TestFlagContext pre-populated with flag values from a classpath properties file.
     *
     * @param classpathResource the classpath resource path to the properties file
     * @return a new context with flags loaded from the properties file
     */
    public static TestFlagContext createFromProperties(String classpathResource) {
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        Properties properties = loadProperties(classpathResource);
        properties.forEach((key, value) -> flagProvider.set(key.toString(), value.toString()));
        return new TestFlagContext(flagProvider);
    }

    private static Properties loadProperties(String classpathResource) {
        Properties properties = new Properties();
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResource);
        if (stream == null) {
            throw new IllegalArgumentException(
                    "Flag source file not found on classpath: " + classpathResource
                            + ". Searched in classpath root and META-INF/.");
        }
        try (stream) {
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load flag source file: " + classpathResource, e);
        }
        return properties;
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
