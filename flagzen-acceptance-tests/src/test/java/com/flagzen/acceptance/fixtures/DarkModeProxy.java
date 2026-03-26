package com.flagzen.acceptance.fixtures;

import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for DarkMode acceptance tests.
 * Simulates the dispatch logic the annotation processor would generate.
 */
class DarkModeProxy implements DarkMode {

    private final FlagProvider flagProvider;
    private final Map<String, Supplier<DarkMode>> variants;
    private final Supplier<DarkMode> defaultVariant;

    DarkModeProxy(FlagProvider flagProvider,
                  Map<String, Supplier<DarkMode>> variants,
                  Supplier<DarkMode> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public void apply() {
        String flagValue = flagProvider.getString("dark-mode").orElse(null);
        if (flagValue != null) {
            Supplier<DarkMode> supplier = variants.get(flagValue);
            if (supplier != null) {
                supplier.get().apply();
                return;
            }
        }
        if (defaultVariant != null) {
            defaultVariant.get().apply();
            return;
        }
        if (flagValue == null) {
            throw UnmatchedVariantException.noFlagValue("dark-mode");
        }
        throw new UnmatchedVariantException("dark-mode", flagValue);
    }
}
