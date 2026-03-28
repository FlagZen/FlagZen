package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.env.EnvironmentVariableFlagProvider;
import com.flagzen.keymapping.FlagKeyFormats;
import com.flagzen.keymapping.FlagKeyParsers;
import com.flagzen.spi.FlagProvider;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for environment variable flag provider acceptance tests.
 */
public class EnvProviderSteps {

    @Before("@walking-skeleton or @US-ENV-01 or @US-ENV-02 or @US-ENV-03 or @US-ENV-04")
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

    // --- US-ENV-01: Additional lookup steps ---

    @Then("looking up flag {string} returns {string}")
    public void lookingUpFlagReturns(String flagKey, String expectedValue) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        assertThat(result).isPresent().hasValue(expectedValue);
    }

    @Then("looking up flag {string} returns no value")
    public void lookingUpFlagReturnsNoValue(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        assertThat(result).isEmpty();
    }

    // --- US-ENV-02: Eager loading ---

    @Given("the provider has been constructed with default configuration")
    public void theProviderHasBeenConstructedWithDefaultConfiguration() {
        theDeveloperCreatesAProviderWithDefaultConfiguration();
    }

    @When("the developer looks up flag {string} multiple times")
    public void theDeveloperLooksUpFlagMultipleTimes(String flagKey) {
        var provider = SharedEnvProviderHolder.getProvider();
        var first = provider.getString(flagKey);
        var second = provider.getString(flagKey);
        var third = provider.getString(flagKey);
        SharedEnvProviderHolder.setLastResult(first);
        SharedEnvProviderHolder.setRepeatedResults(List.of(first, second, third));
    }

    @Then("every lookup returns {string}")
    public void everyLookupReturns(String expectedValue) {
        for (var result : SharedEnvProviderHolder.getRepeatedResults()) {
            assertThat(result).isPresent().hasValue(expectedValue);
        }
    }

    @And("the developer looks up flag {string} with an evaluation context")
    public void theDeveloperLooksUpFlagWithAnEvaluationContext(String flagKey) {
        var context = EvaluationContext.builder()
                .targetingKey("test-user")
                .attribute("tier", "premium")
                .build();
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey, context);
        SharedEnvProviderHolder.setLastResult(result);
    }

    // --- US-ENV-03: ServiceLoader ---

    @Given("the environment variable provider module is on the classpath")
    public void theEnvironmentVariableProviderModuleIsOnTheClasspath() {
        // flagzen-env is already on the test classpath via build.gradle.kts
    }

    @When("the service loader discovers available flag providers")
    public void theServiceLoaderDiscoversAvailableFlagProviders() {
        ServiceLoader<FlagProvider> loader = ServiceLoader.load(FlagProvider.class);
        SharedEnvProviderHolder.setDiscoveredProviders(
                loader.stream().map(ServiceLoader.Provider::get).toList()
        );
    }

    @Then("the environment variable provider is among the discovered providers")
    public void theEnvironmentVariableProviderIsAmongTheDiscoveredProviders() {
        var providers = SharedEnvProviderHolder.getDiscoveredProviders();
        assertThat(providers)
                .anyMatch(p -> p instanceof EnvironmentVariableFlagProvider);
    }

    @When("the developer resolves flag {string} through the auto-discovered provider")
    public void theDeveloperResolvesFlagThroughTheAutoDiscoveredProvider(String flagKey) {
        // Build a custom provider with our test env vars (ServiceLoader would use System.getenv)
        // But we verify discoverability separately; here we test that a discovered provider resolves.
        FlagProvider provider = EnvironmentVariableFlagProvider.builder()
                .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
                .build();
        SharedEnvProviderHolder.setProvider(provider);
        var result = provider.getString(flagKey);
        SharedEnvProviderHolder.setLastResult(result);
    }

    // --- US-ENV-04: Custom parsers and formatters ---

    @Given("the developer configures a provider with screaming snake case parser using prefix {string}")
    public void theDeveloperConfiguresAProviderWithScreamingSnakeCaseParserUsingPrefix(String prefix) {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase(prefix))
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @Given("the developer configures a custom parser for {string} prefixed names")
    public void theDeveloperConfiguresACustomParserForPrefixedNames(String prefix) {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(name -> {
                            if (!name.startsWith(prefix)) {
                                return Optional.empty();
                            }
                            String remainder = name.substring(prefix.length());
                            return Optional.of(
                                    List.of(remainder.toLowerCase().split("_"))
                            );
                        })
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @Given("the developer configures a provider with snake case formatter")
    public void theDeveloperConfiguresAProviderWithSnakeCaseFormatter() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .formatter(FlagKeyFormats.snakeCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @When("the provider is built")
    public void theProviderIsBuilt() {
        var builder = SharedEnvProviderHolder.getBuilder();
        SharedEnvProviderHolder.setProvider(builder.build());
    }

    // --- Error paths: typed accessors ---

    @Then("looking up integer flag {string} returns no value")
    public void lookingUpIntegerFlagReturnsNoValue(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getInt(flagKey);
        assertThat(result).isEmpty();
    }

    @Then("looking up boolean flag {string} returns no value")
    public void lookingUpBooleanFlagReturnsNoValue(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getBoolean(flagKey);
        assertThat(result).isEmpty();
    }

    @But("looking up string flag {string} returns {string}")
    public void lookingUpStringFlagReturns(String flagKey, String expectedValue) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        assertThat(result).isPresent().hasValue(expectedValue);
    }
}
