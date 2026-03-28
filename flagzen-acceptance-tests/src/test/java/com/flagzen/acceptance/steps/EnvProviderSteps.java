package com.flagzen.acceptance.steps;

import com.flagzen.env.EnvironmentVariableFlagProvider;
import com.flagzen.spi.FlagProvider;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for environment variable flag provider acceptance tests.
 */
public class EnvProviderSteps {

    @Before("@walking-skeleton")
    public void resetState() {
        SharedEnvProviderHolder.reset();
    }

    @Given("environment variable {string} is set to {string}")
    public void environmentVariableIsSetTo(String name, String value) {
        SharedEnvProviderHolder.setEnvVar(name, value);
    }

    @Given("no environment variable maps to flag {string}")
    public void noEnvironmentVariableMapsToFlag(String flagKey) {
        // No env vars set - holder is already clean after reset
    }

    @When("the developer creates a provider with default configuration")
    public void theDeveloperCreatesAProviderWithDefaultConfiguration() {
        FlagProvider provider = EnvironmentVariableFlagProvider.builder()
                .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
                .build();
        SharedEnvProviderHolder.setProvider(provider);
    }

    @And("the developer looks up flag {string}")
    public void theDeveloperLooksUpFlag(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        SharedEnvProviderHolder.setLastResult(result);
    }

    @Then("the flag value is {string}")
    public void theFlagValueIs(String expectedValue) {
        assertThat(SharedEnvProviderHolder.getLastResult())
                .isPresent()
                .hasValue(expectedValue);
    }

    @Then("no flag value is returned")
    public void noFlagValueIsReturned() {
        assertThat(SharedEnvProviderHolder.getLastResult())
                .isEmpty();
    }
}
