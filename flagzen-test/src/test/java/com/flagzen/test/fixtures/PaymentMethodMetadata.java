package com.flagzen.test.fixtures;

import com.flagzen.FallbackStrategy;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Hand-crafted FeatureMetadata for PaymentMethod unit tests.
 */
public class PaymentMethodMetadata implements FeatureMetadata<PaymentMethod> {

    @Override
    public Class<PaymentMethod> featureType() {
        return PaymentMethod.class;
    }

    @Override
    public String flagKey() {
        return "payment-method";
    }

    @Override
    public FallbackStrategy fallbackStrategy() {
        return FallbackStrategy.EXCEPTION;
    }

    @Override
    public Map<String, Supplier<PaymentMethod>> variantSuppliers() {
        return Map.of(
                "CREDIT_CARD", CreditCardPayment::new,
                "DEBIT", DebitPayment::new
        );
    }

    @Override
    public Supplier<PaymentMethod> defaultVariantSupplier() {
        return null;
    }

    @Override
    public PaymentMethod createProxy(FlagProvider flagProvider,
                                     Map<String, Supplier<PaymentMethod>> variants,
                                     Supplier<PaymentMethod> defaultVariant) {
        return new PaymentMethodProxy(flagProvider, variants, defaultVariant);
    }
}
