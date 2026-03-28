package com.flagzen.env;

import com.flagzen.keymapping.FlagKeyFormat;
import com.flagzen.keymapping.FlagKeyFormats;
import com.flagzen.keymapping.FlagKeyParser;
import com.flagzen.keymapping.FlagKeyParsers;
import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link FlagProvider} that resolves flag values from environment variables.
 *
 * <p>Environment variables are eagerly loaded at construction time and stored
 * in an immutable map. The default configuration uses the "FLAGZEN_" prefix
 * with screaming-snake-case parsing and kebab-case formatting.
 *
 * <p>Example: environment variable {@code FLAGZEN_CHECKOUT_FLOW=CLASSIC}
 * resolves to flag key {@code checkout-flow} with value {@code "CLASSIC"}.
 */
public final class EnvironmentVariableFlagProvider implements FlagProvider {

    private final Map<String, String> flagMap;

    /**
     * Creates a provider with default configuration.
     * Required by {@link java.util.ServiceLoader} for automatic discovery.
     *
     * <p>Equivalent to calling {@link #create()}.
     */
    public EnvironmentVariableFlagProvider() {
        this(builder().buildFlagMap());
    }

    private EnvironmentVariableFlagProvider(Map<String, String> flagMap) {
        this.flagMap = flagMap;
    }

    /**
     * Creates a provider with default configuration: FLAGZEN_ prefix,
     * screaming-snake-case parser, kebab-case formatter, reading from
     * {@link System#getenv()}.
     *
     * @return a new provider with default configuration
     */
    public static EnvironmentVariableFlagProvider create() {
        return builder().build();
    }

    /**
     * Returns a new builder for constructing a provider with custom configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(flagMap.get(key));
    }

    /**
     * Builder for {@link EnvironmentVariableFlagProvider}.
     */
    public static final class Builder {

        private FlagKeyParser parser = FlagKeyParsers.screamingSnakeCase("FLAGZEN_");
        private FlagKeyFormat formatter = FlagKeyFormats.kebabCase();
        private Supplier<Map<String, String>> environmentSource = System::getenv;

        private Builder() {
        }

        /**
         * Sets the parser used to extract key segments from environment variable names.
         *
         * @param parser the parser to use
         * @return this builder
         */
        public Builder parser(FlagKeyParser parser) {
            this.parser = parser;
            return this;
        }

        /**
         * Sets the formatter used to produce flag key strings from segments.
         *
         * @param formatter the formatter to use
         * @return this builder
         */
        public Builder formatter(FlagKeyFormat formatter) {
            this.formatter = formatter;
            return this;
        }

        /**
         * Sets the source of environment variables. Defaults to {@link System#getenv()}.
         *
         * @param environmentSource a supplier providing the environment variable map
         * @return this builder
         */
        public Builder environmentSource(Supplier<Map<String, String>> environmentSource) {
            this.environmentSource = environmentSource;
            return this;
        }

        /**
         * Builds the immutable flag map from the configured environment source.
         *
         * @return an immutable map of flag keys to values
         */
        Map<String, String> buildFlagMap() {
            Map<String, String> envVars = environmentSource.get();
            Map<String, String> result = new HashMap<>();
            for (var entry : envVars.entrySet()) {
                parser.parse(entry.getKey()).ifPresent(segments -> {
                    String flagKey = formatter.format(segments);
                    result.put(flagKey, entry.getValue());
                });
            }
            return Map.copyOf(result);
        }

        /**
         * Builds the provider, eagerly loading and mapping all environment variables.
         *
         * @return a new provider with the configured settings
         */
        public EnvironmentVariableFlagProvider build() {
            return new EnvironmentVariableFlagProvider(buildFlagMap());
        }
    }
}
