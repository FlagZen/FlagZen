package com.flagzen;

import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.FlagProvider;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Entry point for FlagZen. Creates and configures a {@link FeatureDispatcher}.
 */
public final class FlagZen {

    private FlagZen() {
        // factory class
    }

    /**
     * Creates a dispatcher with the given configuration.
     *
     * @param configurer consumer that configures the dispatcher
     * @return a configured {@link FeatureDispatcher}
     */
    public static FeatureDispatcher dispatcher(Consumer<FlagZenConfiguration> configurer) {
        FlagZenConfiguration config = new FlagZenConfiguration();
        configurer.accept(config);
        return new DefaultFeatureDispatcher(config.flagProvider());
    }

    /**
     * Configuration for FlagZen.
     */
    public static final class FlagZenConfiguration {
        private FlagProvider flagProvider;

        FlagZenConfiguration() {
        }

        /**
         * Sets the flag provider.
         *
         * @param flagProvider the provider to use
         * @return this configuration for chaining
         */
        public FlagZenConfiguration provider(FlagProvider flagProvider) {
            this.flagProvider = Objects.requireNonNull(flagProvider);
            return this;
        }

        FlagProvider flagProvider() {
            if (flagProvider == null) {
                throw new FlagZenException("No FlagProvider configured. Call provider() on FlagZenConfiguration.");
            }
            return flagProvider;
        }
    }
}
