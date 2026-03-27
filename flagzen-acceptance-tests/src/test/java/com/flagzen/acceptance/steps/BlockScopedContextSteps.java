package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.PaymentMethod;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.FlagProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for block-scoped FlagContext.run() scenarios (US-EC-05, US-EC-08).
 */
public class BlockScopedContextSteps {

    private EvaluationContext outerContext;
    private EvaluationContext innerContext;
    private FeatureDispatcher dispatcher;
    private FlagProvider flagProvider;
    private CheckoutFlow supplierResult;
    private String supplierDispatchResult;
    private Exception caughtException;

    // Tracking for flag provider verification
    private final ConcurrentMap<String, String> capturedTargetingKeys = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastCapturedTargetingKey = new AtomicReference<>();
    private boolean contextlessLookupOccurred;

    // Thread safety tracking
    private final ConcurrentMap<String, String> threadTargetingKeys = new ConcurrentHashMap<>();
    private volatile boolean crossThreadLeakage;

    /**
     * Retrieves the evaluation context set by shared steps in EvalContextSteps.
     */
    private EvaluationContext getSharedContext() {
        return SharedDispatcherHolder.getEvalContext();
    }

    // --- Flag provider setup steps ---

    @Given("a flag provider that uses targeting key for resolution")
    public void aFlagProviderThatUsesTargetingKeyForResolution() {
        flagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                contextlessLookupOccurred = true;
                lastCapturedTargetingKey.set(null);
                return Optional.of(defaultVariantForKey(key));
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                if (ctx != null && ctx.targetingKey() != null) {
                    capturedTargetingKeys.put(key, ctx.targetingKey());
                    lastCapturedTargetingKey.set(ctx.targetingKey());
                    return Optional.of(defaultVariantForKey(key));
                }
                return getString(key);
            }

