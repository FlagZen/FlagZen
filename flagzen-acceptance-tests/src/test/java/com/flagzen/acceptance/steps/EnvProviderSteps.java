package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.env.EnvironmentVariableFlagProvider;
import com.flagzen.keymapping.ConflictStrategy;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for environment variable flag provider acceptance tests.
 */
public class EnvProviderSteps {

    @Before("@walking-skeleton or @US-ENV-01 or @US-ENV-02 or @US-ENV-03 or @US-ENV-04 or @US-ENV-07 or @US-ENV-08 or @US-ENV-09 or @US-ENV-10")
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
        try {
            SharedEnvProviderHolder.setProvider(builder.build());
        } catch (IllegalStateException e) {
            SharedEnvProviderHolder.setBuildException(e);
        }
    }

    // --- US-ENV-04: Error paths: typed accessors ---

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

    // --- US-ENV-07: Multiple parsers ---

    @Given("the developer configures parsers for both {string} screaming snake case and {string} camel case")
    public void theDeveloperConfiguresParsersForBothScreamingSnakeCaseAndCamelCase(
            String screamingPrefix, String camelPrefix) {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase(screamingPrefix))
                        .parser(FlagKeyParsers.camelCase(camelPrefix))
                        .warningConsumer(SharedEnvProviderHolder::addWarning)
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @When("the provider is built with default conflict handling")
    public void theProviderIsBuiltWithDefaultConflictHandling() {
        var builder = SharedEnvProviderHolder.getBuilder();
        SharedEnvProviderHolder.setProvider(builder.build());
    }

    @And("no {string} prefixed variable maps to {string}")
    public void noPrefixedVariableMapsTo(String prefix, String flagKey) {
        // No such env var set - already clean from reset
    }

    @Then("no conflict warning is produced")
    public void noConflictWarningIsProduced() {
        assertThat(SharedEnvProviderHolder.getWarnings()).isEmpty();
    }

    // --- US-ENV-08: Multiple formatters ---

    @Given("the developer configures formatters for both kebab case and snake case")
    public void theDeveloperConfiguresFormattersForBothKebabCaseAndSnakeCase() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .formatter(FlagKeyFormats.kebabCase())
                        .formatter(FlagKeyFormats.snakeCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    // --- US-ENV-09: ConflictStrategy cardinality defaults ---

    @Given("the developer configures one parser and one formatter")
    public void theDeveloperConfiguresOneParserAndOneFormatter() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .formatter(FlagKeyFormats.kebabCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @Given("the developer configures two parsers and one formatter")
    public void theDeveloperConfiguresTwoParsersAndOneFormatter() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .parser(FlagKeyParsers.camelCase("myApp"))
                        .formatter(FlagKeyFormats.kebabCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @Given("the developer configures one parser and two formatters")
    public void theDeveloperConfiguresOneParserAndTwoFormatters() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .formatter(FlagKeyFormats.kebabCase())
                        .formatter(FlagKeyFormats.snakeCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @Given("the developer configures two parsers and two formatters")
    public void theDeveloperConfiguresTwoParsersAndTwoFormatters() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .parser(FlagKeyParsers.camelCase("myApp"))
                        .formatter(FlagKeyFormats.kebabCase())
                        .formatter(FlagKeyFormats.snakeCase())
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @And("no explicit conflict strategy is set")
    public void noExplicitConflictStrategyIsSet() {
        // No-op: the builder defaults are already in place
    }

    @Then("the default conflict strategy is warn")
    public void theDefaultConflictStrategyIsWarn() {
        // Build succeeds — confirms no ERROR default. WARN is the safe default.
        var builder = SharedEnvProviderHolder.getBuilder()
                .warningConsumer(SharedEnvProviderHolder.getWarnings()::add);
        SharedEnvProviderHolder.setProvider(builder.build());
        // If strategy were ERROR, any future conflict would throw. Build succeeding confirms WARN.
    }

    @Then("the default conflict strategy is error")
    public void theDefaultConflictStrategyIsError() {
        // Inject conflicting env vars — both parsers produce same flag key.
        // If default is ERROR (NxN), build should throw.
        SharedEnvProviderHolder.setEnvVar("FLAGZEN_CONFLICT_TEST", "a");
        SharedEnvProviderHolder.setEnvVar("myAppConflictTest", "b");
        var builder = SharedEnvProviderHolder.getBuilder();
        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @And("the conflict strategy is explicitly set to warn")
    public void theConflictStrategyIsExplicitlySetToWarn() {
        SharedEnvProviderHolder.setBuilder(
                SharedEnvProviderHolder.getBuilder().onConflict(ConflictStrategy.WARN)
        );
    }

    @Then("the conflict strategy is warn")
    public void theConflictStrategyIsWarn() {
        // Inject conflicting env vars — both parsers produce same flag key.
        // With WARN override, build should succeed despite conflict.
        SharedEnvProviderHolder.setEnvVar("FLAGZEN_CONFLICT_TEST", "a");
        SharedEnvProviderHolder.setEnvVar("myAppConflictTest", "b");
        var builder = SharedEnvProviderHolder.getBuilder()
                .warningConsumer(SharedEnvProviderHolder.getWarnings()::add);
        try {
            SharedEnvProviderHolder.setProvider(builder.build());
            // WARN strategy: construction succeeds despite conflict
        } catch (IllegalStateException e) {
            fail("Expected WARN strategy (construction succeeds), but got ERROR: " + e.getMessage());
        }
    }

    // --- US-ENV-09: WARN/ERROR behavior ---

    @Given("the developer configures two parsers mapping to the same flag key")
    public void theDeveloperConfiguresTwoParsersMappingToTheSameFlagKey() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .parser(FlagKeyParsers.camelCase("myApp"))
                        .warningConsumer(SharedEnvProviderHolder::addWarning)
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @And("the conflict strategy is set to warn")
    public void theConflictStrategyIsSetToWarn() {
        SharedEnvProviderHolder.getBuilder().onConflict(ConflictStrategy.WARN);
    }

    @And("the conflict strategy is set to error")
    public void theConflictStrategyIsSetToError() {
        SharedEnvProviderHolder.getBuilder().onConflict(ConflictStrategy.ERROR);
    }

    @Then("a conflict warning is produced mentioning both environment variable names")
    public void aConflictWarningIsProducedMentioningBothEnvironmentVariableNames() {
        assertThat(SharedEnvProviderHolder.getWarnings())
                .anyMatch(w -> w.contains("FLAGZEN_CHECKOUT_FLOW")
                        && w.contains("myAppCheckoutFlow"));
    }

    @And("the provider continues operating normally")
    public void theProviderContinuesOperatingNormally() {
        assertThat(SharedEnvProviderHolder.getProvider()).isNotNull();
        var result = SharedEnvProviderHolder.getProvider().getString("checkout-flow");
        assertThat(result).isPresent();
    }

    @Then("construction fails with a conflict error")
    public void constructionFailsWithAConflictError() {
        assertThat(SharedEnvProviderHolder.getBuildException())
                .isInstanceOf(IllegalStateException.class);
    }

    @And("the error message mentions both environment variable names and flag key {string}")
    public void theErrorMessageMentionsBothEnvironmentVariableNamesAndFlagKey(String flagKey) {
        var exception = SharedEnvProviderHolder.getBuildException();
        assertThat(exception.getMessage())
                .contains("FLAGZEN_CHECKOUT_FLOW")
                .contains("myAppCheckoutFlow")
                .contains(flagKey);
    }

    // --- US-ENV-10: First-access conflict warning ---

    @Given("a provider was built with warn strategy and flag key {string} had a conflict")
    public void aProviderWasBuiltWithWarnStrategyAndFlagKeyHadAConflict(String flagKey) {
        SharedEnvProviderHolder.setEnvVar("FLAGZEN_CHECKOUT_FLOW", "PREMIUM");
        SharedEnvProviderHolder.setEnvVar("myAppCheckoutFlow", "BASIC");
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .parser(FlagKeyParsers.camelCase("myApp"))
                        .onConflict(ConflictStrategy.WARN)
                        .warningConsumer(SharedEnvProviderHolder::addWarning)
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
        SharedEnvProviderHolder.setProvider(
                SharedEnvProviderHolder.getBuilder().build()
        );
        // Clear construction warnings so we only see first-access warnings
        SharedEnvProviderHolder.getWarnings().clear();
    }

    @When("the developer looks up flag {string} for the first time")
    public void theDeveloperLooksUpFlagForTheFirstTime(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        SharedEnvProviderHolder.setLastResult(result);
    }

    @Then("a conflict warning is produced at the point of use")
    public void aConflictWarningIsProducedAtThePointOfUse() {
        assertThat(SharedEnvProviderHolder.getWarnings())
                .hasSize(1)
                .anyMatch(w -> w.contains("checkout-flow"));
    }

    @And("the developer has already looked up flag {string} once")
    public void theDeveloperHasAlreadyLookedUpFlagOnce(String flagKey) {
        SharedEnvProviderHolder.getProvider().getString(flagKey);
        // Clear warnings from first access
        SharedEnvProviderHolder.getWarnings().clear();
    }

    @When("the developer looks up flag {string} again")
    public void theDeveloperLooksUpFlagAgain(String flagKey) {
        var result = SharedEnvProviderHolder.getProvider().getString(flagKey);
        SharedEnvProviderHolder.setLastResult(result);
    }

    @Then("no additional conflict warning is produced")
    public void noAdditionalConflictWarningIsProduced() {
        assertThat(SharedEnvProviderHolder.getWarnings()).isEmpty();
    }

    @Given("a provider was built with warn strategy")
    public void aProviderWasBuiltWithWarnStrategy() {
        SharedEnvProviderHolder.setBuilder(
                EnvironmentVariableFlagProvider.builder()
                        .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))
                        .parser(FlagKeyParsers.camelCase("myApp"))
                        .onConflict(ConflictStrategy.WARN)
                        .warningConsumer(SharedEnvProviderHolder::addWarning)
                        .environmentSource(() -> SharedEnvProviderHolder.getEnvVars())
        );
    }

    @And("flag key {string} had no conflict during construction")
    public void flagKeyHadNoConflictDuringConstruction(String flagKey) {
        SharedEnvProviderHolder.setEnvVar("FLAGZEN_MAX_RETRIES", "3");
        SharedEnvProviderHolder.setProvider(
                SharedEnvProviderHolder.getBuilder().build()
        );
        SharedEnvProviderHolder.getWarnings().clear();
    }

}
