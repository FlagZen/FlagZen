package com.flagzen.acceptance.fixtures;

import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for PaymentMethod acceptance tests.
 * Simulates the dispatch logic the annotation processor would generate.
 */
class PaymentMethodProxy implements PaymentMethod {

    private final FlagProvider flagProvider;
    private final Map<String, Supplier<PaymentMethod>> variants;
    private final Supplier<PaymentMethod> defaultVariant;

    PaymentMethodProxy(FlagProvider flagProvider,
                       Map<String, Supplier<PaymentMethod>> variants,
                       Supplier<PaymentMethod> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public String execute() {
        String flagValue = flagProvider.getString("payment-method").orElse(null);
        if (flagValue != null) {
            Supplier<PaymentMethod> supplier = variants.get(flagValue);
            if (supplier != null) {
                return supplier.get().execute();
            }
        }
        if (defaultVariant != null) {
            return defaultVariant.get().execute();
        }
        if (flagValue == null) {
            throw UnmatchedVariantException.noFlagValue("payment-method");
        }
        throw new UnmatchedVariantException("payment-method", flagValue, variants.keySet());
    }
}
