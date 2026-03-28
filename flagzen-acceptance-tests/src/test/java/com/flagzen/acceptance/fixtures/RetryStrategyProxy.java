package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Hand-crafted INT proxy for acceptance testing.
 * Simulates the dispatch logic the annotation processor would generate for INT-typed features.
 */
class RetryStrategyProxy implements RetryStrategy {

    private final FlagProvider flagProvider;
    private final Map<Integer, Supplier<RetryStrategy>> variants;
    private final Supplier<RetryStrategy> defaultVariant;
    private final FallbackStrategy fallbackStrategy;

    RetryStrategyProxy(FlagProvider flagProvider,
                       Map<Integer, Supplier<RetryStrategy>> variants,
                       Supplier<RetryStrategy> defaultVariant) {
        this(flagProvider, variants, defaultVariant, FallbackStrategy.EXCEPTION);
    }

    RetryStrategyProxy(FlagProvider flagProvider,
                       Map<Integer, Supplier<RetryStrategy>> variants,
                       Supplier<RetryStrategy> defaultVariant,
                       FallbackStrategy fallbackStrategy) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public String execute() {
        RetryStrategy delegate = resolveVariant();
        if (delegate == null) {
            return null;
        }
        return delegate.execute();
    }

    private RetryStrategy resolveVariant() {
        EvaluationContext context = FlagContext.current();
        OptionalInt flagValue = (context != null)
                ? flagProvider.getInt("max-retries", context)
                : flagProvider.getInt("max-retries");

        if (flagValue.isPresent()) {
            Supplier<RetryStrategy> supplier = variants.get(flagValue.getAsInt());
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
            throw UnmatchedVariantException.noFlagValue("max-retries");
        }
        throw new UnmatchedVariantException("max-retries", String.valueOf(flagValue.getAsInt()), variants.keySet());
    }
}
