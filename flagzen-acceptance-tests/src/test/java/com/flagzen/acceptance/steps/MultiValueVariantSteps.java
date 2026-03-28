package com.flagzen.acceptance.steps;

import com.flagzen.FeatureDispatcher;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.BulkPricing;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.ClassicCheckout;
import com.flagzen.acceptance.fixtures.ModernCheckout;
import com.flagzen.acceptance.fixtures.PricingTier;
import com.flagzen.acceptance.fixtures.PricingTierMetadata;
import com.flagzen.acceptance.fixtures.RateLimiter;
import com.flagzen.acceptance.fixtures.RateLimiterMetadata;
import com.flagzen.acceptance.fixtures.StandardPricing;
import com.flagzen.acceptance.fixtures.ThrottledRate;
import com.flagzen.acceptance.fixtures.UnlimitedRate;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for multi-value variant mapping scenarios.
 * Tests that @Variant(value = {"V1", "V2"}) registers variant class under all values.
 */
public class MultiValueVariantSteps {

    private static final String PACKAGE = "com.example";

    @And("a variant {string} implementing {string} for string values {string} and {string}")
    public void aVariantImplementingForStringValues(String variantClass, String interfaceName,
                                                     String value1, String value2) {
        ensureFeatureSourceHook(interfaceName);
        String qualifiedName = PACKAGE + "." + variantClass;
        if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
            return;
        }
        SharedCompilationContext.markSourceAdded(qualifiedName);
        String method = SharedCompilationContext.getFeatureMethod(interfaceName);
        String methodDecl = (method != null)
                ? "    @Override\n    public void " + method + "() {}\n" : "";
        SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                qualifiedName,
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = {"%s", "%s"}, of = %s.class)
                public class %s implements %s {
                %s}
                """.formatted(PACKAGE, value1, value2, interfaceName, variantClass, interfaceName, methodDecl)
        ));
    }

    @And("a variant {string} implementing {string} for int values {int} and {int}")
    public void aVariantImplementingForIntValues(String variantClass, String interfaceName,
                                                  int value1, int value2) {
        addVariantSource(variantClass, interfaceName,
                "intValue = {%d, %d}".formatted(value1, value2), null);
    }

    @And("a variant {string} implementing {string} for int value {int}")
    public void aVariantImplementingForIntValue(String variantClass, String interfaceName, int value) {
        addVariantSource(variantClass, interfaceName,
                "intValue = %d".formatted(value), null);
    }

    @And("a variant {string} implementing {string} for long values {long} and {long}")
    public void aVariantImplementingForLongValues(String variantClass, String interfaceName,
                                                   long value1, long value2) {
        addVariantSource(variantClass, interfaceName,
                "longValue = {%dL, %dL}".formatted(value1, value2), null);
    }

    @And("a variant {string} implementing {string} for long value {long}")
    public void aVariantImplementingForLongValue(String variantClass, String interfaceName, long value) {
        addVariantSource(variantClass, interfaceName,
                "longValue = %dL".formatted(value), null);
    }

    @And("a variant {string} implementing {string} for double values {double} and {double}")
    public void aVariantImplementingForDoubleValues(String variantClass, String interfaceName,
                                                     double value1, double value2) {
        addVariantSource(variantClass, interfaceName,
                "doubleValue = {@CloseTo(value = %s), @CloseTo(value = %s)}".formatted(value1, value2),
                "import com.flagzen.CloseTo;");
    }

    @And("a repeated variant annotation on {string} for {string} with value {string}")
    public void aRepeatedVariantAnnotationWithValue(String variantClass, String interfaceName, String value) {
        // Adds a second @Variant annotation on an already-registered variant class.
        // We need to re-create the source with both annotations.
        addRepeatedVariantSource(variantClass, interfaceName, new String[]{value});
    }

    @And("a repeated variant annotation on {string} for {string} with values {string} and {string}")
    public void aRepeatedVariantAnnotationWithValues(String variantClass, String interfaceName,
                                                      String value1, String value2) {
        addRepeatedVariantSource(variantClass, interfaceName, new String[]{value1, value2});
    }

    @Given("a feature interface {string} with flag key {string} and fallback REQUIRED")
    public void aFeatureInterfaceWithFlagKeyAndFallbackRequired(String interfaceName, String key) {
        SharedCompilationContext.setFeatureKey(interfaceName, key);
        SharedCompilationContext.setFeatureFallback(interfaceName, "REQUIRED");
        SharedCompilationContext.addPreCompileHook(() ->
                SharedCompilationContext.ensureFeatureSource(interfaceName));
    }

    @And("an inner Variant enum on {string} with values {word}, {word}, {word}")
    public void anInnerVariantEnumOnWithThreeValues(String interfaceName, String val1, String val2, String val3) {
        String enumBlock = "    enum Variant { %s, %s, %s }\n".formatted(val1, val2, val3);
        SharedCompilationContext.setVariantEnumBlock(interfaceName, enumBlock);
    }

    @And("an inner Variant enum on {string} with values {word}, {word}")
    public void anInnerVariantEnumOnWithTwoValues(String interfaceName, String val1, String val2) {
        String enumBlock = "    enum Variant { %s, %s }\n".formatted(val1, val2);
        SharedCompilationContext.setVariantEnumBlock(interfaceName, enumBlock);
    }

    @Then("compilation fails with error containing {string}")
    public void compilationFailsWithErrorContaining(String expectedError) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(expectedError);
    }

    @And("the error identifies {string} as the conflicting value")
    public void theErrorIdentifiesAsTheConflictingValue(String value) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining(value);
    }

    @And("the error names both {string} and {string}")
    public void theErrorNamesBothAnd(String class1, String class2) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining(class1);
        assertThat(compilation).hadErrorContaining(class2);
    }

    @And("the error names {string} and {string}")
    public void theErrorNamesAnd(String class1, String class2) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining(class1);
        assertThat(compilation).hadErrorContaining(class2);
    }

    @And("the error shows the computed ranges")
    public void theErrorShowsTheComputedRanges() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        // The error message should include range notation like [x, y]
        assertThat(compilation).hadErrorContaining("[");
        assertThat(compilation).hadErrorContaining("]");
    }

    @And("the error suggests reducing delta or merging variants")
    public void theErrorSuggestsReducingDeltaOrMergingVariants() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("reduce delta");
    }

    @And("the error shows both ranges")
    public void theErrorShowsBothRanges() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("[");
        assertThat(compilation).hadErrorContaining("]");
    }

    @And("the error suggests reducing delta or removing the redundant entry")
    public void theErrorSuggestsReducingDeltaOrRemovingRedundant() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("reduce delta");
    }

    @And("a variant {string} implementing {string} with @CloseTo value {double} and delta {double}")
    public void aVariantImplementingWithCloseToValueAndDelta(String variantClass, String interfaceName,
                                                              double value, double delta) {
        addVariantSource(variantClass, interfaceName,
                "doubleValue = @CloseTo(value = %s, delta = %s)".formatted(value, delta),
                "import com.flagzen.CloseTo;");
    }

    @And("a variant {string} implementing {string} with @CloseTo value {double}")
    public void aVariantImplementingWithCloseToValue(String variantClass, String interfaceName,
                                                      double value) {
        addVariantSource(variantClass, interfaceName,
                "doubleValue = @CloseTo(value = %s)".formatted(value),
                "import com.flagzen.CloseTo;");
    }

    @And("a variant {string} implementing {string} with @CloseTo values {double} delta {double} and {double} delta {double}")
    public void aVariantImplementingWithCloseToValuesAndDeltas(String variantClass, String interfaceName,
                                                                double value1, double delta1,
                                                                double value2, double delta2) {
        addVariantSource(variantClass, interfaceName,
                "doubleValue = {@CloseTo(value = %s, delta = %s), @CloseTo(value = %s, delta = %s)}".formatted(
                        value1, delta1, value2, delta2),
                "import com.flagzen.CloseTo;");
    }

    @And("a variant {string} implementing {string} with @CloseTo values {double} and {double}")
    public void aVariantImplementingWithCloseToValues(String variantClass, String interfaceName,
                                                       double value1, double value2) {
        addVariantSource(variantClass, interfaceName,
                "doubleValue = {@CloseTo(value = %s), @CloseTo(value = %s)}".formatted(value1, value2),
                "import com.flagzen.CloseTo;");
    }

    @And("the error mentions missing implementation")
    public void theErrorMentionsMissingImplementation() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("has no implementation");
    }

    @And("the generated proxy maps int {int} to {string}")
    public void theGeneratedProxyMapsIntTo(int value, String variantClass) {
        assertMetadataMapsValueTo(String.valueOf(value), variantClass);
    }

    @And("the generated proxy maps long {long} to {string}")
    public void theGeneratedProxyMapsLongTo(long value, String variantClass) {
        assertMetadataMapsValueTo(String.valueOf(value), variantClass);
    }

    @And("the generated proxy maps double approximately {double} to {string}")
    public void theGeneratedProxyMapsDoubleApproximatelyTo(double value, String variantClass) {
        assertMetadataMapsValueTo(String.valueOf(value), variantClass);
    }

    // --- Runtime dispatch steps for multi-value scenario ---

    private final Map<String, Supplier<CheckoutFlow>> multiValueMap = new HashMap<>();
    private final Map<String, Supplier<PricingTier>> intMultiValueMap = new HashMap<>();
    private final Map<String, Supplier<RateLimiter>> longMultiValueMap = new HashMap<>();
    private InMemoryFlagProvider flagProvider;
    private FeatureDispatcher dispatcher;
    private CheckoutFlow resolvedProxy;
    private String callResult;
    private String activeFeatureType;
    private Exception caughtException;

    @Given("a compiled multi-value feature {string} with flag key {string}")
    public void aCompiledMultiValueFeatureWithFlagKey(String featureName, String flagKey) {
        // Fixtures are pre-compiled; metadata will be configured with multi-value mappings
        multiValueMap.clear();
        activeFeatureType = "STRING";
    }

    @Given("a compiled multi-value feature {string} with flag key {string} and type INT")
    public void aCompiledMultiValueFeatureWithFlagKeyAndTypeInt(String featureName, String flagKey) {
        intMultiValueMap.clear();
        activeFeatureType = "INT";
    }

    @Given("a compiled multi-value feature {string} with flag key {string} and type LONG")
    public void aCompiledMultiValueFeatureWithFlagKeyAndTypeLong(String featureName, String flagKey) {
        longMultiValueMap.clear();
        activeFeatureType = "LONG";
    }

    @And("{string} mapped to string values {string} and {string}")
    public void mappedToStringValuesAnd(String variantClass, String value1, String value2) {
        Supplier<CheckoutFlow> supplier = resolveVariantSupplier(variantClass);
        multiValueMap.put(value1, supplier);
        multiValueMap.put(value2, supplier);
    }

    @And("{string} mapped to string value {string}")
    public void mappedToStringValue(String variantClass, String value) {
        Supplier<CheckoutFlow> supplier = resolveVariantSupplier(variantClass);
        multiValueMap.put(value, supplier);
    }

    @And("a flag provider returning {string} for {string}")
    public void aFlagProviderReturningFor(String flagValue, String flagKey) {
        flagProvider = new InMemoryFlagProvider();
        flagProvider.set(flagKey, flagValue);
    }

    @And("{string} mapped to int values {int} and {int}")
    public void mappedToIntValuesAnd(String variantClass, int value1, int value2) {
        Supplier<PricingTier> supplier = resolvePricingTierSupplier(variantClass);
        intMultiValueMap.put(String.valueOf(value1), supplier);
        intMultiValueMap.put(String.valueOf(value2), supplier);
    }

    @And("{string} mapped to int value {int}")
    public void mappedToIntValue(String variantClass, int value) {
        Supplier<PricingTier> supplier = resolvePricingTierSupplier(variantClass);
        intMultiValueMap.put(String.valueOf(value), supplier);
    }

    @And("{string} mapped to long values {long} and {long}")
    public void mappedToLongValuesAnd(String variantClass, long value1, long value2) {
        Supplier<RateLimiter> supplier = resolveRateLimiterSupplier(variantClass);
        longMultiValueMap.put(String.valueOf(value1), supplier);
        longMultiValueMap.put(String.valueOf(value2), supplier);
    }

    @And("{string} mapped to long value {long}")
    public void mappedToLongValue(String variantClass, long value) {
        Supplier<RateLimiter> supplier = resolveRateLimiterSupplier(variantClass);
        longMultiValueMap.put(String.valueOf(value), supplier);
    }

    @And("the feature uses fallback strategy EXCEPTION")
    public void theFeatureUsesFallbackStrategyException() {
        // CheckoutFlowMetadata already declares EXCEPTION as default fallback strategy
    }

    @When("the developer resolves {string} through the multi-value dispatcher")
    public void theDeveloperResolvesThroughTheMultiValueDispatcher(String featureName) {
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
        switch (activeFeatureType) {
            case "INT" -> {
                PricingTierMetadata.setMultiValueVariants(Map.copyOf(intMultiValueMap));
                PricingTier proxy = dispatcher.resolve(PricingTier.class);
                callResult = proxy.execute();
            }
            case "LONG" -> {
                RateLimiterMetadata.setMultiValueVariants(Map.copyOf(longMultiValueMap));
                RateLimiter proxy = dispatcher.resolve(RateLimiter.class);
                callResult = proxy.execute();
            }
            default -> {
                CheckoutFlowMetadata.setMultiValueVariants(Map.copyOf(multiValueMap));
                resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
                callResult = resolvedProxy.execute();
            }
        }
    }

    @When("the developer resolves {string} through the multi-value dispatcher expecting fallback")
    public void theDeveloperResolvesThroughTheMultiValueDispatcherExpectingFallback(String featureName) {
        CheckoutFlowMetadata.setMultiValueVariants(Map.copyOf(multiValueMap));
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
        try {
            resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
            callResult = resolvedProxy.execute();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the {string} variant handles the call")
    public void theVariantHandlesTheCall(String expectedVariant) {
        assertThat(callResult).isEqualTo(expectedVariant);
    }

    @Then("an unmatched variant error is raised for the multi-value feature")
    public void anUnmatchedVariantErrorIsRaisedForTheMultiValueFeature() {
        assertThat(caughtException).isNotNull();
        assertThat(caughtException).isInstanceOf(UnmatchedVariantException.class);
    }

    private Supplier<CheckoutFlow> resolveVariantSupplier(String variantClass) {
        return switch (variantClass) {
            case "ClassicCheckout" -> ClassicCheckout::new;
            case "ModernCheckout" -> ModernCheckout::new;
            default -> throw new IllegalArgumentException("Unknown variant: " + variantClass);
        };
    }

    private Supplier<PricingTier> resolvePricingTierSupplier(String variantClass) {
        return switch (variantClass) {
            case "BulkPricing" -> BulkPricing::new;
            case "StandardPricing" -> StandardPricing::new;
            default -> throw new IllegalArgumentException("Unknown variant: " + variantClass);
        };
    }

    private Supplier<RateLimiter> resolveRateLimiterSupplier(String variantClass) {
        return switch (variantClass) {
            case "ThrottledRate" -> ThrottledRate::new;
            case "UnlimitedRate" -> UnlimitedRate::new;
            default -> throw new IllegalArgumentException("Unknown variant: " + variantClass);
        };
    }

    @And("the generated proxy maps {string} to {string}")
    public void theGeneratedProxyMaps(String value, String variantClass) {
        assertMetadataMapsValueTo(value, variantClass);
    }

    // --- Internal helpers ---

    private void addVariantSource(String variantClass, String interfaceName,
                                  String annotationAttr, String extraImport) {
        ensureFeatureSourceHook(interfaceName);
        String qualifiedName = PACKAGE + "." + variantClass;
        if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
            return;
        }
        SharedCompilationContext.markSourceAdded(qualifiedName);

        String extraImportLine = (extraImport != null) ? extraImport + "\n" : "";
        SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                qualifiedName,
                """
                package %s;

                import com.flagzen.Variant;
                %s
                @Variant(%s, of = %s.class)
                public class %s implements %s {
                }
                """.formatted(PACKAGE, extraImportLine, annotationAttr, interfaceName,
                        variantClass, interfaceName)
        ));
    }

    private void ensureFeatureSourceHook(String interfaceName) {
        SharedCompilationContext.addPreCompileHook(() ->
                SharedCompilationContext.ensureFeatureSource(interfaceName));
    }

    private void addRepeatedVariantSource(String variantClass, String interfaceName, String[] values) {
        // This step adds an additional @Variant annotation to a variant class that was already
        // added by a previous step. We need to retrieve the original source and augment it.
        // Since SharedCompilationContext already has the source, we track repeated annotations
        // and rebuild the source at pre-compile time.
        String qualifiedName = PACKAGE + "." + variantClass;
        String valueArray = values.length == 1
                ? "\"" + values[0] + "\""
                : "{\"" + String.join("\", \"", values) + "\"}";
        SharedCompilationContext.addRepeatedVariantAnnotation(
                qualifiedName, interfaceName, valueArray);
    }

    private void assertMetadataMapsValueTo(String value, String variantClass) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        // Search all generated metadata files for the mapping
        boolean found = false;
        for (JavaFileObject generated : compilation.generatedSourceFiles()) {
            String name = generated.getName();
            if (name.contains("_FlagZenMetadata")) {
                try {
                    String source = generated.getCharContent(false).toString();
                    if (source.contains(variantClass + "::new")) {
                        // Check the value is referenced
                        boolean hasValue = source.contains("\"" + value + "\"")
                                || source.contains(value + ",")
                                || source.contains(value + ")")
                                || source.contains("(" + value + ",")
                                || source.contains("put(" + value);
                        if (hasValue) {
                            found = true;
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new AssertionError("Failed to read generated metadata source", e);
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(found)
                .as("Metadata should map value '%s' to %s".formatted(value, variantClass))
                .isTrue();
    }
}
