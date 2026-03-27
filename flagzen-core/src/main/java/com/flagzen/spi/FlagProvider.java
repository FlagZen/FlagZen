package com.flagzen.spi;

import com.flagzen.EvaluationContext;

import java.util.Optional;

/**
 * SPI for providing flag values at runtime.
 * Implementations supply the current value for a given flag key.
 */
public interface FlagProvider {

    /**
     * Returns the current value for the given flag key.
     *
     * @param key the flag key to look up
     * @return the flag value, or empty if not set
     */
    Optional<String> getString(String key);

    /**
     * Returns the current value for the given flag key using the provided evaluation context.
     * The default implementation ignores the context and delegates to {@link #getString(String)},
     * ensuring backward compatibility for existing providers.
     *
     * @param key the flag key to look up
     * @param context the evaluation context for targeted resolution
     * @return the flag value, or empty if not set
     */
    default Optional<String> getString(String key, EvaluationContext context) {
        return getString(key);
    }
}
