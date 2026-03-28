package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant for throttled rate limiting.
 */
public class ThrottledRate implements RateLimiter {

    @Override
    public String execute() {
        return "ThrottledRate";
    }
}
