package com.flagzen.examples.conditions;

import com.flagzen.Variant;

@Variant(intValue = 3, of = RetryStrategy.class, order = 1)
public class ConservativeRetry implements RetryStrategy {
    @Override
    public int maxRetries() { return 3; }

    @Override
    public String description() { return "conservative"; }
}
