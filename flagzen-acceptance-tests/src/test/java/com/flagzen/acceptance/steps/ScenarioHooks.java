package com.flagzen.acceptance.steps;

import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.DarkModeMetadata;
import com.flagzen.acceptance.fixtures.PaymentMethodMetadata;
import com.flagzen.acceptance.fixtures.PricingTierMetadata;
import com.flagzen.acceptance.fixtures.RateLimiterMetadata;
import com.flagzen.acceptance.fixtures.RetryStrategyMetadata;
import com.flagzen.acceptance.fixtures.SamplingStrategyMetadata;
import io.cucumber.java.Before;

/**
 * Cucumber hooks for scenario lifecycle management.
 */
public class ScenarioHooks {

    @Before
    public void resetSharedState() {
        SharedProxyHolder.reset();
        SharedDispatcherHolder.reset();
        DarkModeMetadata.reset();
        CheckoutFlowMetadata.reset();
        PaymentMethodMetadata.reset();
        SharedCompilationContext.reset();
        SharedTypedDispatchHolder.reset();
        RetryStrategyMetadata.reset();
        SamplingStrategyMetadata.reset();
        PricingTierMetadata.reset();
        RateLimiterMetadata.reset();
        FlagContext.clear();
    }
}
