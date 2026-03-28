package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant for unlimited rate limiting.
 */
public class UnlimitedRate implements RateLimiter {

    @Override
    public String execute() {
        return "UnlimitedRate";
    }
}
