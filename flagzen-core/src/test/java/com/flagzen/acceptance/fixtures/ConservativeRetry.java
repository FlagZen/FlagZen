package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when max-retries flag = 3.
 */
public class ConservativeRetry implements RetryStrategy {

    @Override
    public int maxRetries() {
        return 3;
    }
}
