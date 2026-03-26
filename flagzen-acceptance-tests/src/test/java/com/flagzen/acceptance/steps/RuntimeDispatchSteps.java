package com.flagzen.acceptance.steps;

import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagZen;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.DarkMode;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.FlagProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for runtime dispatch scenarios.
 */
public class RuntimeDispatchSteps {

    private InMemoryFlagProvider flagProvider;
    private FlagProvider customFlagProvider;
    private FeatureDispatcher dispatcher;
    private CheckoutFlow resolvedProxy;
    private CheckoutFlow secondResolvedProxy;
    private String callResult;
    private Exception caughtException;
    private boolean noProviderConfigured;

    @Given("no flag provider is configured")
    public void noFlagProviderIsConfigured() {
        noProviderConfigured = true;
    }

    @Given("a compiled feature {string} with variants {string} and {string}")
    public void aCompiledFeatureWithVariants(String featureName, String variant1, String variant2) {
        // Fixtures are pre-compiled and registered via ServiceLoader
        // (CheckoutFlowMetadata in META-INF/services)
    }

    @And("the flag provider returns {string} for {string}")
    public void theFlagProviderReturnsForKey(String flagValue, String flagKey) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(flagKey, flagValue);
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @And("the developer has resolved {string} through the dispatcher")
    public void theDeveloperHasResolvedThroughTheDispatcher(String featureName) {
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
    }

    @When("the flag provider value changes to {string}")
    public void theFlagProviderValueChangesTo(String newValue) {
        flagProvider.set("checkout-flow", newValue);
    }

    @And("the developer calls {string} on the same proxy")
    public void theDeveloperCallsOnTheSameProxy(String methodName) {
        callResult = resolvedProxy.execute();
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
        try {
            if (noProviderConfigured) {
                dispatcher = FlagZen.dispatcher(config -> { /* no provider */ });
            }
            resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
            SharedProxyHolder.set(resolvedProxy);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @And("calls {string} on the resolved proxy")
    public void callsOnTheResolvedProxy(String methodName) {
        callResult = resolvedProxy.execute();
    }

    @When("the developer resolves {string} through the dispatcher twice")
    public void theDeveloperResolvesThroughTheDispatcherTwice(String featureName) {
        if (flagProvider == null) {
            flagProvider = new InMemoryFlagProvider();
            flagProvider.set("checkout-flow", "CLASSIC");
        }
        if (dispatcher == null) {
            dispatcher = new DefaultFeatureDispatcher(flagProvider);
        }
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
        secondResolvedProxy = dispatcher.resolve(CheckoutFlow.class);
    }

    @Then("both resolutions return the same proxy instance")
    public void bothResolutionsReturnTheSameProxyInstance() {
        assertThat(resolvedProxy).isSameAs(secondResolvedProxy);
    }

    @Then("the call is handled by the {string} variant")
    public void theCallIsHandledByTheVariant(String variantClass) {
        assertThat(callResult).isEqualTo(variantClass);
    }

    @Then("a configuration error is raised")
    public void aConfigurationErrorIsRaised() {
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(com.flagzen.FlagZenException.class);
    }

    @Then("the message states no flag provider is configured")
    public void theMessageStatesNoFlagProviderIsConfigured() {
        assertThat(caughtException.getMessage())
                .containsIgnoringCase("no flagprovider configured");
    }

    @Then("the message suggests how to add one")
    public void theMessageSuggestsHowToAddOne() {
        assertThat(caughtException.getMessage())
                .containsIgnoringCase("provider");
    }

    @Given("a custom flag provider that returns {string} for {string}")
    public void aCustomFlagProviderThatReturnsFor(String flagValue, String flagKey) {
        Map<String, String> flags = Map.of(flagKey, flagValue);
        customFlagProvider = key -> Optional.ofNullable(flags.get(key));
    }

    @When("the developer configures the dispatcher with this provider")
    public void theDeveloperConfiguresTheDispatcherWithThisProvider() {
        dispatcher = FlagZen.dispatcher(config -> config.provider(customFlagProvider));
    }

    @When("resolves {string} through the dispatcher")
    public void resolvesFeatureThroughTheDispatcher(String featureName) {
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @Given("an in-memory flag provider with no flags configured")
    public void anInMemoryFlagProviderWithNoFlagsConfigured() {
        flagProvider = new InMemoryFlagProvider();
    }

    @And("the feature {string} uses fallback strategy EXCEPTION")
    public void theFeatureUsesFallbackStrategyException(String featureName) {
        // DarkModeMetadata already has EXCEPTION as its fallback strategy.
        // Dispatcher created here with the empty provider.
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @When("the developer resolves {string} and calls a method")
    public void theDeveloperResolvesAndCallsAMethod(String featureName) {
        try {
            DarkMode proxy = dispatcher.resolve(DarkMode.class);
            proxy.apply();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("an unmatched variant error is raised")
    public void anUnmatchedVariantErrorIsRaised() {
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(UnmatchedVariantException.class);
    }

    @And("the message indicates no flag value was found for {string}")
    public void theMessageIndicatesNoFlagValueWasFoundFor(String flagKey) {
        assertThat(caughtException.getMessage())
                .containsIgnoringCase("no flag value")
                .containsIgnoringCase(flagKey);
    }

    @Given("a compiled feature {string} with variants {string}, {string}, {string}")
    public void aCompiledFeatureWithThreeVariants(String featureName, String v1, String v2, String v3) {
        // Fixtures are pre-compiled: CheckoutFlowMetadata registers CLASSIC, STREAMLINED, PREMIUM
    }

    @And("{string} uses fallback strategy EXCEPTION")
    public void featureUsesFallbackStrategyException(String featureName) {
        // CheckoutFlowMetadata already declares EXCEPTION as fallback strategy.
        // Dispatcher will be created when flag provider step runs.
    }

    @When("the developer calls {string} on the resolved proxy")
    public void theDeveloperCallsOnTheResolvedProxy(String methodName) {
        try {
            resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
            callResult = resolvedProxy.execute();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @And("the error message lists known variants: {string}, {string}, {string}")
    public void theErrorMessageListsKnownVariants(String v1, String v2, String v3) {
        assertThat(caughtException.getMessage())
                .contains(v1)
                .contains(v2)
                .contains(v3);
    }
}
