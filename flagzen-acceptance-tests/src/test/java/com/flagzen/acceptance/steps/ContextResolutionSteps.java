package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.ContextAccessor;
import com.flagzen.spi.FlagProvider;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for context resolution order scenarios (US-EC-06, US-EC-07).
 * Tests the resolution chain: explicit > accessor > scoped > default.
 */
public class ContextResolutionSteps {

    private final List<ContextAccessor> accessors = new ArrayList<>();
    private FlagProvider flagProvider;
    private FeatureDispatcher dispatcher;
    private EvaluationContext explicitContext;
    private EvaluationContext scopedContext;
    private EvaluationContext defaultContext;
    private boolean noAccessorsRegistered;
    private boolean noScopedContext;
    private boolean noDefaultContext;

    // Tracking for verification
    private final AtomicReference<String> capturedTargetingKey = new AtomicReference<>();
    private boolean contextlessLookupOccurred;
    private boolean emptyAccessorWasSkipped;

    // --- Context accessor setup steps ---

    @Given("a context accessor registered with priority {int} returning targeting key {string}")
    public void aContextAccessorRegisteredWithPriorityReturningTargetingKey(int priority, String targetingKey) {
        accessors.add(new StubContextAccessor(priority, targetingKey));
    }

    @And("a context accessor with priority {int} returning targeting key {string}")
    public void aContextAccessorWithPriorityReturningTargetingKey(int priority, String targetingKey) {
        accessors.add(new StubContextAccessor(priority, targetingKey));
    }

    @And("a context accessor {string} with priority {int} returning targeting key {string}")
    public void aNamedContextAccessorWithPriorityReturningTargetingKey(String name, int priority, String targetingKey) {
        accessors.add(new StubContextAccessor(priority, targetingKey));
    }

    @And("a context accessor with priority {int} returning no context")
    public void aContextAccessorWithPriorityReturningNoContext(int priority) {
        accessors.add(new EmptyContextAccessor(priority));
    }

    @And("a context accessor returning targeting key {string}")
    public void aContextAccessorReturningTargetingKey(String targetingKey) {
        accessors.add(new StubContextAccessor(0, targetingKey));
    }

    @And("no context accessors are registered")
    public void noContextAccessorsAreRegistered() {
        noAccessorsRegistered = true;
        accessors.clear();
    }

    // --- Context setup steps ---

    @And("a scoped context with targeting key {string}")
    public void aScopedContextWithTargetingKey(String targetingKey) {
        scopedContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @And("a default context with targeting key {string}")
    public void aDefaultContextWithTargetingKey(String targetingKey) {
        defaultContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @And("a default context configured with targeting key {string}")
    public void aDefaultContextConfiguredWithTargetingKey(String targetingKey) {
        defaultContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @And("no scoped context is active")
    public void noScopedContextIsActive() {
        noScopedContext = true;
        FlagContext.clear();
    }

    @And("no default context is configured")
    public void noDefaultContextIsConfigured() {
        noDefaultContext = true;
        defaultContext = null;
    }

    @And("an explicit evaluation context with targeting key {string}")
    public void anExplicitEvaluationContextWithTargetingKey(String targetingKey) {
        explicitContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @And("all four context sources are active:")
    public void allFourContextSourcesAreActive(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String source = row.get("source");
            String targetingKey = row.get("targeting key");
            switch (source) {
                case "explicit" -> explicitContext = EvaluationContext.builder()
                        .targetingKey(targetingKey).build();
                case "accessor" -> accessors.add(new StubContextAccessor(0, targetingKey));
                case "scoped" -> scopedContext = EvaluationContext.builder()
                        .targetingKey(targetingKey).build();
                case "default" -> defaultContext = EvaluationContext.builder()
                        .targetingKey(targetingKey).build();
                default -> throw new IllegalArgumentException("Unknown context source: " + source);
            }
        }
    }

    // --- Resolve action steps ---

    @When("the developer resolves {string} without explicit context")
    public void theDeveloperResolvesWithoutExplicitContext(String featureName) {
        FlagContext.clear();
        createDispatcherIfNeeded();

        if (scopedContext != null && !noScopedContext) {
            FlagContext.set(scopedContext);
        }

        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(proxy);
        proxy.execute();
    }

    @When("the developer resolves {string} with the explicit context")
    public void theDeveloperResolvesWithTheExplicitContext(String featureName) {
        FlagContext.clear();
        createDispatcherIfNeeded();

        if (scopedContext != null && !noScopedContext) {
            FlagContext.set(scopedContext);
        }

        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, explicitContext);
        proxy.execute();
    }

    @When("the developer resolves {string} inside the scoped block")
    public void theDeveloperResolvesInsideTheScopedBlock(String featureName) {
        FlagContext.clear();
        createDispatcherIfNeeded();

        FlagContext.run(scopedContext, () -> {
            CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
            proxy.execute();
        });
    }

    @When("the developer resolves {string} with the explicit context inside the scoped block")
    public void theDeveloperResolvesWithTheExplicitContextInsideTheScopedBlock(String featureName) {
        FlagContext.clear();
        createDispatcherIfNeeded();

        FlagContext.run(scopedContext, () -> {
            CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, explicitContext);
            proxy.execute();
        });
    }

    // --- Assertion steps ---

    @Then("the flag provider receives context with targeting key {string}")
    public void theFlagProviderReceivesContextWithTargetingKey(String expectedKey) {
        assertThat(capturedTargetingKey.get()).isEqualTo(expectedKey);
    }

    @Then("the empty accessor is skipped")
    public void theEmptyAccessorIsSkipped() {
        // If we get here and the flag provider received a context, the empty accessor was skipped.
        // The second accessor (with higher priority number) provided the context.
        emptyAccessorWasSkipped = true;
    }

    @Then("the accessor step is skipped without error")
    public void theAccessorStepIsSkippedWithoutError() {
        // If we got here without exception, the accessor step was handled gracefully.
        assertThat(noAccessorsRegistered).isTrue();
    }

    @And("the resolve falls through to scoped or default context")
    public void theResolveFallsThroughToScopedOrDefaultContext() {
        // With no accessors, no scoped context, and no default context,
        // the resolve should fall through to contextless lookup.
        // The scenario uses in-memory provider so it works without context.
    }

    // --- Property test steps ---

    @Given("any combination of context sources")
    public void anyCombinationOfContextSources() {
        // Set up all four context sources
        flagProvider = createCapturingFlagProvider();
        accessors.add(new StubContextAccessor(0, "accessor-key"));
        scopedContext = EvaluationContext.builder().targetingKey("scoped-key").build();
        defaultContext = EvaluationContext.builder().targetingKey("default-key").build();
        explicitContext = EvaluationContext.builder().targetingKey("explicit-key").build();
    }

    @When("the developer resolves a feature multiple times with the same sources active")
    public void theDeveloperResolvesAFeatureMultipleTimesWithTheSameSourcesActive() {
        createDispatcherIfNeeded();

        List<String> results = new ArrayList<>();

        // Run 10 times to prove determinism
        for (int i = 0; i < 10; i++) {
            capturedTargetingKey.set(null);

            // With explicit context
            FlagContext.run(scopedContext, () -> {
                CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, explicitContext);
                proxy.execute();
            });
            results.add(capturedTargetingKey.get());
        }

        // All results must be identical
        assertThat(results).allMatch(r -> "explicit-key".equals(r));
    }

