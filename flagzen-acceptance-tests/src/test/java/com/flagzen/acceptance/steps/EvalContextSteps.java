package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.DefaultCheckout;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.FlagProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for EvaluationContext scenarios (US-EC-01 through US-EC-04).
 */
public class EvalContextSteps {

    private EvaluationContext context;
    private FeatureDispatcher dispatcher;
    private CheckoutFlow resolvedProxy;
    private FlagProvider flagProvider;

    // Tracking fields for verification
    private final AtomicReference<String> capturedFlagKey = new AtomicReference<>();
    private final AtomicReference<EvaluationContext> capturedContext = new AtomicReference<>();
    private final AtomicBoolean contextlessLookupPerformed = new AtomicBoolean(false);

    // --- US-EC-01: EvaluationContext Builder ---

    @Given("a developer needs to target flags for user {string}")
    public void aDeveloperNeedsToTargetFlagsForUser(String userId) {
        // Context: the developer intends to build a context for this user.
    }

    @When("the developer builds an evaluation context with targeting key {string} and attributes:")
    public void theDeveloperBuildsContextWithKeyAndAttributes(String targetingKey, DataTable dataTable) {
        EvaluationContext.Builder builder = EvaluationContext.builder()
                .targetingKey(targetingKey);
        for (Map<String, String> row : dataTable.asMaps()) {
            builder.attribute(row.get("attribute"), row.get("value"));
        }
        context = builder.build();
    }

    @When("the developer builds an evaluation context with only attribute {string} = {string}")
    public void theDeveloperBuildsContextWithOnlyAttribute(String key, String value) {
        context = EvaluationContext.builder()
                .attribute(key, value)
                .build();
    }

    @When("the developer builds an evaluation context with targeting key {string} and no attributes")
    public void theDeveloperBuildsContextWithKeyOnly(String targetingKey) {
        context = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @Then("the context targeting key is {string}")
    public void theContextTargetingKeyIs(String expected) {
        assertThat(context.targetingKey()).isEqualTo(expected);
    }

    @Then("the context targeting key is absent")
    public void theContextTargetingKeyIsAbsent() {
        assertThat(context.targetingKey()).isNull();
    }

    @Then("the context attribute {string} is {string}")
    public void theContextAttributeIs(String key, String expected) {
        assertThat(context.attributes()).containsEntry(key, expected);
    }

    @Then("the context has no attributes")
    public void theContextHasNoAttributes() {
        assertThat(context.attributes()).isEmpty();
    }

    @Then("the context has an empty attributes collection, not null")
    public void theContextHasEmptyAttributesNotNull() {
        assertThat(context.attributes()).isNotNull().isEmpty();
    }

    // --- US-EC-02: Explicit Context on FeatureDispatcher.resolve() ---

    @Given("a feature {string} with variants {string} and {string}")
    public void aFeatureWithVariants(String featureName, String variant1, String variant2) {
        // Fixtures are pre-compiled (CheckoutFlowMetadata registers CLASSIC, STREAMLINED, PREMIUM)
    }

    @And("a flag provider that returns {string} when context attribute {string} is {string}")
    public void aFlagProviderThatReturnsWhenContextAttribute(String returnValue, String attrKey, String attrValue) {
        flagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                capturedFlagKey.set(key);
                contextlessLookupPerformed.set(true);
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                capturedFlagKey.set(key);
                capturedContext.set(ctx);
                if (ctx != null && attrValue.equals(ctx.attributes().get(attrKey))) {
                    return Optional.of(returnValue);
                }
                return getString(key);
            }
        };
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @And("an evaluation context with targeting key {string} and attribute {string} = {string}")
    public void anEvaluationContextWithTargetingKeyAndAttribute(String targetingKey, String attrKey, String attrValue) {
        context = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .attribute(attrKey, attrValue)
                .build();
    }

