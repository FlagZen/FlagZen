package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for Cucumber acceptance tests.
 * Simulates the dispatch logic the annotation processor would generate,
 * including context-aware flag resolution via {@link FlagContext}.
 */
class CheckoutFlowProxy implements CheckoutFlow {

    private final FlagProvider flagProvider;
    private final Map<String, Supplier<CheckoutFlow>> variants;
    private final Supplier<CheckoutFlow> defaultVariant;

    CheckoutFlowProxy(FlagProvider flagProvider,
                      Map<String, Supplier<CheckoutFlow>> variants,
                      Supplier<CheckoutFlow> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public String execute() {
        return resolveVariant().execute();
    }

    private CheckoutFlow resolveVariant() {
        EvaluationContext context = FlagContext.current();
        Optional<String> flagValue = (context != null)
                ? flagProvider.getString("checkout-flow", context)
                : flagProvider.getString("checkout-flow");
        String value = flagValue.orElse(null);
        if (value != null) {
            Supplier<CheckoutFlow> supplier = variants.get(value);
            if (supplier != null) {
                return supplier.get();
            }
        }
        if (defaultVariant != null) {
            return defaultVariant.get();
        }
        if (value == null) {
            throw UnmatchedVariantException.noFlagValue("checkout-flow");
        }
        throw new UnmatchedVariantException("checkout-flow", value, variants.keySet());
    }
}
