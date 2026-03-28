package com.flagzen.acceptance.steps;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.cucumber.java.en.And;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Optional;

import static com.google.testing.compile.CompilationSubject.assertThat;

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
