package com.flagzen.acceptance.steps;

import com.flagzen.processor.FlagZenProcessor;
import com.google.common.truth.StringSubject;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Step definitions for compile-time validation scenarios.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation result (driven port output).
 */
public class CompileTimeSteps {

    private static final String PACKAGE = "com.example";

    private String featureInterfaceName;
    private String flagKey;
    private String fallbackStrategy;
    private String methodName;
    private boolean hasVariantEnum = true;
    private List<String> variantEnumValues;
    private final List<JavaFileObject> sourceFiles = new ArrayList<>();
    private final List<String> variantNames = new ArrayList<>();
    private final Set<String> featureSourcesAdded = new HashSet<>();
    private final Map<String, String> featureKeyMap = new HashMap<>();
    private Compilation compilation;
    private String generatedProxySource;

    @Given("a feature interface {string} with flag key {string}")
    public void aFeatureInterfaceWithFlagKey(String interfaceName, String key) {
        this.featureInterfaceName = interfaceName;
        this.flagKey = key;
        this.featureKeyMap.put(interfaceName, key);
    }

    @Given("a feature interface {string} with flag key {string} and fallback strategy {word}")
    public void aFeatureInterfaceWithFlagKeyAndFallbackStrategy(String interfaceName, String key, String strategy) {
        this.featureInterfaceName = interfaceName;
        this.flagKey = key;
        this.fallbackStrategy = strategy;
    }

    @And("no inner Variant enum is defined on {string}")
    public void noInnerVariantEnumIsDefinedOn(String interfaceName) {
        this.hasVariantEnum = false;
    }

