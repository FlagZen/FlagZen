package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when max-retries flag = 10.
 */
public class AggressiveRetry implements RetryStrategy {

    @Override
    public String execute() {
        return "AggressiveRetry";
    }
}
