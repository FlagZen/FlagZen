package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when sampling-ratio flag ~= 0.1.
 */
public class LowSampling implements SamplingStrategy {

    @Override
    public String execute() {
        return "LowSampling";
    }
}
