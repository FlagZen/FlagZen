package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when sampling-ratio flag ~= 0.12 (overlapping range with LowSampling).
 */
public class NearLowSampling implements SamplingStrategy {

    @Override
    public String execute() {
        return "NearLowSampling";
    }
}
