package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: variant activated when sampling-ratio flag ~= 0.5.
 */
public class MediumSampling implements SamplingStrategy {

    @Override
    public String execute() {
        return "MediumSampling";
    }
}
