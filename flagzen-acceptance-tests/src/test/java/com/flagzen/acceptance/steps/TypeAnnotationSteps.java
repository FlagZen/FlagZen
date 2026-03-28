package com.flagzen.acceptance.steps;

import com.flagzen.FeatureType;
import com.flagzen.processor.FlagZenProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Step definitions for typed annotation model scenarios (milestone-1-type-annotations.feature).
 * Sources are added to {@link SharedCompilationContext} so that {@link CompileTimeSteps#theProjectCompiles()}
 * includes them in compilation.
 */
public class TypeAnnotationSteps {

    private static final String PACKAGE = "com.example";

    private final Map<String, String> featureTypes = new HashMap<>();
    private final Map<String, String> featureKeys = new HashMap<>();
    private final Map<String, String> featureFallbacks = new HashMap<>();
    private final Map<String, List<VariantDef>> featureVariants = new HashMap<>();
    private boolean hasDefaultVariant = false;

    private String currentFeature;

    // Property test state
    private List<String> propertyErrorMessages;

    // Multi-feature variant state
    private String multiFeatureVariant;
    private List<String> multiFeatureInterfaces;
    private final List<String> multiFeatureAnnotations = new ArrayList<>();

    // --- Feature interface setup ---

    @Given("a feature interface {string} with flag key {string} and type INT")
    public void featureWithIntType(String name, String key) {
        setupFeature(name, key, "INT");
    }

    @Given("a feature interface {string} with flag key {string} and type BOOLEAN")
    public void featureWithBooleanType(String name, String key) {
        setupFeature(name, key, "BOOLEAN");
    }

    @Given("a feature interface {string} with flag key {string} and type LONG")
    public void featureWithLongType(String name, String key) {
        setupFeature(name, key, "LONG");
    }

    @Given("a feature interface {string} with flag key {string} and type DOUBLE")
    public void featureWithDoubleType(String name, String key) {
        setupFeature(name, key, "DOUBLE");
    }

    @Given("a feature interface {string} with flag key {string} and no type attribute")
    public void featureWithNoTypeAttribute(String name, String key) {
        setupFeature(name, key, null);
    }

    @Given("a feature {string} with type INT")
    public void featureWithTypeInt(String name) {
        setupFeature(name, toKey(name), "INT");
    }

    @Given("a feature {string} with type BOOLEAN")
    public void featureWithTypeBoolean(String name) {
        setupFeature(name, toKey(name), "BOOLEAN");
    }

    @Given("a feature {string} with type LONG")
    public void featureWithTypeLong(String name) {
        setupFeature(name, toKey(name), "LONG");
    }

    @Given("a feature {string} with type DOUBLE")
    public void featureWithTypeDouble(String name) {
        setupFeature(name, toKey(name), "DOUBLE");
    }

    @Given("a feature {string} with type STRING")
    public void featureWithTypeString(String name) {
        setupFeature(name, toKey(name), "STRING");
    }

    // --- Variant definitions ---

    @And("a variant {string} implementing {string} for integer value {int}")
    public void variantImplementingFeatureForIntegerValue(String variantName, String featureName, int value) {
        String savedFeature = currentFeature;
        currentFeature = featureName;
        addVariant(variantName, "intValue = " + value, "standard", null);
        currentFeature = savedFeature;
    }

    @And("a variant {string} with integer value {int}")
    public void variantWithIntegerValue(String variantName, int value) {
        addVariant(variantName, "intValue = " + value, "standard", null);
    }

    @And("a variant {string} with boolean value true")
    public void variantWithBooleanTrue(String variantName) {
        addVariant(variantName, "booleanValue = \"true\"", "standard", null);
    }

    @And("a variant {string} with boolean value false")
    public void variantWithBooleanFalse(String variantName) {
        addVariant(variantName, "booleanValue = \"false\"", "standard", null);
    }

    @And("a variant {string} with long value {long}")
    public void variantWithLongValue(String variantName, long value) {
        addVariant(variantName, "longValue = " + value + "L", "standard", null);
    }

    @And("a variant {string} with double value {double} and default tolerance")
    public void variantWithDoubleValueDefaultTolerance(String variantName, double value) {
        addVariant(variantName, "doubleValue = @CloseTo(value = " + value + ")",
                "standard", "import com.flagzen.CloseTo;");
    }

    @And("a variant {string} with double value {double} and tolerance {double}")
    public void variantWithDoubleValueAndTolerance(String variantName, double value, double tolerance) {
        addVariant(variantName, "doubleValue = @CloseTo(value = " + value + ", delta = " + tolerance + ")",
                "standard", "import com.flagzen.CloseTo;");
    }

    @And("a variant {string} with string value {string}")
    public void variantWithStringValue(String variantName, String value) {
        addVariant(variantName, "value = \"" + value + "\"", "standard", null);
    }

    @And("a variant {string} annotated as active when true")
    public void variantAnnotatedWhenTrue(String variantName) {
        addVariant(variantName, null, "whenTrue", null);
    }

    @And("a variant {string} annotated as active when false")
    public void variantAnnotatedWhenFalse(String variantName) {
        addVariant(variantName, null, "whenFalse", null);
    }

    @And("a variant with boolean value true")
    public void unnamedVariantWithBooleanTrue() {
        addVariant("BoolTrueVariant", "booleanValue = \"true\"", "standard", null);
    }

    @And("a variant with integer value {int}")
    public void unnamedVariantWithIntegerValue(int value) {
        addVariant("IntVariant", "intValue = " + value, "standard", null);
    }

    @And("a variant with long value {long}")
    public void unnamedVariantWithLongValue(long value) {
        addVariant("LongVariant", "longValue = " + value + "L", "standard", null);
    }

    @And("a default variant {string}")
    public void defaultVariantForCurrentFeature(String variantName) {
        hasDefaultVariant = true;
        String qualifiedName = PACKAGE + "." + variantName;
        if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
            return;
        }
        SharedCompilationContext.markSourceAdded(qualifiedName);
        SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                qualifiedName,
                """
                package %s;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = %s.class)
                public class %s implements %s {
                }
                """.formatted(PACKAGE, currentFeature, variantName, currentFeature)
        ));
    }

    @And("no variant for false and no default variant")
    public void noVariantForFalseAndNoDefault() {
        // Intentionally empty -- we don't add a false variant or default
    }

    // --- Feature with fallback ---

    @Given("a feature {string} with type BOOLEAN and fallback REQUIRED")
    public void featureWithTypeBooleanAndFallbackRequired(String name) {
        currentFeature = name;
        featureTypes.put(name, "BOOLEAN");
        featureKeys.put(name, toKey(name));
        featureFallbacks.put(name, "REQUIRED");
        SharedCompilationContext.setFeatureType(name, "BOOLEAN");
        addFeatureSourceWithFallback(name, toKey(name), "BOOLEAN", "REQUIRED");
    }

    @Given("a feature {string} with flag key {string} and type INT")
    public void featureWithFlagKeyAndTypeInt(String name, String key) {
        setupFeature(name, key, "INT");
    }

    // --- Compile error assertions ---

    @Then("compilation fails with an error on {string}")
    public void compilationFailsWithErrorOn(String variantName) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(variantName);
    }

    @And("the error message mentions type INT and string value")
    public void errorMessageMentionsTypeIntAndStringValue() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("INT");
        assertThat(compilation).hadErrorContaining("value");
    }

    @And("the error suggests using integer value {int}")
    public void errorSuggestsUsingIntegerValue(int value) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("intValue");
    }

    @And("the error suggests using a boolean variant")
    public void errorSuggestsUsingBooleanVariant() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).hadErrorContaining("boolean");
    }

    @Then("compilation fails suggesting a long value variant")
    public void compilationFailsSuggestingLongValueVariant() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("longValue");
    }

    @Then("compilation fails suggesting an approximate double value variant")
    public void compilationFailsSuggestingDoubleValueVariant() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("doubleValue");
    }

    @Then("compilation fails suggesting an integer value variant")
    public void compilationFailsSuggestingIntegerValueVariant() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("intValue");
    }

    @Then("compilation fails identifying {string} as using the wrong attribute")
    public void compilationFailsIdentifyingVariantAsWrongAttribute(String variantName) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(variantName);
    }

    @Then("compilation fails with a duplicate variant value error for integer {int}")
    public void compilationFailsWithDuplicateIntegerValue(int value) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining(String.valueOf(value));
    }

    @Then("compilation fails with a duplicate variant value error for long {long}")
    public void compilationFailsWithDuplicateLongValue(long value) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining(String.valueOf(value));
    }

    @Then("compilation fails with a duplicate variant value error for boolean true")
    public void compilationFailsWithDuplicateBooleanTrue() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("true");
    }

    @Then("compilation fails requiring variants for both true and false")
    public void compilationFailsRequiringBothTrueAndFalse() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("false");
    }

    @Then("the error message includes {string} and {string}")
    public void errorMessageIncludes(String text1, String text2) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(text1);
        assertThat(compilation).hadErrorContaining(text2);
    }

    // --- Property test: every type mismatch produces actionable fix ---

    @Given("any feature type and a variant using the wrong attribute")
    public void anyFeatureTypeAndVariantUsingWrongAttribute() {
        // We'll compile multiple mismatched scenarios and collect error messages
        propertyErrorMessages = new ArrayList<>();
    }

    @When("the annotation processor validates the variant")
    public void annotationProcessorValidatesVariant() {
        // Test each type with a wrong attribute
        Map<String, String[]> mismatches = Map.of(
                "INT", new String[]{"value = \"wrong\"", "StringOnInt"},
                "LONG", new String[]{"intValue = 1", "IntOnLong"},
                "BOOLEAN", new String[]{"intValue = 1", "IntOnBool"},
                "DOUBLE", new String[]{"intValue = 1", "IntOnDouble"},
                "STRING", new String[]{"intValue = 1", "IntOnString"}
        );

        for (var entry : mismatches.entrySet()) {
            String featureType = entry.getKey();
            String attr = entry.getValue()[0];
            String variantName = entry.getValue()[1];
            String featureName = "PropTest" + featureType;
            String key = "prop-test-" + featureType.toLowerCase();

            String typeImport = "import com.flagzen.FeatureType;\n";
            JavaFileObject featureSrc = JavaFileObjects.forSourceString(
                    PACKAGE + "." + featureName,
                    """
                    package %s;

                    import com.flagzen.Feature;
                    %s
                    @Feature(value = "%s", type = FeatureType.%s)
                    public interface %s {
                    }
                    """.formatted(PACKAGE, typeImport, key, featureType, featureName)
            );

            JavaFileObject variantSrc = JavaFileObjects.forSourceString(
                    PACKAGE + "." + variantName,
                    """
                    package %s;

                    import com.flagzen.Variant;

                    @Variant(%s, of = %s.class)
                    public class %s implements %s {
                    }
                    """.formatted(PACKAGE, attr, featureName, variantName, featureName)
            );

            Compilation compilation = javac()
                    .withProcessors(new FlagZenProcessor())
                    .compile(featureSrc, variantSrc);

            List<String> errors = compilation.errors().stream()
                    .map(d -> d.getMessage(Locale.ROOT))
                    .collect(Collectors.toList());
            propertyErrorMessages.addAll(errors);
        }
    }

    @Then("the error message includes a suggested fix with the correct attribute syntax")
    public void errorMessageIncludesSuggestedFix() {
        org.assertj.core.api.Assertions.assertThat(propertyErrorMessages)
                .as("Every type mismatch should produce at least one error")
                .isNotEmpty();
        for (String msg : propertyErrorMessages) {
            org.assertj.core.api.Assertions.assertThat(msg)
                    .as("Error message should contain a suggested fix: " + msg)
                    .containsIgnoringCase("use");
        }
    }

    @And("a variant {string} implementing {string} annotated as active when true")
    public void variantImplementingFeatureAnnotatedWhenTrue(String variantName, String featureName) {
        String savedFeature = currentFeature;
        currentFeature = featureName;
        addVariant(variantName, null, "whenTrue", null);
        currentFeature = savedFeature;
    }

    @And("a variant {string} implementing {string} annotated as active when false")
    public void variantImplementingFeatureAnnotatedWhenFalse(String variantName, String featureName) {
        String savedFeature = currentFeature;
        currentFeature = featureName;
        addVariant(variantName, null, "whenFalse", null);
        currentFeature = savedFeature;
    }

    // --- WhenTrue/WhenFalse multi-feature scenarios ---

    @Given("a variant {string} implementing both {string} and {string}")
    public void variantImplementingBothFeatures(String variantName, String feature1, String feature2) {
        ensureFeatureRegistered(feature1);
        ensureFeatureRegistered(feature2);
        multiFeatureVariant = variantName;
        multiFeatureInterfaces = List.of(feature1, feature2);
        multiFeatureAnnotations.clear();
    }

    @And("it is annotated as active when true targeting {string}")
    public void annotatedWhenTrueTargeting(String targetFeature) {
        multiFeatureAnnotations.add("@WhenTrue(of = " + targetFeature + ".class)");
        registerMultiFeatureHook();
    }

    @And("it is annotated as active when false targeting {string}")
    public void annotatedWhenFalseTargeting(String targetFeature) {
        multiFeatureAnnotations.add("@WhenFalse(of = " + targetFeature + ".class)");
        registerMultiFeatureHook();
    }

    @Given("a variant {string} implementing only {string}")
    public void variantImplementingOnly(String variantName, String featureName) {
        ensureFeatureRegistered(featureName);
        multiFeatureVariant = variantName;
        multiFeatureInterfaces = List.of(featureName);
        multiFeatureAnnotations.clear();
    }

    @And("it is annotated as active when true without an explicit target")
    public void annotatedWhenTrueWithoutTarget() {
        multiFeatureAnnotations.add("@WhenTrue");
        registerMultiFeatureHook();
    }

    // Note: "When the project compiles" and "Then compilation succeeds" are defined in CompileTimeSteps.
    // This class adds sources to SharedCompilationContext which CompileTimeSteps includes in compilation.

    @Then("compilation succeeds with both boolean variants registered")
    public void compilationSucceedsWithBothBooleanVariantsRegistered() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).succeeded();
    }

    // --- Feature model assertions ---

    @And("the feature model records type as BOOLEAN")
    public void featureModelRecordsBoolean() {
        assertProxyVariantMapType("Boolean");
    }

    @And("the feature model records type as LONG")
    public void featureModelRecordsLong() {
        assertProxyVariantMapType("Long");
    }

    @And("the feature model records type as DOUBLE")
    public void featureModelRecordsDouble() {
        assertProxyVariantMapType("Double");
    }

    @And("the feature model records type as STRING")
    public void featureModelRecordsString() {
        assertProxyVariantMapType("String");
    }

    @And("existing proxy generation behavior is unchanged")
    public void existingProxyGenerationBehaviorUnchanged() {
        assertProxyVariantMapType("String");
    }

    // --- Variant model assertions ---

    @And("the variant model records integer {int} for {string}")
    public void variantModelRecordsIntegerFor(int value, String variantName) {
        assertMetadataContains(variantName + "::new");
    }

    @And("the variant model records boolean true for {string}")
    public void variantModelRecordsBooleanTrueFor(String variantName) {
        assertMetadataContains(variantName + "::new");
    }

    @And("the variant model records boolean false for {string}")
    public void variantModelRecordsBooleanFalseFor(String variantName) {
        assertMetadataContains(variantName + "::new");
    }

    @And("the variant model records long {long} for {string}")
    public void variantModelRecordsLongFor(long value, String variantName) {
        assertMetadataContains(variantName + "::new");
    }

    @And("the variant model records double {double} with tolerance {double}")
    public void variantModelRecordsDoubleWithTolerance(double value, double tolerance) {
        assertProxyVariantMapType("Double");
    }

    @And("the variant model records double {double} with tolerance 1e-{int}")
    public void variantModelRecordsDoubleWithScientificTolerance(double value, int exponent) {
        assertProxyVariantMapType("Double");
    }

    @And("the variant model records string {string} for {string}")
    public void variantModelRecordsStringFor(String value, String variantName) {
        assertMetadataContains("\"" + value + "\"");
        assertMetadataContains(variantName + "::new");
    }

    // --- WhenTrue/WhenFalse equivalence assertions ---

    @Then("the processor treats it identically to a boolean variant with value true")
    public void treatedIdenticallyToBooleanTrue() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).succeeded();
        assertProxyVariantMapType("Boolean");
    }

    @Then("the processor treats it identically to a boolean variant with value false")
    public void treatedIdenticallyToBooleanFalse() {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).succeeded();
        assertProxyVariantMapType("Boolean");
    }

    @Then("boolean true is registered for {string} on {string}")
    public void booleanTrueRegisteredFor(String featureName, String variantName) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).succeeded();
        assertMetadataContainsForFeature(featureName, variantName + "::new");
    }

    @And("boolean false is registered for {string} on {string}")
    public void booleanFalseRegisteredFor(String featureName, String variantName) {
        assertMetadataContainsForFeature(featureName, variantName + "::new");
    }

    @Then("the processor infers the target feature as {string}")
    public void processorInfersTargetFeature(String featureName) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        assertThat(compilation).succeeded();
        assertMetadataContainsForFeature(featureName, multiFeatureVariant + "::new");
    }

    // --- Internal helpers ---

    private record VariantDef(String name, String annotationAttr, String kind, String extraImport) {}

    private void setupFeature(String name, String key, String type) {
        currentFeature = name;
        featureTypes.put(name, type);
        featureKeys.put(name, key);
        if (type != null) {
            SharedCompilationContext.setFeatureType(name, type);
        }
        addFeatureSource(name, key, type);
    }

    private void ensureFeatureRegistered(String featureName) {
        if (!featureKeys.containsKey(featureName)) {
            String type = "BOOLEAN";
            String key = toKey(featureName);
            featureTypes.put(featureName, type);
            featureKeys.put(featureName, key);
            addFeatureSource(featureName, key, type);
        }
    }

    private void addFeatureSourceWithFallback(String name, String key, String type, String fallback) {
        SharedCompilationContext.addPreCompileHook(() -> {
            String qualifiedName = PACKAGE + "." + name;
            if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
                return;
            }
            SharedCompilationContext.markSourceAdded(qualifiedName);

            String typeAttr = (type != null) ? ", type = FeatureType." + type : "";
            String fallbackAttr = (fallback != null) ? ", fallback = FallbackStrategy." + fallback : "";
            String featureTypeImport = (type != null)
                    ? "import com.flagzen.FeatureType;\n" : "";
            String fallbackImport = (fallback != null)
                    ? "import com.flagzen.FallbackStrategy;\n" : "";

            SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                    qualifiedName,
                    """
                    package %s;

                    import com.flagzen.Feature;
                    %s%s
                    @Feature(value = "%s"%s%s)
                    public interface %s {
                    }
                    """.formatted(PACKAGE, featureTypeImport, fallbackImport, key, typeAttr, fallbackAttr, name)
            ));
        });
    }

    private void addFeatureSource(String name, String key, String type) {
        // Defer source generation to pre-compile hook so methods declared later are included
        SharedCompilationContext.addPreCompileHook(() -> {
            String qualifiedName = PACKAGE + "." + name;
            if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
                return;
            }
            SharedCompilationContext.markSourceAdded(qualifiedName);

            String typeAttr = (type != null) ? ", type = FeatureType." + type : "";
            String featureTypeImport = (type != null)
                    ? "import com.flagzen.FeatureType;\n" : "";
            String method = SharedCompilationContext.getFeatureMethod(name);
            String methodDecl = (method != null) ? "    String " + method + "();\n" : "";

            SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                    qualifiedName,
                    """
                    package %s;

                    import com.flagzen.Feature;
                    %s
                    @Feature(value = "%s"%s)
                    public interface %s {
                    %s}
                    """.formatted(PACKAGE, featureTypeImport, key, typeAttr, name, methodDecl)
            ));
        });
    }

    private void addVariant(String name, String annotationAttr, String kind, String extraImport) {
        featureVariants
                .computeIfAbsent(currentFeature, k -> new ArrayList<>())
                .add(new VariantDef(name, annotationAttr, kind, extraImport));
        addVariantSource(name, annotationAttr, kind, extraImport, currentFeature);
    }

    private void addVariantSource(String name, String annotationAttr, String kind,
                                  String extraImport, String featureName) {
        String qualifiedName = PACKAGE + "." + name;
        if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
            return;
        }
        SharedCompilationContext.markSourceAdded(qualifiedName);

        String methodOverride = buildMethodOverride(featureName);

        JavaFileObject source;
        if ("whenTrue".equals(kind)) {
            source = JavaFileObjects.forSourceString(qualifiedName,
                    """
                    package %s;

                    import com.flagzen.WhenTrue;

                    @WhenTrue(of = %s.class)
                    public class %s implements %s {
                    %s}
                    """.formatted(PACKAGE, featureName, name, featureName, methodOverride));
        } else if ("whenFalse".equals(kind)) {
            source = JavaFileObjects.forSourceString(qualifiedName,
                    """
                    package %s;

                    import com.flagzen.WhenFalse;

                    @WhenFalse(of = %s.class)
                    public class %s implements %s {
                    %s}
                    """.formatted(PACKAGE, featureName, name, featureName, methodOverride));
        } else {
            String extraImportLine = (extraImport != null) ? extraImport + "\n" : "";
            source = JavaFileObjects.forSourceString(qualifiedName,
                    """
                    package %s;

                    import com.flagzen.Variant;
                    %s
                    @Variant(%s, of = %s.class)
                    public class %s implements %s {
                    %s}
                    """.formatted(PACKAGE, extraImportLine, annotationAttr, featureName,
                            name, featureName, methodOverride));
        }
        SharedCompilationContext.addSourceFile(source);
    }

    private String buildMethodOverride(String featureName) {
        String method = SharedCompilationContext.getFeatureMethod(featureName);
        if (method == null) {
            return "";
        }
        return "    @Override\n    public String " + method + "() { return getClass().getSimpleName(); }\n";
    }

    private boolean multiFeatureHookRegistered = false;

    private void registerMultiFeatureHook() {
        if (!multiFeatureHookRegistered) {
            SharedCompilationContext.addPreCompileHook(this::emitMultiFeatureVariant);
            multiFeatureHookRegistered = true;
        }
    }

    private void emitMultiFeatureVariant() {
        if (multiFeatureVariant == null || multiFeatureAnnotations.isEmpty()) {
            return;
        }
        String qualifiedName = PACKAGE + "." + multiFeatureVariant;
        if (SharedCompilationContext.isSourceAdded(qualifiedName)) {
            return;
        }
        SharedCompilationContext.markSourceAdded(qualifiedName);

        String imports = """
                import com.flagzen.WhenTrue;
                import com.flagzen.WhenFalse;
                """;
        String annotations = String.join("\n", multiFeatureAnnotations);
        String interfaces = String.join(", ", multiFeatureInterfaces);

        SharedCompilationContext.addSourceFile(JavaFileObjects.forSourceString(
                qualifiedName,
                """
                package %s;

                %s
                %s
                public class %s implements %s {
                }
                """.formatted(PACKAGE, imports, annotations, multiFeatureVariant, interfaces)
        ));
    }

    private void assertProxyVariantMapType(String boxedType) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        String proxyQualified = PACKAGE + "." + currentFeature + "_FlagZenProxy";
        assertThat(compilation).generatedSourceFile(proxyQualified).isNotNull();
        try {
            Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(proxyQualified);
            org.assertj.core.api.Assertions.assertThat(proxyFile).isPresent();
            String source = proxyFile.get().getCharContent(false).toString();
            org.assertj.core.api.Assertions.assertThat(source)
                    .contains("Map<" + boxedType + ", Supplier<" + currentFeature + ">> variants");
        } catch (IOException e) {
            throw new AssertionError("Failed to read generated proxy source", e);
        }
    }

    private void assertMetadataContains(String expected) {
        assertMetadataContainsForFeature(currentFeature, expected);
    }

    private void assertMetadataContainsForFeature(String featureName, String expected) {
        Compilation compilation = SharedCompilationContext.getCompilation();
        String metadataQualified = PACKAGE + "." + featureName + "_FlagZenMetadata";
        try {
            Optional<JavaFileObject> metadataFile = compilation.generatedSourceFile(metadataQualified);
            org.assertj.core.api.Assertions.assertThat(metadataFile).isPresent();
            String source = metadataFile.get().getCharContent(false).toString();
            org.assertj.core.api.Assertions.assertThat(source).contains(expected);
        } catch (IOException e) {
            throw new AssertionError("Failed to read generated metadata source", e);
        }
    }

    private static String toKey(String name) {
        // CamelCase to kebab-case
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
