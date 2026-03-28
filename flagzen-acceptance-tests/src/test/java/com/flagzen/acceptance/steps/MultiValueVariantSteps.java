package com.flagzen.acceptance.steps;

import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.ClassicCheckout;
import com.flagzen.acceptance.fixtures.ModernCheckout;
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
        String method = SharedCompilationContext.getFeatureMethod(interfaceName);
        String methodDecl = (method != null)
                ? "    @Override\n    public void " + method + "() {}\n" : "";
        SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                PACKAGE + "." + variantClass,
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = {"%s", "%s"}, of = %s.class)
                public class %s implements %s {
                %s}
                """.formatted(PACKAGE, value1, value2, interfaceName, variantClass, interfaceName, methodDecl)
        ));
        SharedCompilationContext.markSourceAdded(PACKAGE + "." + variantClass);
    }

    // --- Runtime dispatch steps for multi-value scenario ---

    private final Map<String, Supplier<CheckoutFlow>> multiValueMap = new HashMap<>();
    private InMemoryFlagProvider flagProvider;
    private FeatureDispatcher dispatcher;
    private CheckoutFlow resolvedProxy;
    private String callResult;

    @Given("a compiled multi-value feature {string} with flag key {string}")
    public void aCompiledMultiValueFeatureWithFlagKey(String featureName, String flagKey) {
        // Fixtures are pre-compiled; metadata will be configured with multi-value mappings
        multiValueMap.clear();
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

    @When("the developer resolves {string} through the multi-value dispatcher")
    public void theDeveloperResolvesThroughTheMultiValueDispatcher(String featureName) {
        CheckoutFlowMetadata.setMultiValueVariants(Map.copyOf(multiValueMap));
        dispatcher = new DefaultFeatureDispatcher(flagProvider);
        resolvedProxy = dispatcher.resolve(CheckoutFlow.class);
        callResult = resolvedProxy.execute();
    }

    @Then("the {string} variant handles the call")
    public void theVariantHandlesTheCall(String expectedVariant) {
        assertThat(callResult).isEqualTo(expectedVariant);
    }

    private Supplier<CheckoutFlow> resolveVariantSupplier(String variantClass) {
        return switch (variantClass) {
            case "ClassicCheckout" -> ClassicCheckout::new;
            case "ModernCheckout" -> ModernCheckout::new;
            default -> throw new IllegalArgumentException("Unknown variant: " + variantClass);
        };
    }

    @And("the generated proxy maps {string} to {string}")
    public void theGeneratedProxyMaps(String value, String variantClass) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        // Check the metadata class which contains the variant suppliers map
        // We need to find which feature this belongs to -- search all generated metadata files
        String metadataQualified = PACKAGE + ".CheckoutFlow_FlagZenMetadata";
        try {
            Optional<JavaFileObject> metadataFile = compilation.generatedSourceFile(metadataQualified);
            org.assertj.core.api.Assertions.assertThat(metadataFile)
                    .as("Metadata file for CheckoutFlow should be generated")
                    .isPresent();
            String source = metadataFile.get().getCharContent(false).toString();
            org.assertj.core.api.Assertions.assertThat(source)
                    .as("Metadata should map value '%s' to %s".formatted(value, variantClass))
                    .contains("\"" + value + "\"")
                    .contains(variantClass + "::new");
        } catch (IOException e) {
            throw new AssertionError("Failed to read generated metadata source", e);
        }
    }
}
