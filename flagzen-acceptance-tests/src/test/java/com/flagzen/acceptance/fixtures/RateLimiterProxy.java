package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Supplier;

/**
 * Hand-crafted LONG proxy for RateLimiter acceptance testing.
 * Simulates the dispatch logic the annotation processor would generate for LONG-typed features.
 */
class RateLimiterProxy implements RateLimiter {

    private final FlagProvider flagProvider;
    private final Map<Long, Supplier<RateLimiter>> variants;
    private final Supplier<RateLimiter> defaultVariant;

    RateLimiterProxy(FlagProvider flagProvider,
                     Map<Long, Supplier<RateLimiter>> variants,
                     Supplier<RateLimiter> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public String execute() {
        return resolveVariant().execute();
    }

    private RateLimiter resolveVariant() {
        EvaluationContext context = FlagContext.current();
        OptionalLong flagValue = (context != null)
                ? flagProvider.getLong("rate-limit", context)
                : flagProvider.getLong("rate-limit");

        if (flagValue.isPresent()) {
            Supplier<RateLimiter> supplier = variants.get(flagValue.getAsLong());
            if (supplier != null) {
                return supplier.get();
            }
        }

        if (defaultVariant != null) {
            return defaultVariant.get();
        }

        if (flagValue.isEmpty()) {
            throw UnmatchedVariantException.noFlagValue("rate-limit");
        }
        throw new UnmatchedVariantException("rate-limit", String.valueOf(flagValue.getAsLong()), variants.keySet());
    }
}
