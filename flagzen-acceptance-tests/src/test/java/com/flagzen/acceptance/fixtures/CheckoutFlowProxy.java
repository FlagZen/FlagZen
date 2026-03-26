package com.flagzen.acceptance.fixtures;

import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for Cucumber acceptance tests.
 * Simulates the dispatch logic the annotation processor would generate.
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
        String flagValue = flagProvider.getString("checkout-flow").orElse(null);
        if (flagValue != null) {
            Supplier<CheckoutFlow> supplier = variants.get(flagValue);
            if (supplier != null) {
                return supplier.get().execute();
            }
        }
        if (defaultVariant != null) {
            return defaultVariant.get().execute();
        }
        if (flagValue == null) {
            throw UnmatchedVariantException.noFlagValue("checkout-flow");
        }
        throw new UnmatchedVariantException("checkout-flow", flagValue);
    }
}
