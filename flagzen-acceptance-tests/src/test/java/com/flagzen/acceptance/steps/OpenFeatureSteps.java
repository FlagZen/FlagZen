package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.openfeature.OpenFeatureFlagProvider;
import com.flagzen.spi.FlagProvider;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for OpenFeature adapter acceptance tests.
 *
 * <p>These steps invoke through the {@link FlagProvider} driving port exclusively.
 * The OpenFeature InMemoryProvider serves as the test double for the upstream
 * flag management service.
 */
public class OpenFeatureSteps {

    private final Map<String, Flag<?>> flags = new HashMap<>();
    private InMemoryProvider inMemoryProvider;
    private String namedDomain;
    private InMemoryProvider namedProvider;
    private boolean noProviderScenario;
    private final List<LogRecord> capturedLogs = new ArrayList<>();
    private Handler logHandler;

    @Before("@walking-skeleton or @US-OF-01 or @US-OF-02 or @US-OF-03")
    public void resetState() {
        SharedOpenFeatureHolder.reset();
        flags.clear();
        inMemoryProvider = null;
        namedDomain = null;
        namedProvider = null;
        noProviderScenario = false;
        capturedLogs.clear();
        removeLogHandler();
        installLogHandler();
    }

    private void installLogHandler() {
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                capturedLogs.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger.getLogger("com.flagzen.openfeature").addHandler(logHandler);
    }

    private void removeLogHandler() {
        if (logHandler != null) {
            Logger.getLogger("com.flagzen.openfeature").removeHandler(logHandler);
            logHandler = null;
        }
    }

    // --- Given: flag management service setup ---

    @Given("the flag management service has flag {string} set to {string}")
    public void flagServiceHasStringFlag(String key, String value) {
        flags.put(key, Flag.<String>builder()
                .variant("on", value)
                .defaultVariant("on")
                .build());
    }

    @Given("the flag management service has boolean flag {string} set to true")
    public void flagServiceHasBooleanFlagTrue(String key) {
        flags.put(key, Flag.<Boolean>builder()
                .variant("on", true)
                .defaultVariant("on")
                .build());
    }

    @Given("the flag management service has integer flag {string} set to {int}")
    public void flagServiceHasIntegerFlag(String key, int value) {
        flags.put(key, Flag.<Integer>builder()
                .variant("on", value)
                .defaultVariant("on")
                .build());
    }

    @Given("the flag management service has double flag {string} set to {double}")
    public void flagServiceHasDoubleFlag(String key, double value) {
        flags.put(key, Flag.<Double>builder()
                .variant("on", value)
                .defaultVariant("on")
                .build());
    }

    @Given("the flag management service has no flag named {string}")
    public void flagServiceHasNoFlag(String key) {
        // Do not add any flag -- the InMemoryProvider will return DEFAULT reason
    }

