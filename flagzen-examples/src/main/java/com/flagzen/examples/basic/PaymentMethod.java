package com.flagzen.examples.basic;

import com.flagzen.Feature;

@Feature("payment-method")
public interface PaymentMethod {
    String process(double amount);
}
