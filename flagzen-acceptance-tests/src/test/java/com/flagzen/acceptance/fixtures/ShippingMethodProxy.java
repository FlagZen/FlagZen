package com.flagzen.acceptance.fixtures;

import com.flagzen.EvaluationContext;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Hand-crafted proxy for ShippingMethod acceptance tests.
 */
class ShippingMethodProxy implements ShippingMethod {

    private final FlagProvider flagProvider;
    private final Map<String, Supplier<ShippingMethod>> variants;
    private final Supplier<ShippingMethod> defaultVariant;

    ShippingMethodProxy(FlagProvider flagProvider,
                        Map<String, Supplier<ShippingMethod>> variants,
                        Supplier<ShippingMethod> defaultVariant) {
        this.flagProvider = flagProvider;
        this.variants = variants;
        this.defaultVariant = defaultVariant;
    }

    @Override
    public String execute() {
        return resolveVariant().execute();
    }

    private ShippingMethod resolveVariant() {
        EvaluationContext context = FlagContext.current();
        Optional<String> flagValue = (context != null)
                ? flagProvider.getString("shipping-method", context)
                : flagProvider.getString("shipping-method");
        String value = flagValue.orElse(null);
        if (value != null) {
            Supplier<ShippingMethod> supplier = variants.get(value);
            if (supplier != null) {
                return supplier.get();
            }
        }
        if (defaultVariant != null) {
            return defaultVariant.get();
        }
        if (value == null) {
            throw UnmatchedVariantException.noFlagValue("shipping-method");
        }
        throw new UnmatchedVariantException("shipping-method", value, variants.keySet());
    }
}
