package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when rate-limit flag = 1000.
 */
public class LowVolumeLimit implements RateLimiter {

    @Override
    public String execute() {
        return "LowVolumeLimit";
    }
}
