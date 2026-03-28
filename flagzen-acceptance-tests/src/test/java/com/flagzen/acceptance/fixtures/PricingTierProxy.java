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
 * Hand-crafted INT proxy for PricingTier acceptance testing.
 * Simulates the dispatch logic the annotation processor would generate for INT-typed features.
 */
class PricingTierProxy implements PricingTier {

    private final FlagProvider flagProvider;
    private final Map<Integer, Supplier<PricingTier>> variants;
    private final Supplier<PricingTier> defaultVariant;

    PricingTierProxy(FlagProvider flagProvider,
                     Map<Integer, Supplier<PricingTier>> variants,
                     Supplier<PricingTier> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public String execute() {
        PricingTier delegate = resolveVariant();
        if (delegate == null) {
            return null;
        }
        return delegate.execute();
    }

    private PricingTier resolveVariant() {
        EvaluationContext context = FlagContext.current();
        OptionalInt flagValue = (context != null)
                ? flagProvider.getInt("pricing-tier", context)
                : flagProvider.getInt("pricing-tier");

        if (flagValue.isPresent()) {
            Supplier<PricingTier> supplier = variants.get(flagValue.getAsInt());
            if (supplier != null) {
                return supplier.get();
            }
        }

        if (defaultVariant != null) {
            return defaultVariant.get();
        }

        if (flagValue.isEmpty()) {
            throw UnmatchedVariantException.noFlagValue("pricing-tier");
        }
        throw new UnmatchedVariantException("pricing-tier", String.valueOf(flagValue.getAsInt()), variants.keySet());
    }
}
