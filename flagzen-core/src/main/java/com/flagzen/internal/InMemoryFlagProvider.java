package com.flagzen.internal;

import com.flagzen.spi.FlagProvider;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory flag provider backed by a ConcurrentHashMap.
 * Mutable and thread-safe. Useful for testing and local development.
 */
public class InMemoryFlagProvider implements FlagProvider {

    private final ConcurrentMap<String, String> flags = new ConcurrentHashMap<>();

    /**
     * Sets a flag value.
     *
     * @param key the flag key
     * @param value the flag value
     */
    public void set(String key, String value) {
        flags.put(key, value);
    }

    @Override
    public Optional<String> getString(String key) {
        return Optional.ofNullable(flags.get(key));
    }
}
