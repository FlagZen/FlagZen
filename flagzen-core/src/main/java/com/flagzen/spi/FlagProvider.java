package com.flagzen.spi;

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
}