    @Given("the flag management service returns an error for flag {string}")
    public void flagServiceReturnsErrorForStringFlag(String key) {
        // Configure a flag with a context evaluator that throws, causing an error
        flags.put(key, Flag.<String>builder()
                .variant("on", "ERROR")
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    throw new RuntimeException("Simulated evaluation error");
                })
                .build());
    }

    @Given("the flag management service returns an error for boolean flag {string}")
    public void flagServiceReturnsErrorForBooleanFlag(String key) {
        flags.put(key, Flag.<Boolean>builder()
                .variant("on", true)
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    throw new RuntimeException("Simulated evaluation error");
                })
                .build());
    }

    @Given("no flag management service has been registered")
    public void noFlagServiceRegistered() {
        noProviderScenario = true;
    }

    @Given("the developer has a dedicated OpenFeature client for the {string} domain")
    public void dedicatedClientForDomain(String domain) {
        namedDomain = domain;
    }

    @Given("the flag management service has flag {string} set to {string} for that client")
    public void flagServiceHasStringFlagForNamedClient(String key, String value) {
        flags.put(key, Flag.<String>builder()
                .variant("on", value)
                .defaultVariant("on")
                .build());
    }

    // --- Given: context-aware flag setup (US-OF-03) ---

    @Given("the flag management service returns {string} for {string} when the targeting key is {string}")
    public void flagServiceReturnsForTargetingKey(String value, String key, String targetingKey) {
        flags.put(key, Flag.<String>builder()
                .variant("on", value)
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    if (targetingKey.equals(ctx.getTargetingKey())) {
                        return value;
                    }
                    return "";
                })
                .build());
    }

    @Given("the flag management service returns {string} for {string} when attribute {string} is {int}")
    public void flagServiceReturnsForIntAttribute(String value, String key, String attr, int attrValue) {
        flags.put(key, Flag.<String>builder()
                .variant("on", value)
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    var ctxValue = ctx.getValue(attr);
                    if (ctxValue != null && ctxValue.isNumber()
                            && ctxValue.asInteger() != null
                            && ctxValue.asInteger() == attrValue) {
                        return value;
                    }
                    return "";
                })
                .build());
    }

    @Given("the flag management service returns {string} for {string} when attribute {string} is {string}")
    public void flagServiceReturnsForStringAttribute(String value, String key, String attr, String attrValue) {
        flags.put(key, Flag.<String>builder()
                .variant("on", value)
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    var ctxValue = ctx.getValue(attr);
                    if (ctxValue != null && attrValue.equals(ctxValue.asString())) {
                        return value;
                    }
                    return "";
                })
                .build());
    }

    @Given("the flag management service returns true for boolean flag {string} when the targeting key is {string}")
    public void flagServiceReturnsBooleanForTargetingKey(String key, String targetingKey) {
        flags.put(key, Flag.<Boolean>builder()
                .variant("on", true)
                .defaultVariant("on")
                .contextEvaluator((flag, ctx) -> {
                    if (targetingKey.equals(ctx.getTargetingKey())) {
                        return true;
                    }
                    return false;
                })
                .build());
    }

    // --- Given: adapter construction ---

    @Given("the developer creates an OpenFeature adapter with that service")
    public void createAdapterWithService() {
        inMemoryProvider = new InMemoryProvider(flags);
        String uniqueName = "test-" + System.nanoTime();
        OpenFeatureAPI.getInstance().setProviderAndWait(uniqueName, inMemoryProvider);
        Client client = OpenFeatureAPI.getInstance().getClient(uniqueName);
        SharedOpenFeatureHolder.setProvider(OpenFeatureFlagProvider.create(client));
    }

    @When("the developer creates an OpenFeature adapter with that specific client")
    public void createAdapterWithSpecificClient() {
        namedProvider = new InMemoryProvider(flags);
        OpenFeatureAPI.getInstance().setProviderAndWait(namedDomain, namedProvider);
        Client client = OpenFeatureAPI.getInstance().getClient(namedDomain);
        SharedOpenFeatureHolder.setProvider(OpenFeatureFlagProvider.create(client));
    }

    @Given("the developer creates an OpenFeature adapter with default configuration")
    public void createAdapterWithDefaults() {
        // No provider registered -- use a unique domain to get a client with no provider
        String uniqueName = "no-provider-" + System.nanoTime();
        Client client = OpenFeatureAPI.getInstance().getClient(uniqueName);
        SharedOpenFeatureHolder.setProvider(OpenFeatureFlagProvider.create(client));
    }

    // --- Given: evaluation context building ---

    @Given("the developer builds an evaluation context with targeting key {string} and attribute {string} set to {string}")
    public void buildContextWithTargetingKeyAndStringAttribute(String targetingKey, String attrKey, String attrValue) {
        EvaluationContext ctx = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .attribute(attrKey, attrValue)
                .build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    @Given("the developer builds an evaluation context with attribute {string} set to {int} and attribute {string} set to true")
    public void buildContextWithIntAndBooleanAttributes(String attrKey1, int attrValue1, String attrKey2) {
        EvaluationContext ctx = EvaluationContext.builder()
                .attribute(attrKey1, attrValue1)
                .attribute(attrKey2, true)
                .build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    @Given("the developer builds an evaluation context with no targeting key and attribute {string} set to {string}")
    public void buildContextWithNoTargetingKeyAndStringAttribute(String attrKey, String attrValue) {
        EvaluationContext ctx = EvaluationContext.builder()
                .attribute(attrKey, attrValue)
                .build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    @Given("the developer builds an evaluation context with an unsupported attribute type")
    public void buildContextWithUnsupportedAttribute() {
        EvaluationContext ctx = EvaluationContext.builder()
                .attribute("timestamp", java.time.Instant.now())
                .build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    @Given("the developer builds an evaluation context with targeting key {string}")
    public void buildContextWithTargetingKeyOnly(String targetingKey) {
        EvaluationContext ctx = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    @Given("the developer builds an empty evaluation context")
    public void buildEmptyContext() {
        EvaluationContext ctx = EvaluationContext.builder().build();
        SharedOpenFeatureHolder.setEvaluationContext(ctx);
    }

    // --- When: flag resolution (feature file language: "resolves ... through the adapter") ---

    @When("the developer resolves string flag {string} through the adapter")
    public void resolveStringFlag(String key) {
        var result = SharedOpenFeatureHolder.getProvider().getString(key);
        SharedOpenFeatureHolder.setLastStringResult(result);
    }

    @When("the developer resolves boolean flag {string} through the adapter")
    public void resolveBooleanFlag(String key) {
        var result = SharedOpenFeatureHolder.getProvider().getBoolean(key);
        SharedOpenFeatureHolder.setLastBooleanResult(result);
    }

    @When("the developer resolves integer flag {string} through the adapter")
    public void resolveIntegerFlag(String key) {
        var result = SharedOpenFeatureHolder.getProvider().getInt(key);
        SharedOpenFeatureHolder.setLastIntResult(result);
    }

    @When("the developer resolves long flag {string} through the adapter")
    public void resolveLongFlag(String key) {
        var result = SharedOpenFeatureHolder.getProvider().getLong(key);
        SharedOpenFeatureHolder.setLastLongResult(result);
    }

    @When("the developer resolves double flag {string} through the adapter")
    public void resolveDoubleFlag(String key) {
        var result = SharedOpenFeatureHolder.getProvider().getDouble(key);
        SharedOpenFeatureHolder.setLastDoubleResult(result);
    }

    @When("the developer resolves string flag {string} with that evaluation context")
    public void resolveStringFlagWithContext(String key) {
        var ctx = SharedOpenFeatureHolder.getEvaluationContext();
        var result = SharedOpenFeatureHolder.getProvider().getString(key, ctx);
        SharedOpenFeatureHolder.setLastStringResult(result);
    }

    @When("the developer resolves boolean flag {string} with that evaluation context")
    public void resolveBooleanFlagWithContext(String key) {
        var ctx = SharedOpenFeatureHolder.getEvaluationContext();
        var result = SharedOpenFeatureHolder.getProvider().getBoolean(key, ctx);
        SharedOpenFeatureHolder.setLastBooleanResult(result);
    }

    // --- Then: assertions (adapter language) ---

    @Then("the adapter returns {string}")
    public void adapterReturnsString(String expected) {
        assertThat(SharedOpenFeatureHolder.getLastStringResult())
                .isPresent()
                .hasValue(expected);
    }

    @Then("the adapter returns no string value")
    public void adapterReturnsNoStringValue() {
        assertThat(SharedOpenFeatureHolder.getLastStringResult()).isEmpty();
    }

    @Then("the adapter returns boolean true")
    public void adapterReturnsBooleanTrue() {
        assertThat(SharedOpenFeatureHolder.getLastBooleanResult())
                .isPresent()
                .hasValue(true);
    }

    @Then("the adapter returns no boolean value")
    public void adapterReturnsNoBooleanValue() {
        assertThat(SharedOpenFeatureHolder.getLastBooleanResult()).isEmpty();
    }

    @Then("the adapter returns integer {int}")
    public void adapterReturnsInteger(int expected) {
        assertThat(SharedOpenFeatureHolder.getLastIntResult()).isPresent();
        assertThat(SharedOpenFeatureHolder.getLastIntResult().getAsInt()).isEqualTo(expected);
    }

    @Then("the adapter returns no integer value")
    public void adapterReturnsNoIntegerValue() {
        assertThat(SharedOpenFeatureHolder.getLastIntResult()).isEmpty();
    }

    @Then("the adapter returns double {double}")
    public void adapterReturnsDouble(double expected) {
        assertThat(SharedOpenFeatureHolder.getLastDoubleResult()).isPresent();
        assertThat(SharedOpenFeatureHolder.getLastDoubleResult().getAsDouble()).isEqualTo(expected);
    }

    @Then("the adapter returns long {long}")
    public void adapterReturnsLong(long expected) {
        assertThat(SharedOpenFeatureHolder.getLastLongResult()).isPresent();
        assertThat(SharedOpenFeatureHolder.getLastLongResult().getAsLong()).isEqualTo(expected);
    }

    @And("a warning is logged about the unsupported attribute type")
    public void warningLoggedAboutUnsupportedType() {
        assertThat(capturedLogs)
                .anyMatch(record -> record.getLevel() == Level.WARNING
                        && record.getMessage().contains("Unsupported attribute type"));
    }

}
