package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.DarkMode;
import com.flagzen.acceptance.fixtures.DarkModeMetadata;
import com.flagzen.acceptance.fixtures.DefaultRetry;
import com.flagzen.acceptance.fixtures.RateLimiter;
import com.flagzen.acceptance.fixtures.RetryStrategy;
import com.flagzen.acceptance.fixtures.RetryStrategyMetadata;
import com.flagzen.acceptance.fixtures.SamplingStrategy;
import com.flagzen.acceptance.fixtures.SamplingStrategyMetadata;
import com.flagzen.acceptance.fixtures.DoubleVariantEntry;
import com.flagzen.acceptance.fixtures.LowSampling;
import com.flagzen.acceptance.fixtures.MediumSampling;
import com.flagzen.acceptance.fixtures.NearLowSampling;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.ContextAccessor;
import com.flagzen.spi.FlagProvider;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for typed dispatch scenarios (milestone-3-typed-dispatch.feature).
 * Covers INT, BOOLEAN, LONG, DOUBLE dispatch, evaluation context integration,
 * and FlagProvider typed accessor methods.
 */
public class TypedDispatchSteps {

    private InMemoryFlagProvider flagProvider;
    private FlagProvider contextAwareFlagProvider;
    private DefaultFeatureDispatcher dispatcher;
    private String dispatchResult;
    private Exception caughtException;
    private boolean noopResult;

    // FlagProvider accessor test state
    private OptionalInt readInt;
    private Optional<Boolean> readBoolean;
    private OptionalLong readLong;
    private OptionalDouble readDouble;

    // DOUBLE variant accumulator
    private final List<DoubleVariantEntry> doubleVariants = new ArrayList<>();

    // --- Feature + variant setup (Given steps) ---

    @Given("a feature {string} with type INT and variants for integer values {int} and {int}")
    public void featureWithIntVariants(String featureName, int v1, int v2) {
        // RetryStrategy fixtures are pre-compiled and registered via ServiceLoader
    }

    @Given("a feature {string} with type INT and fallback EXCEPTION")
    public void featureWithIntAndFallbackException(String featureName) {
        RetryStrategyMetadata.setFallbackStrategy(com.flagzen.FallbackStrategy.EXCEPTION);
    }

    @Given("a feature {string} with type INT and a default variant {string}")
    public void featureWithIntAndDefaultVariant(String featureName, String defaultName) {
        RetryStrategyMetadata.setDefaultVariant(DefaultRetry::new);
    }

    @Given("a feature {string} with type INT and fallback NOOP")
    public void featureWithIntAndFallbackNoop(String featureName) {
        RetryStrategyMetadata.setFallbackStrategy(com.flagzen.FallbackStrategy.NOOP);
    }

    @Given("a boolean feature {string} with variants for true and false")
    public void booleanFeatureWithVariants(String featureName) {
        DarkModeMetadata.enableBooleanDispatch();
    }

    @Given("a feature {string} with type LONG and variants for long values {long} and {long}")
    public void featureWithLongVariants(String featureName, long v1, long v2) {
        // RateLimiter fixtures are pre-compiled and registered via ServiceLoader
    }

    @Given("a double feature {string}")
    public void doubleFeature(String featureName) {
        doubleVariants.clear();
    }

    @Given("a feature {string} with type DOUBLE and fallback EXCEPTION")
    public void featureWithDoubleAndFallbackException(String featureName) {
        doubleVariants.clear();
        SamplingStrategyMetadata.setFallbackStrategy(com.flagzen.FallbackStrategy.EXCEPTION);
    }

    // --- DOUBLE variant definitions ---

    @And("a dispatch variant {string} at double value {double} with default tolerance")
    public void variantWithDoubleDefaultTolerance(String variantName, double value) {
        doubleVariants.add(new DoubleVariantEntry(
                value, 1e-10, supplierForSamplingVariant(variantName)));
        SamplingStrategyMetadata.setVariants(List.copyOf(doubleVariants));
    }

