package com.flagzen.acceptance.steps;

import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.PaymentMethod;

/**
 * Thread-safe holder for sharing the resolved proxy between step definition classes.
 * Reset before each scenario via {@link ScenarioHooks}.
 */
final class SharedProxyHolder {

    private static final ThreadLocal<CheckoutFlow> PROXY = new ThreadLocal<>();
    private static final ThreadLocal<PaymentMethod> PAYMENT_PROXY = new ThreadLocal<>();

    private SharedProxyHolder() {
    }

    static void set(CheckoutFlow proxy) {
        PROXY.set(proxy);
    }

    static CheckoutFlow get() {
        return PROXY.get();
    }

    static void setPayment(PaymentMethod proxy) {
        PAYMENT_PROXY.set(proxy);
    }

    static PaymentMethod getPayment() {
        return PAYMENT_PROXY.get();
    }

    static void reset() {
        PROXY.remove();
        PAYMENT_PROXY.remove();
    }
}
