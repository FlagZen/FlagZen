package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when rate-limit flag = 50000.
 */
public class HighVolumeLimit implements RateLimiter {

    @Override
    public String execute() {
        return "HighVolumeLimit";
    }
}