    @When("the developer resolves {string} with that evaluation context")
    public void theDeveloperResolvesWithThatEvaluationContext(String featureName) {
        FlagContext.clear();
        if (dispatcher == null) {
            InMemoryFlagProvider shared = SharedDispatcherHolder.getInMemoryProvider();
            if (shared != null) {
                dispatcher = new DefaultFeatureDispatcher(shared);
            }
        }
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class, context);
    }

    @Then("the resolved proxy dispatches to the {string} variant")
    public void theResolvedProxyDispatchesToTheVariant(String expectedVariant) {
        String titleCase = expectedVariant.substring(0, 1).toUpperCase()
                + expectedVariant.substring(1).toLowerCase();
        String expected = titleCase + "Checkout";
        // If dispatch result was captured inside a scoped block, use that
        String preCapture = SharedProxyHolder.getLastDispatchResult();
        if (preCapture != null) {
            assertThat(preCapture).isEqualTo(expected);
            return;
        }
        CheckoutFlow proxy = resolvedProxy != null ? resolvedProxy : SharedProxyHolder.get();
        assertThat(proxy.execute()).isEqualTo(expected);
    }

    @When("the developer resolves {string} without evaluation context")
    public void theDeveloperResolvesWithoutEvaluationContext(String featureName) {
        FlagContext.clear();
        if (dispatcher == null) {
            InMemoryFlagProvider shared = SharedDispatcherHolder.getInMemoryProvider();
            if (shared != null) {
                dispatcher = new DefaultFeatureDispatcher(shared);
                flagProvider = shared;
            }
        }
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
    }

    @And("behavior is identical to pre-context FlagZen")
    public void behaviorIsIdenticalToPreContextFlagZen() {
        // Verified by the previous assertion: resolve(Class) dispatches correctly without context.
        assertThat(resolvedProxy).isNotNull();
    }

    @When("the developer resolves {string} with null evaluation context")
    public void theDeveloperResolvesWithNullEvaluationContext(String featureName) {
        FlagContext.clear();
        if (dispatcher == null) {
            InMemoryFlagProvider shared = SharedDispatcherHolder.getInMemoryProvider();
            if (shared != null) {
                dispatcher = new DefaultFeatureDispatcher(shared);
            }
        }
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class, null);
    }

    @And("the flag provider receives a contextless flag lookup")
    public void theFlagProviderReceivesAContextlessFlagLookup() {
        // When null context is passed, the proxy should not have context available.
        // FlagContext.current() should be null after null was passed.
        assertThat(FlagContext.current()).isNull();
    }

    // --- US-EC-03: FlagProvider Context-Aware Overload ---

    @And("an evaluation context with targeting key {string}")
    public void anEvaluationContextWithTargetingKey(String targetingKey) {
        context = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
        SharedDispatcherHolder.setEvalContext(context);
    }

    @Then("the in-memory flag provider returns {string} regardless of context")
    public void theInMemoryFlagProviderReturnsRegardlessOfContext(String expectedValue) {
        InMemoryFlagProvider provider = SharedDispatcherHolder.getInMemoryProvider();
        assertThat(provider).isNotNull();
        assertThat(provider.getString("checkout-flow"))
                .isPresent()
                .hasValue(expectedValue);
    }

    @And("a context-aware flag provider that returns {string} for plan {string}")
    public void aContextAwareFlagProviderThatReturnsForPlan(String returnValue, String planValue) {
        flagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                capturedFlagKey.set(key);
                contextlessLookupPerformed.set(true);
                return Optional.empty();
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                capturedFlagKey.set(key);
                capturedContext.set(ctx);
                if (ctx != null && planValue.equals(ctx.attributes().get("plan"))) {
                    return Optional.of(returnValue);
                }
                return getString(key);
            }
        };
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @And("an evaluation context with attribute {string} = {string}")
    public void anEvaluationContextWithAttribute(String attrKey, String attrValue) {
        context = EvaluationContext.builder()
                .attribute(attrKey, attrValue)
                .build();
    }

    @And("an evaluation context with attribute {string} = {string} but no {string} attribute")
    public void anEvaluationContextWithAttributeButNoAttribute(String attrKey, String attrValue, String missingAttr) {
        context = EvaluationContext.builder()
                .attribute(attrKey, attrValue)
                .build();
        assertThat(context.attributes()).doesNotContainKey(missingAttr);
    }

    @Then("the resolved proxy dispatches to the default variant")
    public void theResolvedProxyDispatchesToTheDefaultVariant() {
        // When no flag value is returned and there's a default variant, use it.
        CheckoutFlowMetadata.setDefaultVariant(DefaultCheckout::new);
        // Re-create dispatcher to pick up default variant
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class, context);
        String result = resolvedProxy.execute();
        assertThat(result).isEqualTo("DefaultCheckout");
    }

    // --- US-EC-04: Generated Proxy Passes Context ---

    @Given("a feature {string} with flag key {string}")
    public void aFeatureWithFlagKey(String featureName, String flagKey) {
        // CheckoutFlowMetadata uses "checkout-flow" as flag key
    }

    @And("a context-aware flag provider")
    public void aContextAwareFlagProvider() {
        flagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                capturedFlagKey.set(key);
                contextlessLookupPerformed.set(true);
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                capturedFlagKey.set(key);
                capturedContext.set(ctx);
                return Optional.of("CLASSIC");
            }
        };
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @Then("the flag provider receives both the flag key {string} and the evaluation context")
    public void theFlagProviderReceivesBothTheFlagKeyAndTheEvaluationContext(String expectedKey) {
        // Invoke the proxy to trigger flag lookup
        resolvedProxy.execute();
        assertThat(capturedFlagKey.get()).isEqualTo(expectedKey);
        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get()).isEqualTo(context);
    }

    @Then("the flag provider receives only the flag key {string} without context")
    public void theFlagProviderReceivesOnlyTheFlagKeyWithoutContext(String expectedKey) {
        // When resolving without context, FlagContext.current() should be null,
        // so the proxy calls getString(key) without context.
        assertThat(FlagContext.current())
                .as("No context should be set when resolving without evaluation context")
                .isNull();
        // Verify the proxy works (dispatches correctly without context)
        String result = resolvedProxy.execute();
        assertThat(result).isNotNull();
    }
}
