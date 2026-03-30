package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Hand-crafted ordered dispatch proxy for condition dispatch acceptance testing.
 * Simulates the proxy generated for features with {@code @Condition}-based variants.
 *
 * <p>Dispatch order:
 * <ol>
 *   <li>order=1: exact match "free" -> FreeTier</li>
 *   <li>order=2: condition IsEnterpriseTier -> EnterpriseTier</li>
 * </ol>
 * Default: StandardTier
 */
class PricingTierProxy implements PricingTier {

    private final FlagProvider flagProvider;
    private final Supplier<PricingTier> defaultVariant;
    private final FallbackStrategy fallbackStrategy;

    // Ordered variant suppliers (baked in at construction)
    private final Supplier<PricingTier> variant0 = FreeTier::new;
    private final Supplier<PricingTier> variant1 = EnterpriseTier::new;

    // Predicate for condition-based variant at order=2
    private final Predicate<String> pred1 = new IsEnterpriseTier();

    PricingTierProxy(FlagProvider flagProvider, Supplier<PricingTier> defaultVariant,
                     FallbackStrategy fallbackStrategy) {
        this.flagProvider = flagProvider;
        this.defaultVariant = defaultVariant;
        this.fallbackStrategy = fallbackStrategy;
    }

    @Override
    public String tierName() {
        return resolveVariant().tierName();
    }

    private PricingTier resolveVariant() {
        EvaluationContext context = FlagContext.current();
        Optional<String> flagValue = (context != null)
                ? flagProvider.getString("pricing-tier", context)
                : flagProvider.getString("pricing-tier");
        String rawValue = flagValue.orElse(null);

        if (rawValue != null) {
            // order=1: exact match
            if (rawValue.equals("free")) {
                return variant0.get();
            }
            // order=2: condition predicate
            if (pred1.test(rawValue)) {
                return variant1.get();
            }
        }

        if (defaultVariant != null) {
            return defaultVariant.get();
        }

        if (rawValue == null) {
            throw UnmatchedVariantException.noFlagValue("pricing-tier");
        }
        throw UnmatchedVariantException.forConditionFeature(
                "pricing-tier", rawValue, "IsEnterpriseTier(order=2)");
    }
}
