package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Hand-crafted DOUBLE proxy for SamplingStrategy acceptance testing.
 * Simulates the dispatch logic the annotation processor would generate for DOUBLE-typed features.
 * Uses approximate matching with configurable tolerance per variant.
 */
class SamplingStrategyProxy implements SamplingStrategy {

    private final FlagProvider flagProvider;
    private final List<DoubleVariantEntry> variants;
    private final Supplier<SamplingStrategy> defaultVariant;
    private final FallbackStrategy fallbackStrategy;

    SamplingStrategyProxy(FlagProvider flagProvider,
                          List<DoubleVariantEntry> variants,
                          Supplier<SamplingStrategy> defaultVariant,
                          FallbackStrategy fallbackStrategy) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public String execute() {
        return resolveVariant().execute();
    }

    private SamplingStrategy resolveVariant() {
        EvaluationContext context = FlagContext.current();
        OptionalDouble flagValue = (context != null)
                ? flagProvider.getDouble("sampling-ratio", context)
                : flagProvider.getDouble("sampling-ratio");

        if (flagValue.isPresent()) {
            double val = flagValue.getAsDouble();
            for (DoubleVariantEntry entry : variants) {
                if (entry.matches(val)) {
                    return entry.supplier().get();
                }
            }
        }

        if (defaultVariant != null) {
            return defaultVariant.get();
        }

        if (fallbackStrategy == FallbackStrategy.EXCEPTION) {
            if (flagValue.isEmpty()) {
                throw UnmatchedVariantException.noFlagValue("sampling-ratio");
            }
            throw new UnmatchedVariantException("sampling-ratio", String.valueOf(flagValue.getAsDouble()),
                    variants.stream().map(e -> String.valueOf(e.value())).toList());
        }

        throw new UnmatchedVariantException("sampling-ratio", String.valueOf(flagValue.orElse(Double.NaN)),
                variants.stream().map(e -> String.valueOf(e.value())).toList());
    }
}
