package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hand-crafted boolean dispatch proxy for DarkMode acceptance testing.
 * Simulates the dispatch logic the annotation processor would generate for BOOLEAN-typed features.
 */
class DarkModeBooleanProxy implements DarkMode {

    private final FlagProvider flagProvider;
    private final Map<Boolean, Supplier<DarkMode>> variants;
    private final Supplier<DarkMode> defaultVariant;
    private final FallbackStrategy fallbackStrategy;

    DarkModeBooleanProxy(FlagProvider flagProvider,
                         Map<Boolean, Supplier<DarkMode>> variants,
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
        EvaluationContext context = FlagContext.current();
        Optional<Boolean> flagValue = (context != null)
                ? flagProvider.getBoolean("dark-mode", context)
                : flagProvider.getBoolean("dark-mode");

        if (flagValue.isPresent()) {
            Supplier<DarkMode> supplier = variants.get(flagValue.get());
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

        if (flagValue.isEmpty()) {
            throw UnmatchedVariantException.noFlagValue("dark-mode");
        }
        throw new UnmatchedVariantException("dark-mode", String.valueOf(flagValue.get()), variants.keySet());
    }
}
