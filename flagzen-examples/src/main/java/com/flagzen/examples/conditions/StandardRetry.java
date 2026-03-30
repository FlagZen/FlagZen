package com.flagzen.examples.conditions;

import com.flagzen.DefaultVariant;

@DefaultVariant(of = RetryStrategy.class)
public class StandardRetry implements RetryStrategy {
    @Override
    public int maxRetries() { return 5; }

    @Override
    public String description() { return "standard"; }
}