    @And("a method {string} declared on {string}")
    public void aMethodDeclaredOn(String method, String interfaceName) {
        this.methodName = method;
        featureSourcesAdded.add(interfaceName);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + interfaceName,
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("%s")
                public interface %s {
                    void %s();
                }
                """.formatted(PACKAGE, flagKey, interfaceName, method)
        ));
    }

    @And("a variant {string} implementing {string} for value {string}")
    public void aVariantImplementingForValue(String variantClass, String interfaceName, String value) {
        ensureFeatureSourceExists(interfaceName);
        variantNames.add(variantClass);
        if (methodName != null) {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + variantClass,
                    """
                    package %s;

                    import com.flagzen.Variant;

                    @Variant(value = "%s", of = %s.class)
                    public class %s implements %s {
                        @Override
                        public void %s() {}
                    }
                    """.formatted(PACKAGE, value, interfaceName, variantClass, interfaceName, methodName)
            ));
        } else {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + variantClass,
                    """
                    package %s;

                    import com.flagzen.Variant;

                    @Variant(value = "%s", of = %s.class)
                    public class %s implements %s {
                    }
                    """.formatted(PACKAGE, value, interfaceName, variantClass, interfaceName)
            ));
        }
    }

    @And("a class {string} annotated as variant {string} but not implementing {string}")
    public void aClassAnnotatedAsVariantButNotImplementing(String className, String value, String interfaceName) {
        ensureFeatureSourceExists(interfaceName);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = "%s", of = %s.class)
                public class %s {
                }
                """.formatted(PACKAGE, value, interfaceName, className)
        ));
    }

    @And("the error states the variant class must implement the feature interface")
    public void theErrorStatesTheVariantClassMustImplementTheFeatureInterface() {
        assertThat(compilation).hadErrorContaining("must implement the feature interface");
    }

    @And("a class {string} annotated for {string} with value {string} and for {string} with value {string}")
    public void aClassAnnotatedForTwoFeatures(String className, String feature1, String value1, String feature2, String value2) {
        ensureFeatureSourceExists(feature1);
        ensureFeatureSourceExists(feature2);
        variantNames.add(className);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = "%s", of = %s.class)
                @Variant(value = "%s", of = %s.class)
                public class %s implements %s, %s {
                }
                """.formatted(PACKAGE, value1, feature1, value2, feature2, className, feature1, feature2)
        ));
    }

    @And("{string} implements both {string} and {string}")
    public void implementsBothInterfaces(String className, String interface1, String interface2) {
        // Already handled in the annotation step above - class source already includes implements clause
    }

    @And("{string} is registered for both {string} and {string}")
    public void isRegisteredForBothFeatures(String className, String key1, String key2) {
        String feature1 = featureKeyMap.entrySet().stream()
                .filter(e -> e.getValue().equals(key1))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow();
        String feature2 = featureKeyMap.entrySet().stream()
                .filter(e -> e.getValue().equals(key2))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow();

        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + feature1 + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains(className);
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + feature2 + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains(className);
    }

    @When("the project compiles")
    public void theProjectCompiles() {
        compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(sourceFiles.toArray(new JavaFileObject[0]));
    }

    @Then("compilation succeeds")
    public void compilationSucceeds() {
        assertThat(compilation).succeeded();
    }

    @And("the fallback strategy {word} is recorded for {string}")
    public void theFallbackStrategyIsRecordedFor(String strategy, String key) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + featureInterfaceName + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("FallbackStrategy." + strategy);
    }

    @And("{string} is accepted as a valid variant value")
    public void isAcceptedAsAValidVariantValue(String value) {
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + featureInterfaceName + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("\"" + value + "\"");
    }

    @And("a dispatch proxy {string} is generated")
    public void aDispatchProxyIsGenerated(String proxyName) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + proxyName)
                .isNotNull();
    }

    @And("the proxy implements the {string} interface")
    public void theProxyImplementsTheInterface(String interfaceName) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + interfaceName + "_FlagZenProxy")
                .contentsAsUtf8String()
                .contains("implements " + interfaceName);
    }

    @Given("a class {string} annotated as a feature with key {string}")
    public void aClassAnnotatedAsAFeatureWithKey(String className, String key) {
        featureSourcesAdded.add(className);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("%s")
                public class %s {
                }
                """.formatted(PACKAGE, key, className)
        ));
    }

    @Then("compilation fails")
    public void compilationFails() {
        assertThat(compilation).failed();
    }

    @And("the error message states {string}")
    public void theErrorMessageStates(String expectedMessage) {
        assertThat(compilation).hadErrorContaining(expectedMessage);
    }

    @And("a class {string} annotated as the default variant implementing {string}")
    public void aClassAnnotatedAsTheDefaultVariantImplementing(String className, String interfaceName) {
        ensureFeatureSourceExists(interfaceName);
        variantNames.add(className);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = %s.class)
                public class %s implements %s {
                }
                """.formatted(PACKAGE, interfaceName, className, interfaceName)
        ));
    }

    @And("{string} is registered as the fallback for {string}")
    public void isRegisteredAsTheFallbackFor(String className, String key) {
        String feature = featureKeyMap.entrySet().stream()
                .filter(e -> e.getValue().equals(key))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow();
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + feature + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains(className + "::new");
    }

    @And("a default variant {string} implementing {string}")
    public void aDefaultVariantImplementing(String className, String interfaceName) {
        ensureFeatureSourceExists(interfaceName);
        variantNames.add(className);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = %s.class)
                public class %s implements %s {
                }
                """.formatted(PACKAGE, interfaceName, className, interfaceName)
        ));
    }

    @And("an inner Variant enum with values {string}, {string}, {string}")
    public void anInnerVariantEnumWithValues(String val1, String val2, String val3) {
        this.variantEnumValues = List.of(val1, val2, val3);
    }

    @And("the error states {string} is not a valid value for {string}")
    public void theErrorStatesValueIsNotValidForFeature(String value, String featureName) {
        assertThat(compilation).hadErrorContaining(
                "@Variant(\"" + value + "\") does not match any value in " + featureName + ".Variant"
        );
    }

    @And("the error lists valid values: {string}, {string}, {string}")
    public void theErrorListsValidValues(String val1, String val2, String val3) {
        assertThat(compilation).hadErrorContaining("Valid values: " + val1 + ", " + val2 + ", " + val3);
    }

    @And("the error lists {string} as missing an implementation")
    public void theErrorListsValueAsMissingAnImplementation(String missingValue) {
        assertThat(compilation).hadErrorContaining(missingValue);
        assertThat(compilation).hadErrorContaining("has no implementation");
    }

    @And("the error identifies both {string} and {string} as conflicting")
    public void theErrorIdentifiesBothAsConflicting(String class1, String class2) {
        assertThat(compilation).hadErrorContaining(class1);
        assertThat(compilation).hadErrorContaining(class2);
        assertThat(compilation).hadErrorContaining("Duplicate");
    }

    @Given("a compiled feature {string} with flag key {string}")
    public void aCompiledFeatureWithFlagKey(String interfaceName, String key) {
        this.featureInterfaceName = interfaceName;
        this.flagKey = key;
        this.featureKeyMap.put(interfaceName, key);
        ensureFeatureSourceExists(interfaceName);
        // Add a minimal variant so the processor generates a proxy
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + ".Default" + interfaceName,
                """
                package %s;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = %s.class)
                public class Default%s implements %s {
                }
                """.formatted(PACKAGE, interfaceName, interfaceName, interfaceName)
        ));
        compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(sourceFiles.toArray(new JavaFileObject[0]));
        assertThat(compilation).succeeded();
    }

    @And("the dispatch proxy {string} has been generated")
    public void theDispatchProxyHasBeenGenerated(String proxyName) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + proxyName)
                .isNotNull();
    }

    @When("the developer inspects the proxy's string representation")
    public void theDeveloperInspectsTheProxyStringRepresentation() throws Exception {
        extractGeneratedProxySource();
    }

    @When("the developer inspects the generated source code")
    public void theDeveloperInspectsTheGeneratedSourceCode() throws Exception {
        extractGeneratedProxySource();
    }


    @Then("it shows {string}")
    public void itShows(String expected) {
        org.assertj.core.api.Assertions.assertThat(generatedProxySource)
                .contains("return \"" + expected + "\"");
    }

    @Then("it contains no reflection imports")
    public void itContainsNoReflectionImports() {
        org.assertj.core.api.Assertions.assertThat(generatedProxySource)
                .doesNotContain("java.lang.reflect");
    }

    @And("dispatch uses direct method calls or map lookups")
    public void dispatchUsesDirectMethodCallsOrMapLookups() {
        org.assertj.core.api.Assertions.assertThat(generatedProxySource)
                .satisfiesAnyOf(
                        source -> org.assertj.core.api.Assertions.assertThat(source).contains("variants.get("),
                        source -> org.assertj.core.api.Assertions.assertThat(source).contains("resolveVariant()")
                );
    }

    private void ensureFeatureSourceExists(String interfaceName) {
        if (featureSourcesAdded.contains(interfaceName)) {
            return;
        }
        featureSourcesAdded.add(interfaceName);
        String key = featureKeyMap.getOrDefault(interfaceName, flagKey);
        String enumBlock = buildVariantEnumBlock();
        if (fallbackStrategy != null) {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + interfaceName,
                    """
                    package %s;

                    import com.flagzen.Feature;
                    import com.flagzen.FallbackStrategy;

                    @Feature(value = "%s", fallback = FallbackStrategy.%s)
                    public interface %s {
                    %s}
                    """.formatted(PACKAGE, key, fallbackStrategy, interfaceName, enumBlock)
            ));
        } else {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + interfaceName,
                    """
                    package %s;

                    import com.flagzen.Feature;

                    @Feature("%s")
                    public interface %s {
                    %s}
                    """.formatted(PACKAGE, key, interfaceName, enumBlock)
            ));
        }
    }

    private void extractGeneratedProxySource() throws Exception {
        var proxyFile = compilation.generatedSourceFile(
                PACKAGE + "." + featureInterfaceName + "_FlagZenProxy");
        org.assertj.core.api.Assertions.assertThat(proxyFile).isPresent();
        generatedProxySource = proxyFile.get().getCharContent(false).toString();
    }

    // --- US-04 @property: one proxy per feature ---

    private final List<String> propertyFeatureNames = new ArrayList<>();

    @Given("any valid feature interface with at least one variant")
    public void anyValidFeatureInterfaceWithAtLeastOneVariant() {
        // Compile two distinct features, each with at least one variant
        propertyFeatureNames.clear();
        propertyFeatureNames.add("CheckoutFlow");
        propertyFeatureNames.add("DarkMode");

        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + ".CheckoutFlow",
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public interface CheckoutFlow {
                }
                """.formatted(PACKAGE)
        ));
        featureSourcesAdded.add("CheckoutFlow");
        featureKeyMap.put("CheckoutFlow", "checkout-flow");

        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + ".ClassicCheckout",
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class ClassicCheckout implements CheckoutFlow {
                }
                """.formatted(PACKAGE)
        ));

        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + ".DarkMode",
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("dark-mode")
                public interface DarkMode {
                }
                """.formatted(PACKAGE)
        ));
        featureSourcesAdded.add("DarkMode");
        featureKeyMap.put("DarkMode", "dark-mode");

        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + ".DarkModeOn",
                """
                package %s;

                import com.flagzen.Variant;

                @Variant(value = "ON", of = DarkMode.class)
                public class DarkModeOn implements DarkMode {
                }
                """.formatted(PACKAGE)
        ));
    }

    @Then("exactly one proxy class is generated per feature interface")
    public void exactlyOneProxyClassIsGeneratedPerFeatureInterface() {
        assertThat(compilation).succeeded();

        for (String feature : propertyFeatureNames) {
            // Exactly one proxy exists for each feature
            Optional<JavaFileObject> proxy = compilation.generatedSourceFile(
                    PACKAGE + "." + feature + "_FlagZenProxy");
            org.assertj.core.api.Assertions.assertThat(proxy)
                    .as("Proxy for " + feature)
                    .isPresent();

            // No second proxy (e.g., _FlagZenProxy2)
            Optional<JavaFileObject> secondProxy = compilation.generatedSourceFile(
                    PACKAGE + "." + feature + "_FlagZenProxy2");
            org.assertj.core.api.Assertions.assertThat(secondProxy)
                    .as("No duplicate proxy for " + feature)
                    .isEmpty();
        }
    }

    @And("each proxy implements its corresponding feature interface")
    public void eachProxyImplementsItsCorrespondingFeatureInterface() throws Exception {
        for (String feature : propertyFeatureNames) {
            Optional<JavaFileObject> proxy = compilation.generatedSourceFile(
                    PACKAGE + "." + feature + "_FlagZenProxy");
            org.assertj.core.api.Assertions.assertThat(proxy).isPresent();
            String source = proxy.get().getCharContent(false).toString();
            org.assertj.core.api.Assertions.assertThat(source)
                    .as("Proxy for %s implements %s".formatted(feature, feature))
                    .contains("implements " + feature);
        }
    }

    private String buildVariantEnumBlock() {
        if (variantEnumValues == null || variantEnumValues.isEmpty()) {
            return "";
        }
        return "    enum Variant { " + String.join(", ", variantEnumValues) + " }\n";
    }
}
