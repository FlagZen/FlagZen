package com.flagzen.acceptance.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for DarkMode acceptance tests.
 * Simulates the dispatch logic the annotation processor would generate,
 * including NOOP fallback strategy support.
 */
class DarkModeProxy implements DarkMode {

    private final FlagProvider flagProvider;
    private final Map<String, Supplier<DarkMode>> variants;
    private final Supplier<DarkMode> defaultVariant;
    private final FallbackStrategy fallbackStrategy;

    DarkModeProxy(FlagProvider flagProvider,
                  Map<String, Supplier<DarkMode>> variants,
                  Supplier<DarkMode> defaultVariant,
                  FallbackStrategy fallbackStrategy) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public void apply() {
        DarkMode delegate = resolveVariant();
        if (delegate != null) {
            delegate.apply();
        }
    }

    @Override
    public boolean isEnabled() {
        DarkMode delegate = resolveVariant();
        if (delegate != null) {
            return delegate.isEnabled();
        }
        return false;
    }

    private DarkMode resolveVariant() {
        String flagValue = flagProvider.getString("dark-mode").orElse(null);
        if (flagValue != null) {
            Supplier<DarkMode> supplier = variants.get(flagValue);
            if (supplier != null) {
                return supplier.get();
            }
        }
        if (defaultVariant != null) {
            return defaultVariant.get();
        }
        if (fallbackStrategy == FallbackStrategy.NOOP) {
            return null;
        }
        if (flagValue == null) {
            throw UnmatchedVariantException.noFlagValue("dark-mode");
        }
        throw new UnmatchedVariantException("dark-mode", flagValue, variants.keySet());
    }
}
