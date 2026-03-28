package com.flagzen.spring;

import com.flagzen.spi.FlagProvider;

import java.util.Optional;

/**
 * An in-memory flag provider that always returns empty.
 * Used as fallback when no {@link FlagProvider} bean is defined in the application context.
 * Intended for development and testing use only.
 */
class InMemoryFlagProvider implements FlagProvider {

    @Override
    public Optional<String> getString(String key) {
        return Optional.empty();
    }
}