    @And("a dispatch variant {string} at double value {double} with tolerance {double}")
    public void variantWithDoubleAndTolerance(String variantName, double value, double tolerance) {
        doubleVariants.add(new DoubleVariantEntry(
                value, tolerance, supplierForSamplingVariant(variantName)));
        SamplingStrategyMetadata.setVariants(List.copyOf(doubleVariants));
    }

    @And("variants with double values {double} and {double}")
    public void variantsWithDoubleValues(double v1, double v2) {
        doubleVariants.clear();
        doubleVariants.add(new DoubleVariantEntry(v1, 1e-10, LowSampling::new));
        doubleVariants.add(new DoubleVariantEntry(v2, 1e-10, MediumSampling::new));
        SamplingStrategyMetadata.setVariants(List.copyOf(doubleVariants));
    }

    // --- Flag provider setup ---

    @And("a flag provider returning integer {int} for {string}")
    public void flagProviderReturningInt(int value, String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, String.valueOf(value));
    }

    @Given("a flag provider initially returning integer {int} for {string}")
    public void flagProviderInitiallyReturningInt(int value, String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, String.valueOf(value));
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
    }

    @And("a flag provider returning boolean true for {string}")
    public void flagProviderReturningBooleanTrue(String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, "true");
    }

    @And("a flag provider returning boolean false for {string}")
    public void flagProviderReturningBooleanFalse(String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, "false");
    }

    @And("a flag provider returning long {long} for {string}")
    public void flagProviderReturningLong(long value, String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, String.valueOf(value));
    }

    @And("a flag provider returning double {double} for {string}")
    public void flagProviderReturningDouble(double value, String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, String.valueOf(value));
    }

    @And("a flag provider returning no value for {string}")
    public void flagProviderReturningNoValue(String key) {
        flagProvider = new InMemoryFlagProvider();
        // No value set for key
    }

    @Given("a flag provider returning integer {int} for {string} regardless of context")
    public void flagProviderReturningIntRegardless(int value, String key) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, String.valueOf(value));
    }

    // --- Context-aware flag providers ---

    @Given("a context-aware flag provider for {string}")
    public void contextAwareFlagProviderFor(String key) {
        // The "it returns" step will configure this
    }

    @And("it returns integer {int} for plan {string} and integer {int} for plan {string}")
    public void itReturnsIntForPlanAndIntForPlan(int v1, String plan1, int v2, String plan2) {
        contextAwareFlagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of(String.valueOf(v1));
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (context != null) {
                    Object plan = context.attributes().get("plan");
                    if (plan2.equals(plan)) return Optional.of(String.valueOf(v2));
                    if (plan1.equals(plan)) return Optional.of(String.valueOf(v1));
                }
                return getString(key);
            }
        };
    }

    @Given("a context-aware flag provider returning boolean true for {string} when preference is {string}")
    public void contextAwareFlagProviderBooleanTrue(String key, String prefValue) {
        DarkModeMetadata.enableBooleanDispatch();
        contextAwareFlagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key2) {
                return Optional.of("false");
            }

            @Override
            public Optional<String> getString(String key2, EvaluationContext context) {
                if (context != null && prefValue.equals(context.attributes().get("preference"))) {
                    return Optional.of("true");
                }
                return getString(key2);
            }
        };
    }

    @Given("a context-aware flag provider for {string} returning integer {int} for free and {int} for enterprise")
    public void contextAwareFlagProviderIntForFreeAndEnterprise(String key, int freeVal, int entVal) {
        contextAwareFlagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String k) {
                return Optional.of(String.valueOf(freeVal));
            }

            @Override
            public Optional<String> getString(String k, EvaluationContext context) {
                if (context != null) {
                    Object plan = context.attributes().get("plan");
                    if ("enterprise".equals(plan)) return Optional.of(String.valueOf(entVal));
                    if ("free".equals(plan)) return Optional.of(String.valueOf(freeVal));
                }
                return getString(k);
            }
        };
    }

    @Given("a context accessor providing context with plan {string}")
    public void contextAccessorProvidingContext(String plan) {
        // Will be used when creating the dispatcher in the When step
        ContextAccessor accessor = new ContextAccessor() {
            @Override
            public Optional<EvaluationContext> getContext() {
                return Optional.of(EvaluationContext.builder().attribute("plan", plan).build());
            }

            @Override
            public int priority() {
                return 0;
            }
        };
        SharedTypedDispatchHolder.setContextAccessor(accessor);
    }

    @Given("a scoped context block with plan {string}")
    public void scopedContextBlockWithPlan(String plan) {
        contextAwareFlagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("3");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (context != null) {
                    Object p = context.attributes().get("plan");
                    if ("enterprise".equals(p)) return Optional.of("10");
                    if ("free".equals(p)) return Optional.of("3");
                }
                return getString(key);
            }
        };
        EvaluationContext ctx = EvaluationContext.builder().attribute("plan", plan).build();
        FlagContext.set(ctx);
    }

    // --- Resolution (When steps) ---

    @When("the developer resolves {string}")
    public void theDeveloperResolves(String featureName) {
        try {
            ensureDispatcher();
            dispatchResult = resolveAndExecute(featureName);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the flag value changes to integer {int}")
    public void theFlagValueChangesToInteger(int newValue) {
        flagProvider.set("max-retries", String.valueOf(newValue));
    }

    @And("the developer calls a method on the resolved {string} proxy")
    public void theDeveloperCallsMethodOnResolvedProxy(String featureName) {
        try {
            // Dispatcher already exists from the Given step that created it
            dispatchResult = resolveAndExecute(featureName);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer resolves {string} with context targeting plan {string}")
    public void theDeveloperResolvesWithContextTargetingPlan(String featureName, String plan) {
        try {
            FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
            dispatcher = new DefaultFeatureDispatcher(provider);
            EvaluationContext ctx = EvaluationContext.builder().attribute("plan", plan).build();
            dispatchResult = resolveAndExecuteWithContext(featureName, ctx);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer resolves {string} with context attribute preference {string}")
    public void theDeveloperResolvesWithContextAttributePreference(String featureName, String prefValue) {
        try {
            FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
            dispatcher = new DefaultFeatureDispatcher(provider);
            EvaluationContext ctx = EvaluationContext.builder().attribute("preference", prefValue).build();
            dispatchResult = resolveAndExecuteWithContext(featureName, ctx);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer runs a scoped context block targeting plan {string}")
    public void theDeveloperRunsScopedContextBlock(String plan) {
        FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
        dispatcher = new DefaultFeatureDispatcher(provider);
        EvaluationContext ctx = EvaluationContext.builder().attribute("plan", plan).build();
        FlagContext.set(ctx);
    }

    @And("resolves {string} inside the block")
    public void resolvesInsideTheBlock(String featureName) {
        try {
            dispatchResult = resolveAndExecute(featureName);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer resolves {string} with explicit context targeting plan {string} inside the block")
    public void theDeveloperResolvesWithExplicitContextInsideBlock(String featureName, String plan) {
        try {
            FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
            dispatcher = new DefaultFeatureDispatcher(provider);
            EvaluationContext explicitCtx = EvaluationContext.builder().attribute("plan", plan).build();
            dispatchResult = resolveAndExecuteWithContext(featureName, explicitCtx);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer resolves typed feature {string} without explicit context")
    public void theDeveloperResolvesTypedWithoutExplicitContext(String featureName) {
        try {
            FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
            ContextAccessor accessor = SharedTypedDispatchHolder.getContextAccessor();
            if (accessor != null) {
                dispatcher = new DefaultFeatureDispatcher(provider, accessor);
            } else {
                dispatcher = new DefaultFeatureDispatcher(provider);
            }
            dispatchResult = resolveAndExecute(featureName);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the developer resolves {string} without any evaluation context")
    public void theDeveloperResolvesWithoutAnyContext(String featureName) {
        try {
            ensureDispatcher();
            FlagContext.clear();
            dispatchResult = resolveAndExecute(featureName);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    // --- FlagProvider accessor tests (When steps) ---

    @Given("a flag provider with flag {string} having string value {string}")
    public void flagProviderWithStringValue(String key, String value) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(key, value);
    }

    @Given("a flag provider with no flag {string}")
    public void flagProviderWithNoFlag(String key) {
        flagProvider = new InMemoryFlagProvider();
    }

    @When("the developer reads the integer value for {string}")
    public void readsIntegerValue(String key) {
        readInt = flagProvider.getInt(key);
    }

    @When("the developer reads the boolean value for {string}")
    public void readsBooleanValue(String key) {
        readBoolean = flagProvider.getBoolean(key);
    }

    @When("the developer reads the long value for {string}")
    public void readsLongValue(String key) {
        readLong = flagProvider.getLong(key);
    }

    @When("the developer reads the double value for {string}")
    public void readsDoubleValue(String key) {
        readDouble = flagProvider.getDouble(key);
    }

    @When("the developer reads the boolean, integer, long, and double values for {string}")
    public void readsAllTypedValues(String key) {
        readBoolean = flagProvider.getBoolean(key);
        readInt = flagProvider.getInt(key);
        readLong = flagProvider.getLong(key);
        readDouble = flagProvider.getDouble(key);
    }

    // --- Context-aware conditional API steps (Phase 05) ---

    @And("it returns string {string} for targeting key {string}")
    public void itReturnsStringForTargetingKey(String value, String targetingKey) {
        contextAwareFlagProvider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.empty();
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (context != null && targetingKey.equals(context.targetingKey())) {
                    return Optional.of(value);
                }
                return getString(key);
            }
        };
    }

    @When("the developer reads the integer value for {string} with context targeting {string}")
    public void readsIntegerValueWithContext(String key, String targetingKey) {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey(targetingKey).build();
        readInt = contextAwareFlagProvider.getInt(key, ctx);
    }

    @When("the developer reads the boolean value for {string} with context targeting {string}")
    public void readsBooleanValueWithContext(String key, String targetingKey) {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey(targetingKey).build();
        readBoolean = contextAwareFlagProvider.getBoolean(key, ctx);
    }

    @When("the developer reads the long value for {string} with context targeting {string}")
    public void readsLongValueWithContext(String key, String targetingKey) {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey(targetingKey).build();
        readLong = contextAwareFlagProvider.getLong(key, ctx);
    }

    @When("the developer reads the double value for {string} with context targeting {string}")
    public void readsDoubleValueWithContext(String key, String targetingKey) {
        EvaluationContext ctx = EvaluationContext.builder().targetingKey(targetingKey).build();
        readDouble = contextAwareFlagProvider.getDouble(key, ctx);
    }

    // --- Assertions (Then steps) ---

    @Then("{string} handles the method call")
    public void handlesTheMethodCall(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("{string} handles the call")
    public void handlesTheCall(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("resolution fails with an unmatched variant error listing known values {int} and {int}")
    public void resolutionFailsListingKnownValues(int v1, int v2) {
        assertThat(caughtException).isInstanceOf(UnmatchedVariantException.class);
        assertThat(caughtException.getMessage())
                .contains(String.valueOf(v1))
                .contains(String.valueOf(v2));
    }

    @Then("the NOOP proxy is used returning safe defaults")
    public void noopProxyReturnsSafeDefaults() {
        assertThat(caughtException).as("No exception expected").isNull();
        // NOOP proxy returns null for method calls
        assertThat(dispatchResult).isNull();
    }

    @Then("resolution fails with an unmatched variant error")
    public void resolutionFailsWithUnmatchedVariantError() {
        assertThat(caughtException).isInstanceOf(UnmatchedVariantException.class);
    }

    @Then("{string} is selected")
    public void isSelected(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("{string} is selected because the value is within tolerance")
    public void isSelectedBecauseWithinTolerance(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("{string} is selected as the first match")
    public void isSelectedAsFirstMatch(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("the explicit context wins and {string} is selected")
    public void explicitContextWinsAndIsSelected(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("the accessor-provided context is used and {string} is selected")
    public void accessorProvidedContextIsUsedAndIsSelected(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @Then("{string} is selected based on the contextless flag value")
    public void isSelectedBasedOnContextlessFlagValue(String expectedVariant) {
        assertThat(caughtException).as("No exception expected").isNull();
        assertThat(dispatchResult).isEqualTo(expectedVariant);
    }

    @And("after the block exits the scoped context is cleared")
    public void afterBlockExitsScopedContextIsCleared() {
        FlagContext.clear();
        assertThat(FlagContext.current()).isNull();
    }

    // --- FlagProvider accessor assertions ---

    @Then("integer {int} is returned")
    public void integerIsReturned(int expected) {
        assertThat(readInt).isPresent();
        assertThat(readInt.getAsInt()).isEqualTo(expected);
    }

    @Then("boolean true is returned")
    public void booleanTrueIsReturned() {
        assertThat(readBoolean).isPresent();
        assertThat(readBoolean.get()).isTrue();
    }

    @Then("boolean false is returned")
    public void booleanFalseIsReturned() {
        assertThat(readBoolean).isPresent();
        assertThat(readBoolean.get()).isFalse();
    }

    @Then("long {long} is returned")
    public void longIsReturned(long expected) {
        assertThat(readLong).isPresent();
        assertThat(readLong.getAsLong()).isEqualTo(expected);
    }

    @Then("no value is returned")
    public void noValueIsReturned() {
        // At least one of the typed reads should be empty
        if (readBoolean != null) assertThat(readBoolean).isEmpty();
        else if (readInt != null) assertThat(readInt).isEmpty();
        else if (readLong != null) assertThat(readLong).isEmpty();
        else if (readDouble != null) assertThat(readDouble).isEmpty();
    }

    @Then("double {double} is returned")
    public void doubleIsReturned(double expected) {
        assertThat(readDouble).isPresent();
        assertThat(readDouble.getAsDouble()).isEqualTo(expected);
    }

    @Then("all return no value")
    public void allReturnNoValue() {
        assertThat(readBoolean).isEmpty();
        assertThat(readInt).isEmpty();
        assertThat(readLong).isEmpty();
        assertThat(readDouble).isEmpty();
    }

    // --- Internal helpers ---

    private void ensureDispatcher() {
        if (dispatcher == null) {
            FlagProvider provider = contextAwareFlagProvider != null ? contextAwareFlagProvider : flagProvider;
            dispatcher = new DefaultFeatureDispatcher(provider);
        }
    }

    private String resolveAndExecute(String featureName) {
        return switch (featureName) {
            case "RetryStrategy" -> {
                RetryStrategy proxy = dispatcher.resolve(RetryStrategy.class);
                yield proxy.execute();
            }
            case "DarkMode" -> {
                DarkMode proxy = dispatcher.resolve(DarkMode.class);
                yield proxy.isEnabled() ? "DarkModeOn" : "DarkModeOff";
            }
            case "RateLimiter" -> {
                RateLimiter proxy = dispatcher.resolve(RateLimiter.class);
                yield proxy.execute();
            }
            case "SamplingStrategy" -> {
                SamplingStrategy proxy = dispatcher.resolve(SamplingStrategy.class);
                yield proxy.execute();
            }
            default -> throw new IllegalArgumentException("Unknown feature: " + featureName);
        };
    }

    private String resolveAndExecuteWithContext(String featureName, EvaluationContext context) {
        return switch (featureName) {
            case "RetryStrategy" -> {
                RetryStrategy proxy = dispatcher.resolve(RetryStrategy.class, context);
                yield proxy.execute();
            }
            case "DarkMode" -> {
                DarkMode proxy = dispatcher.resolve(DarkMode.class, context);
                yield proxy.isEnabled() ? "DarkModeOn" : "DarkModeOff";
            }
            case "RateLimiter" -> {
                RateLimiter proxy = dispatcher.resolve(RateLimiter.class, context);
                yield proxy.execute();
            }
            case "SamplingStrategy" -> {
                SamplingStrategy proxy = dispatcher.resolve(SamplingStrategy.class, context);
                yield proxy.execute();
            }
            default -> throw new IllegalArgumentException("Unknown feature: " + featureName);
        };
    }

    private java.util.function.Supplier<SamplingStrategy> supplierForSamplingVariant(String name) {
        return switch (name) {
            case "LowSampling" -> LowSampling::new;
            case "MediumSampling" -> MediumSampling::new;
            case "NearLowSampling" -> NearLowSampling::new;
            default -> throw new IllegalArgumentException("Unknown sampling variant: " + name);
        };
    }
}
