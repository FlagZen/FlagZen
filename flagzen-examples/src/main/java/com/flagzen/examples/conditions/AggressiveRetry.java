package com.flagzen.examples.conditions;

import com.flagzen.Condition;
import com.flagzen.Variant;

@Variant(when = @Condition(matches = HighRetryRange.class), of = RetryStrategy.class, order = 2)
public class AggressiveRetry implements RetryStrategy {
    @Override
    public int maxRetries() { return 10; }

    @Override
    public String description() { return "aggressive"; }
}
