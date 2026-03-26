package com.flagzen.acceptance.steps;

import io.cucumber.java.Before;

/**
 * Cucumber hooks for scenario lifecycle management.
 */
public class ScenarioHooks {

    @Before
    public void resetSharedState() {
        SharedProxyHolder.reset();
    }
}
