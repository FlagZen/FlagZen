package com.flagzen.acceptance.steps;

import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.DarkModeMetadata;
import io.cucumber.java.Before;

/**
 * Cucumber hooks for scenario lifecycle management.
 */
public class ScenarioHooks {

    @Before
    public void resetSharedState() {
        SharedProxyHolder.reset();
        DarkModeMetadata.reset();
        CheckoutFlowMetadata.reset();
    }
}