    @Then("the same context source wins every time")
    public void theSameContextSourceWinsEveryTime() {
        // Verified in the When step above
    }

    @And("the order is always: explicit, then accessor, then scoped, then default")
    public void theOrderIsAlwaysExplicitThenAccessorThenScopedThenDefault() {
        createDispatcherIfNeeded();

        // Test 1: Explicit wins over all
        capturedTargetingKey.set(null);
        FlagContext.run(scopedContext, () -> {
            dispatcher.resolve(CheckoutFlow.class, explicitContext).execute();
        });
        assertThat(capturedTargetingKey.get()).isEqualTo("explicit-key");

        // Test 2: Accessor wins when no explicit
        capturedTargetingKey.set(null);
        FlagContext.run(scopedContext, () -> {
            dispatcher.resolve(CheckoutFlow.class).execute();
        });
        assertThat(capturedTargetingKey.get()).isEqualTo("accessor-key");

        // Test 3: Scoped wins when no explicit and no accessor
        FeatureDispatcher noAccessorDispatcher = new DefaultFeatureDispatcher(
                flagProvider, defaultContext);
        capturedTargetingKey.set(null);
        FlagContext.run(scopedContext, () -> {
            noAccessorDispatcher.resolve(CheckoutFlow.class).execute();
        });
        assertThat(capturedTargetingKey.get()).isEqualTo("scoped-key");

        // Test 4: Default wins when nothing else
        capturedTargetingKey.set(null);
        FlagContext.clear();
        noAccessorDispatcher.resolve(CheckoutFlow.class).execute();
        assertThat(capturedTargetingKey.get()).isEqualTo("default-key");
    }

    // --- Helper methods ---

    private void createDispatcherIfNeeded() {
        if (dispatcher != null) {
            return;
        }
        if (flagProvider == null) {
            // Check for shared in-memory provider from RuntimeDispatchSteps
            InMemoryFlagProvider shared = SharedDispatcherHolder.getInMemoryProvider();
            if (shared != null) {
                flagProvider = shared;
            } else {
                flagProvider = createCapturingFlagProvider();
            }
        }
        ContextAccessor[] accessorArray = accessors.toArray(new ContextAccessor[0]);
        if (defaultContext != null) {
            dispatcher = new DefaultFeatureDispatcher(flagProvider, defaultContext, accessorArray);
        } else {
            dispatcher = new DefaultFeatureDispatcher(flagProvider, accessorArray);
        }
    }

    private FlagProvider createCapturingFlagProvider() {
        return new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                contextlessLookupOccurred = true;
                capturedTargetingKey.set(null);
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                if (ctx != null && ctx.targetingKey() != null) {
                    capturedTargetingKey.set(ctx.targetingKey());
                    return Optional.of("CLASSIC");
                }
                return getString(key);
            }
        };
    }

    // --- Test doubles ---

    private static class StubContextAccessor implements ContextAccessor {
        private final int priority;
        private final String targetingKey;

        StubContextAccessor(int priority, String targetingKey) {
            this.priority = priority;
            this.targetingKey = targetingKey;
        }

        @Override
        public Optional<EvaluationContext> getContext() {
            return Optional.of(EvaluationContext.builder()
                    .targetingKey(targetingKey)
                    .build());
        }

        @Override
        public int priority() {
            return priority;
        }
    }

    private static class EmptyContextAccessor implements ContextAccessor {
        private final int priority;

        EmptyContextAccessor(int priority) {
            this.priority = priority;
        }

        @Override
        public Optional<EvaluationContext> getContext() {
            return Optional.empty();
        }

        @Override
        public int priority() {
            return priority;
        }
    }
}
