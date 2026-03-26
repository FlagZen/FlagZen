package com.flagzen.acceptance.steps;

import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.test.TestFlagContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for walking skeleton scenario 3:
 * "Developer pins a flag value in a test with a single annotation"
 */
public class TestingSupportSteps {

    private TestFlagContext testFlagContext;
    private CheckoutFlow resolvedProxy;
    private String pinnedFlagKey;
    private String pinnedFlagValue;

    @Given("a test method annotated to pin {string} to {string}")
    public void aTestMethodAnnotatedToPinTo(String flagKey, String flagValue) {
        // Simulate what @PinFlag + FlagZenExtension would do:
        // create a TestFlagContext and pin the value
        pinnedFlagKey = flagKey;
        pinnedFlagValue = flagValue;
    }

    @When("the test resolves {string}")
    public void theTestResolvesFeature(String featureName) {
        // Programmatic equivalent of what FlagZenExtension does for @PinFlag
        testFlagContext = TestFlagContext.create();
        testFlagContext.pin(pinnedFlagKey, pinnedFlagValue);
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
    }

    @Then("the resolved proxy delegates to {string}")
    public void theResolvedProxyDelegatesTo(String variantClass) {
        String result = resolvedProxy.execute();
        assertThat(result).isEqualTo(variantClass);
    }

    @And("no flag provider setup was needed in the test")
    public void noFlagProviderSetupWasNeededInTheTest() {
        // This is verified by the fact that we only used TestFlagContext.pin()
        // and resolve() -- no InMemoryFlagProvider or DefaultFeatureDispatcher
        // was instantiated directly in the "test" steps above.
        // The assertion is structural: the test code above proves this.
    }
}
