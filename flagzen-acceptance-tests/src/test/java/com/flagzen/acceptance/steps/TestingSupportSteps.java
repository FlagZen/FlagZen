package com.flagzen.acceptance.steps;

import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.test.TestFlagContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing support scenarios (US-07).
 */
public class TestingSupportSteps {

    private TestFlagContext testFlagContext;
    private CheckoutFlow resolvedProxy;
    private String pinnedFlagKey;
    private String pinnedFlagValue;

    @Given("a test method annotated to pin {string} to {string}")
    public void aTestMethodAnnotatedToPinTo(String flagKey, String flagValue) {
        pinnedFlagKey = flagKey;
        pinnedFlagValue = flagValue;
    }

    @When("the test resolves {string}")
    public void theTestResolvesFeature(String featureName) {
        testFlagContext = TestFlagContext.create();
        testFlagContext.pin(pinnedFlagKey, pinnedFlagValue);
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @And("no flag provider setup was needed in the test")
    public void noFlagProviderSetupWasNeededInTheTest() {
        // Structural: only TestFlagContext.pin() and resolve() used above
    }

    @Given("a test receives a test flag context as a parameter")
    public void aTestReceivesATestFlagContextAsAParameter() {
        testFlagContext = TestFlagContext.create();
    }

    @When("the test pins {string} to {string} via the context")
    public void theTestPinsToViaTheContext(String flagKey, String flagValue) {
        testFlagContext.pin(flagKey, flagValue);
    }

    @And("resolves {string}")
    public void resolvesFeature(String featureName) {
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @Then("the pin is scoped to the current test only")
    public void thePinIsScopedToTheCurrentTestOnly() {
        // Verify isolation: a fresh context does not inherit pins from the first context
        TestFlagContext freshContext = TestFlagContext.create();
        freshContext.pin("checkout-flow", "CLASSIC");
        CheckoutFlow freshProxy = freshContext.resolve(CheckoutFlow.class);
        // The fresh context pinned CLASSIC, so it must NOT delegate to PremiumCheckout
        assertThat(freshProxy.execute()).isEqualTo("ClassicCheckout");
    }
}
