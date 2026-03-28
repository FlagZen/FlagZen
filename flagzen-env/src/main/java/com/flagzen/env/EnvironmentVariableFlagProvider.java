package com.flagzen.env;

import com.flagzen.keymapping.ConflictStrategy;
import com.flagzen.keymapping.FlagKeyFormat;
import com.flagzen.keymapping.FlagKeyFormats;
import com.flagzen.keymapping.FlagKeyParser;
import com.flagzen.keymapping.FlagKeyParsers;
import com.flagzen.spi.FlagProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

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

    private static final Logger LOGGER =
            Logger.getLogger(EnvironmentVariableFlagProvider.class.getName());

    private final Map<String, String> flagMap;
    private final Set<String> conflictedKeys;
    private final Set<String> warnedKeys;
    private final Consumer<String> warningConsumer;

    /**
     * Creates a provider with default configuration.
     * Required by {@link java.util.ServiceLoader} for automatic discovery.
     *
     * <p>Equivalent to calling {@link #create()}.
     */
    public EnvironmentVariableFlagProvider() {
        this(builder().buildResult());
    }

    private EnvironmentVariableFlagProvider(BuildResult result) {
        this.flagMap = result.flagMap();
        this.conflictedKeys = result.conflictedKeys();
        this.warnedKeys = ConcurrentHashMap.newKeySet();
        this.warningConsumer = result.warningConsumer();
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
        if (conflictedKeys.contains(key) && warnedKeys.add(key)) {
            warningConsumer.accept(
                    "Flag key '" + key + "' was mapped from multiple environment variables"
            );
        }
        return Optional.ofNullable(flagMap.get(key));
    }

    /**
     * Builder for {@link EnvironmentVariableFlagProvider}.
     */
    public static final class Builder {

        private final List<FlagKeyParser> parsers = new ArrayList<>();
        private final List<FlagKeyFormat> formatters = new ArrayList<>();
        private Supplier<Map<String, String>> environmentSource = System::getenv;
        private ConflictStrategy conflictStrategy;
        private Consumer<String> warningConsumer;

        private Builder() {
        }

        /**
         * Adds a parser used to extract key segments from environment variable names.
         * Multiple parsers can be added; each environment variable is tried against
         * all parsers.
         *
         * @param parser the parser to add
         * @return this builder
         */
        public Builder parser(FlagKeyParser parser) {
            this.parsers.add(parser);
            return this;
        }

        /**
         * Adds a formatter used to produce flag key strings from segments.
         * Multiple formatters can be added; each parsed result is formatted
         * by all formatters, potentially producing multiple flag keys.
         *
         * @param formatter the formatter to add
         * @return this builder
         */
        public Builder formatter(FlagKeyFormat formatter) {
            this.formatters.add(formatter);
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
         * Sets the conflict strategy, overriding the cardinality-based default.
         *
         * @param strategy the conflict strategy to use
         * @return this builder
         */
        public Builder onConflict(ConflictStrategy strategy) {
            this.conflictStrategy = strategy;
            return this;
        }

        /**
         * Sets the consumer for warning messages. Defaults to JUL logger warning.
         *
         * @param consumer the warning consumer
         * @return this builder
         */
        public Builder warningConsumer(Consumer<String> consumer) {
            this.warningConsumer = consumer;
            return this;
        }

        ConflictStrategy effectiveConflictStrategy() {
            if (conflictStrategy != null) {
                return conflictStrategy;
            }
            int parserCount = parsers.isEmpty() ? 1 : parsers.size();
            int formatterCount = formatters.isEmpty() ? 1 : formatters.size();
            if (parserCount > 1 && formatterCount > 1) {
                return ConflictStrategy.ERROR;
            }
            return ConflictStrategy.WARN;
        }

        /**
         * Builds the immutable flag map from the configured environment source.
         *
         * @return an immutable map of flag keys to values
         */
        Map<String, String> buildFlagMap() {
            return buildResult().flagMap();
        }

        BuildResult buildResult() {
            List<FlagKeyParser> effectiveParsers = parsers.isEmpty()
                    ? List.of(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                    : List.copyOf(parsers);
            List<FlagKeyFormat> effectiveFormatters = formatters.isEmpty()
                    ? List.of(FlagKeyFormats.kebabCase())
                    : List.copyOf(formatters);
            Consumer<String> effectiveWarningConsumer = warningConsumer != null
                    ? warningConsumer
                    : LOGGER::warning;
            ConflictStrategy strategy = effectiveConflictStrategy();

            Map<String, String> envVars = environmentSource.get();
            Map<String, String> result = new HashMap<>();
            Map<String, String> flagKeyToEnvVar = new HashMap<>();
            Set<String> conflicted = ConcurrentHashMap.newKeySet();

            for (var entry : envVars.entrySet()) {
                String envVarName = entry.getKey();
                String envVarValue = entry.getValue();

                for (FlagKeyParser parser : effectiveParsers) {
                    parser.parse(envVarName).ifPresent(segments -> {
                        for (FlagKeyFormat formatter : effectiveFormatters) {
                            String flagKey = formatter.format(segments);
                            String previousEnvVar = flagKeyToEnvVar.get(flagKey);
                            if (previousEnvVar != null
                                    && !previousEnvVar.equals(envVarName)) {
                                handleConflict(
                                        strategy,
                                        flagKey,
                                        previousEnvVar,
                                        envVarName,
                                        effectiveWarningConsumer,
                                        conflicted
                                );
                            }
                            flagKeyToEnvVar.put(flagKey, envVarName);
                            result.put(flagKey, envVarValue);
                        }
                    });
                }
            }

            return new BuildResult(
                    Map.copyOf(result),
                    Collections.unmodifiableSet(conflicted),
                    effectiveWarningConsumer
            );
        }

        private void handleConflict(
                ConflictStrategy strategy,
                String flagKey,
                String previousEnvVar,
                String newEnvVar,
                Consumer<String> warningConsumer,
                Set<String> conflicted
        ) {
            String message = "Flag key '" + flagKey
                    + "' mapped from env var '" + newEnvVar
                    + "' overrides previous mapping from '" + previousEnvVar + "'";
            if (strategy == ConflictStrategy.ERROR) {
                throw new IllegalStateException(message);
            }
            warningConsumer.accept(message);
            conflicted.add(flagKey);
        }

        /**
         * Builds the provider, eagerly loading and mapping all environment variables.
         *
         * @return a new provider with the configured settings
         */
        public EnvironmentVariableFlagProvider build() {
            return new EnvironmentVariableFlagProvider(buildResult());
        }
    }

    record BuildResult(
            Map<String, String> flagMap,
            Set<String> conflictedKeys,
            Consumer<String> warningConsumer
    ) {
    }
}