            private String defaultVariantForKey(String key) {
                return "payment-method".equals(key) ? "CREDIT_CARD" : "CLASSIC";
            }
        };
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @And("a flag provider that returns {string} when targeting key is {string}")
    public void aFlagProviderThatReturnsWhenTargetingKeyIs(String returnValue, String targetingKey) {
        flagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                if (ctx != null && targetingKey.equals(ctx.targetingKey())) {
                    return Optional.of(returnValue);
                }
                return getString(key);
            }
        };
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    // --- Context setup steps ---

    @Given("an outer evaluation context with targeting key {string}")
    public void anOuterEvaluationContextWithTargetingKey(String targetingKey) {
        outerContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    @And("an inner evaluation context with targeting key {string}")
    public void anInnerEvaluationContextWithTargetingKey(String targetingKey) {
        innerContext = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();
    }

    // --- Scoped block action steps ---

    @When("the developer wraps resolve calls for {string} and {string} inside a scoped context block")
    public void theDeveloperWrapsResolveCallsInsideAScopedContextBlock(String feature1, String feature2) {
        EvaluationContext context = getSharedContext();
        FlagContext.run(context, () -> {
            CheckoutFlow checkoutProxy = dispatcher.resolve(CheckoutFlow.class);
            PaymentMethod paymentProxy = dispatcher.resolve(PaymentMethod.class);
            // Invoke proxies to trigger flag lookups within the scoped context
            checkoutProxy.execute();
            paymentProxy.execute();
        });
    }

    @Then("both flag lookups receive the context with targeting key {string}")
    public void bothFlagLookupsReceiveTheContextWithTargetingKey(String expectedKey) {
        assertThat(capturedTargetingKeys.get("checkout-flow")).isEqualTo(expectedKey);
        assertThat(capturedTargetingKeys.get("payment-method")).isEqualTo(expectedKey);
    }

    @When("the developer completes a scoped context block with that context")
    public void theDeveloperCompletesAScopedContextBlockWithThatContext() {
        EvaluationContext context = getSharedContext();
        FlagContext.run(context, () -> {
            dispatcher.resolve(CheckoutFlow.class);
        });
    }

    @When("the developer resolves {string} after the block")
    public void theDeveloperResolvesAfterTheBlock(String featureName) {
        // Clear proxy cache by creating a fresh dispatcher
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
        contextlessLookupOccurred = false;
        dispatcher.resolve(CheckoutFlow.class);
    }

    @Then("no scoped context is available for that resolve call")
    public void noScopedContextIsAvailableForThatResolveCall() {
        assertThat(FlagContext.current()).isNull();
    }

    // --- Nesting steps ---

    @When("the developer nests an inner scoped block inside an outer scoped block")
    public void theDeveloperNestsAnInnerScopedBlockInsideAnOuterScopedBlock() {
        // Nesting happens; resolve in next step
    }

    @And("resolves {string} inside the inner block")
    public void resolvesInsideTheInnerBlock(String featureName) {
        FlagContext.run(outerContext, () -> {
            FlagContext.run(innerContext, () -> {
                dispatcher.resolve(CheckoutFlow.class).execute();
            });
        });
    }

    @Then("the flag provider receives targeting key {string}")
    public void theFlagProviderReceivesTargetingKey(String expectedKey) {
        assertThat(lastCapturedTargetingKey.get()).isEqualTo(expectedKey);
    }

    @When("the developer exits the inner scoped block but remains in the outer block")
    public void theDeveloperExitsTheInnerScopedBlockButRemainsInTheOuterBlock() {
        FlagContext.run(outerContext, () -> {
            FlagContext.run(innerContext, () -> {
                // inner block completes
            });
            // After inner exits, resolve and invoke in outer scope
            dispatcher.resolve(CheckoutFlow.class).execute();
        });
    }

    @And("resolves {string} in the outer scope")
    public void resolvesInTheOuterScope(String featureName) {
        // Resolution already happened in the When step above
    }

    // --- Supplier steps ---

    @When("the developer resolves {string} inside a scoped supplier block")
    public void theDeveloperResolvesInsideAScopedSupplierBlock(String featureName) {
        EvaluationContext context = getSharedContext();
        supplierDispatchResult = FlagContext.run(context, () -> {
            CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
            return proxy.execute();
        });
        supplierResult = dispatcher.resolve(CheckoutFlow.class);
    }

    @Then("the returned result is the resolved {string} proxy")
    public void theReturnedResultIsTheResolvedProxy(String featureName) {
        assertThat(supplierResult).isNotNull();
    }

    @And("the proxy dispatches to the {string} variant")
    public void theProxyDispatchesToTheVariant(String expectedVariant) {
        String titleCase = expectedVariant.substring(0, 1).toUpperCase()
                + expectedVariant.substring(1).toLowerCase();
        assertThat(supplierDispatchResult).isEqualTo(titleCase + "Checkout");
    }

    // --- Exception steps ---

    @When("a scoped context block throws an exception")
    public void aScopedContextBlockThrowsAnException() {
        EvaluationContext context = getSharedContext();
        try {
            FlagContext.run(context, () -> {
                throw new RuntimeException("block failure");
            });
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the exception propagates to the caller")
    public void theExceptionPropagatesToTheCaller() {
        assertThat(caughtException)
                .isNotNull()
                .isInstanceOf(RuntimeException.class)
                .hasMessage("block failure");
    }

    @And("the scoped context is cleaned up")
    public void theScopedContextIsCleanedUp() {
        assertThat(FlagContext.current()).isNull();
    }

    @And("subsequent resolve calls do not see targeting key {string}")
    public void subsequentResolveCallsDoNotSeeTargetingKey(String targetingKey) {
        assertThat(FlagContext.current()).isNull();
    }

    // --- Null rejection steps ---

    @When("the developer attempts to run a scoped block with null context")
    public void theDeveloperAttemptsToRunAScopedBlockWithNullContext() {
        try {
            FlagContext.run(null, () -> { /* no-op */ });
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the operation is rejected with a clear error message")
    public void theOperationIsRejectedWithAClearErrorMessage() {
        assertThat(caughtException)
                .isNotNull()
                .isInstanceOf(com.flagzen.FlagZenException.class);
        assertThat(caughtException.getMessage()).containsIgnoringCase("context");
    }

    // --- Runtime version steps ---

    @When("the developer resolves {string} inside a scoped context block")
    public void theDeveloperResolvesInsideAScopedContextBlock(String featureName) {
        EvaluationContext context = getSharedContext();
        FlagContext.run(context, () -> {
            CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
            SharedProxyHolder.set(proxy);
            SharedProxyHolder.setLastDispatchResult(proxy.execute());
        });
    }

    @And("the context is properly scoped to the block regardless of the carrier mechanism")
    public void theContextIsProperlyScoped() {
        assertThat(FlagContext.current()).isNull();
    }

    // --- Thread safety steps ---

    @Given("two threads each running scoped context blocks with different targeting keys")
    public void twoThreadsEachRunning() throws InterruptedException {
        FlagProvider threadSafeProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                if (ctx != null) {
                    threadTargetingKeys.put(Thread.currentThread().getName(), ctx.targetingKey());
                    return Optional.of("CLASSIC");
                }
                return getString(key);
            }
        };

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicReference<String> thread1Seen = new AtomicReference<>();
        AtomicReference<String> thread2Seen = new AtomicReference<>();

        FeatureDispatcher dispatcher1 = new DefaultFeatureDispatcher(threadSafeProvider);
        FeatureDispatcher dispatcher2 = new DefaultFeatureDispatcher(threadSafeProvider);

        EvaluationContext ctx1 = EvaluationContext.builder().targetingKey("thread-1-key").build();
        EvaluationContext ctx2 = EvaluationContext.builder().targetingKey("thread-2-key").build();

        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                FlagContext.run(ctx1, () -> {
                    dispatcher1.resolve(CheckoutFlow.class);
                    thread1Seen.set(FlagContext.current().targetingKey());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        }, "test-thread-1");

        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                FlagContext.run(ctx2, () -> {
                    dispatcher2.resolve(CheckoutFlow.class);
                    thread2Seen.set(FlagContext.current().targetingKey());
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        }, "test-thread-2");

        t1.start();
        t2.start();
        startLatch.countDown();
        doneLatch.await();

        crossThreadLeakage = !"thread-1-key".equals(thread1Seen.get())
                || !"thread-2-key".equals(thread2Seen.get());

        threadTargetingKeys.put("thread1-seen", thread1Seen.get());
        threadTargetingKeys.put("thread2-seen", thread2Seen.get());
    }

    @Then("each thread's resolve calls use only its own targeting key")
    public void eachThreadUsesOwnTargetingKey() {
        assertThat(threadTargetingKeys.get("thread1-seen")).isEqualTo("thread-1-key");
        assertThat(threadTargetingKeys.get("thread2-seen")).isEqualTo("thread-2-key");
    }

    @And("no cross-thread context leakage occurs")
    public void noCrossThreadContextLeakageOccurs() {
        assertThat(crossThreadLeakage).isFalse();
    }
}
