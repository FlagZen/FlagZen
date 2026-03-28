package com.flagzen.spring;

import com.flagzen.spi.FlagProvider;

import java.util.Optional;

/**
 * A no-op flag provider that always returns empty.
 * Used as fallback when no {@link FlagProvider} bean is defined in the application context.
 */
class NoOpFlagProvider implements FlagProvider {

    @Override
    public Optional<String> getString(String key) {
        return Optional.empty();
    }
}
