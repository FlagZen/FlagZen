package com.flagzen.acceptance.steps;

import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for walking skeleton scenario 2:
 * "Developer resolves a feature to the active variant at runtime"
 */
public class RuntimeDispatchSteps {

    private InMemoryFlagProvider flagProvider;
    private FeatureDispatcher dispatcher;
    private CheckoutFlow resolvedProxy;
    private String callResult;

    @Given("a compiled feature {string} with variants {string} and {string}")
    public void aCompiledFeatureWithVariants(String featureName, String variant1, String variant2) {
        // Fixtures are pre-compiled and registered via ServiceLoader
        // (CheckoutFlowMetadata in META-INF/services)
    }

    @And("an in-memory flag provider with {string} set to {string}")
    public void anInMemoryFlagProviderWithSetTo(String flagKey, String flagValue) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(flagKey, flagValue);
    }

    @And("the dispatcher is configured with this provider")
    public void theDispatcherIsConfiguredWithThisProvider() {
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @When("the developer resolves {string} through the dispatcher")
    public void theDeveloperResolvesThroughTheDispatcher(String featureName) {
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
    }

    @And("calls {string} on the resolved proxy")
    public void callsOnTheResolvedProxy(String methodName) {
        callResult = resolvedProxy.execute();
    }

    @Then("the call is handled by the {string} variant")
    public void theCallIsHandledByTheVariant(String variantClass) {
        assertThat(callResult).isEqualTo(variantClass);
    }
}
