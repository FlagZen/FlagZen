package com.flagzen.acceptance.steps;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared assertion steps used across multiple step definition classes.
 * Reads resolved proxy from {@link SharedProxyHolder}.
 */
public class SharedAssertionSteps {

    @Then("the resolved proxy delegates to {string}")
    public void theResolvedProxyDelegatesTo(String variantClass) {
        var proxy = SharedProxyHolder.get();
        assertThat(proxy).as("resolvedProxy must be set before assertion").isNotNull();
        String result = proxy.execute();
        assertThat(result).isEqualTo(variantClass);
    }
}
