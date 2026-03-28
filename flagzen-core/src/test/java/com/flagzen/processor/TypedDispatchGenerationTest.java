package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for typed dispatch code generation (LONG, DOUBLE, INT, BOOLEAN resolve methods)
 * and typed variant validation edge cases in FlagZenProcessor.
 */
class TypedDispatchGenerationTest {

    // --- LONG dispatch proxy generation ---

    @Test
    void compilesLongTypedFeatureAndGeneratesProxyWithLongVariantMap() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.RateLimit",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "rate-limit", type = FeatureType.LONG)
                public interface RateLimit { long getLimit(); }
                """);

        JavaFileObject lowLimit = JavaFileObjects.forSourceString(
                "com.example.LowLimit",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(longValue = 1000L, of = RateLimit.class)
                public class LowLimit implements RateLimit {
                    @Override public long getLimit() { return 1000L; }
                }
                """);

        JavaFileObject highLimit = JavaFileObjects.forSourceString(
                "com.example.HighLimit",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(longValue = 50000L, of = RateLimit.class)
                public class HighLimit implements RateLimit {
                    @Override public long getLimit() { return 50000L; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, lowLimit, highLimit);

        assertThat(compilation).succeeded();
        Optional<JavaFileObject> proxy = compilation.generatedSourceFile("com.example.RateLimit_FlagZenProxy");
        assertThat(proxy).isPresent();
        String source = proxy.get().getCharContent(false).toString();
        assertThat(source).contains("Map<Long, Supplier<RateLimit>>");
        assertThat(source).contains("getLong");
        // Verify NOOP conditional branch: EXCEPTION is default, so no "return null"
        assertThat(source).doesNotContain("return null");
    }

    // --- DOUBLE dispatch proxy generation ---

    @Test
    void compilesDoubleTypedFeatureAndGeneratesProxyWithDoubleVariantMap() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.SampleRate",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "sample-rate", type = FeatureType.DOUBLE)
                public interface SampleRate { double getRate(); }
                """);

        JavaFileObject lowRate = JavaFileObjects.forSourceString(
                "com.example.LowRate",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.1), of = SampleRate.class)
                public class LowRate implements SampleRate {
                    @Override public double getRate() { return 0.1; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, lowRate);

        assertThat(compilation).succeeded();
        Optional<JavaFileObject> proxy = compilation.generatedSourceFile("com.example.SampleRate_FlagZenProxy");
        assertThat(proxy).isPresent();
        String source = proxy.get().getCharContent(false).toString();
        assertThat(source).contains("Map<Double, Supplier<SampleRate>>");
        assertThat(source).contains("getDouble");
    }

    // --- NOOP fallback generates null return for typed proxies ---

    @ParameterizedTest(name = "NOOP fallback generates null return for {0} typed proxy")
    @MethodSource("noopTypedFeatures")
    void noopFallbackGeneratesNullReturnForTypedProxy(String typeName, String featureType,
                                                       String variantAttr, String extraImport)
            throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example." + typeName + "Feature",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                import com.flagzen.FallbackStrategy;
                @Feature(value = "noop-test", type = FeatureType.%s, fallback = FallbackStrategy.NOOP)
                public interface %sFeature { }
                """.formatted(featureType, typeName));

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example." + typeName + "Variant",
                """
                package com.example;
                import com.flagzen.Variant;
                %s
                @Variant(%s, of = %sFeature.class)
                public class %sVariant implements %sFeature { }
                """.formatted(extraImport, variantAttr, typeName, typeName, typeName));

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).succeeded();
        Optional<JavaFileObject> proxy = compilation.generatedSourceFile(
                "com.example." + typeName + "Feature_FlagZenProxy");
        assertThat(proxy).isPresent();
        String source = proxy.get().getCharContent(false).toString();
        assertThat(source).contains("return null");
    }

    static Stream<Arguments> noopTypedFeatures() {
        return Stream.of(
                Arguments.of("Int", "INT", "intValue = 1", ""),
                Arguments.of("Long", "LONG", "longValue = 1L", ""),
                Arguments.of("Double", "DOUBLE", "doubleValue = @CloseTo(value = 0.1)",
                        "import com.flagzen.CloseTo;"),
                Arguments.of("Bool", "BOOLEAN", "booleanValue = \"true\"", "")
        );
    }

    // --- Boolean REQUIRED missing true variant ---

    @Test
    void rejectsBooleanRequiredFeatureMissingTrueVariant() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Toggle",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                import com.flagzen.FallbackStrategy;
                @Feature(value = "toggle", type = FeatureType.BOOLEAN, fallback = FallbackStrategy.REQUIRED)
                public interface Toggle { }
                """);

        JavaFileObject falseVariant = JavaFileObjects.forSourceString(
                "com.example.ToggleOff",
                """
                package com.example;
                import com.flagzen.WhenFalse;
                @WhenFalse(of = Toggle.class)
                public class ToggleOff implements Toggle { }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, falseVariant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("true");
    }

    // --- Boolean REQUIRED with both true and false compiles ---

    @Test
    void booleanRequiredWithBothVariantsCompiles() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Feature1",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                import com.flagzen.FallbackStrategy;
                @Feature(value = "f1", type = FeatureType.BOOLEAN, fallback = FallbackStrategy.REQUIRED)
                public interface Feature1 { }
                """);

        JavaFileObject trueV = JavaFileObjects.forSourceString(
                "com.example.F1True",
                """
                package com.example;
                import com.flagzen.WhenTrue;
                @WhenTrue(of = Feature1.class)
                public class F1True implements Feature1 { }
                """);

        JavaFileObject falseV = JavaFileObjects.forSourceString(
                "com.example.F1False",
                """
                package com.example;
                import com.flagzen.WhenFalse;
                @WhenFalse(of = Feature1.class)
                public class F1False implements Feature1 { }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, trueV, falseV);

        assertThat(compilation).succeeded();
    }

    // --- Duplicate typed variant values ---

    @Test
    void rejectsDuplicateIntegerVariantValues() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Retries",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "retries", type = FeatureType.INT)
                public interface Retries { }
                """);

        JavaFileObject v1 = JavaFileObjects.forSourceString(
                "com.example.RetryA",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(intValue = 3, of = Retries.class)
                public class RetryA implements Retries { }
                """);

        JavaFileObject v2 = JavaFileObjects.forSourceString(
                "com.example.RetryB",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(intValue = 3, of = Retries.class)
                public class RetryB implements Retries { }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("3");
        assertThat(compilation).hadErrorContaining("RetryA");
        assertThat(compilation).hadErrorContaining("RetryB");
    }

    // --- describeWrongAttributes: verify specific wrong attribute names in error ---

    @ParameterizedTest(name = "error for {0} feature with {1} attribute mentions {2}")
    @MethodSource("wrongAttributeDescriptionCases")
    void errorMessageDescribesSpecificWrongAttribute(String featureType, String variantAnnotation,
                                                      String expectedWrongAttr) {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.WrongAttr",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "wrong-attr", type = FeatureType.%s)
                public interface WrongAttr { }
                """.formatted(featureType));

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.BadImpl",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(%s, of = WrongAttr.class)
                public class BadImpl implements WrongAttr { }
                """.formatted(variantAnnotation));

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(expectedWrongAttr);
    }

    static Stream<Arguments> wrongAttributeDescriptionCases() {
        return Stream.of(
                // INT feature: string value → error mentions "string"
                Arguments.of("INT", "value = \"foo\"", "string"),
                // INT feature: long value → error mentions "long"
                Arguments.of("INT", "longValue = 99L", "long"),
                // INT feature: boolean value → error mentions "boolean"
                Arguments.of("INT", "booleanValue = \"true\"", "boolean"),
                // LONG feature: string value → error mentions "string"
                Arguments.of("LONG", "value = \"foo\"", "string"),
                // LONG feature: int value → error mentions "integer"
                Arguments.of("LONG", "intValue = 1", "integer"),
                // BOOLEAN feature: string value → error mentions "string"
                Arguments.of("BOOLEAN", "value = \"foo\"", "string"),
                // BOOLEAN feature: int value → error mentions "integer"
                Arguments.of("BOOLEAN", "intValue = 1", "integer"),
                // BOOLEAN feature: long value → error mentions "long"
                Arguments.of("BOOLEAN", "longValue = 1L", "long"),
                // DOUBLE feature: string value → error mentions "string"
                Arguments.of("DOUBLE", "value = \"foo\"", "string"),
                // DOUBLE feature: int value → error mentions "integer"
                Arguments.of("DOUBLE", "intValue = 1", "integer"),
                // DOUBLE feature: long value → error mentions "long"
                Arguments.of("DOUBLE", "longValue = 1L", "long"),
                // DOUBLE feature: boolean value → error mentions "boolean"
                Arguments.of("DOUBLE", "booleanValue = \"true\"", "boolean"),
                // STRING feature: int value → error mentions "integer"
                Arguments.of("STRING", "intValue = 1", "integer"),
                // STRING feature: long value → error mentions "long"
                Arguments.of("STRING", "longValue = 1L", "long")
        );
    }

    // --- WhenTrue on non-boolean feature should fail or be ignored ---

    @Test
    void compilesWhenTrueWithExplicitTargetOnMultiFeatureVariant() throws IOException {
        JavaFileObject feature1 = JavaFileObjects.forSourceString(
                "com.example.FeatureA",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "feat-a", type = FeatureType.BOOLEAN)
                public interface FeatureA { }
                """);

        JavaFileObject feature2 = JavaFileObjects.forSourceString(
                "com.example.FeatureB",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "feat-b", type = FeatureType.BOOLEAN)
                public interface FeatureB { }
                """);

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.MultiImpl",
                """
                package com.example;
                import com.flagzen.WhenTrue;
                import com.flagzen.WhenFalse;
                @WhenTrue(of = FeatureA.class)
                @WhenFalse(of = FeatureB.class)
                public class MultiImpl implements FeatureA, FeatureB { }
                """);

        JavaFileObject variantA = JavaFileObjects.forSourceString(
                "com.example.AFalse",
                """
                package com.example;
                import com.flagzen.WhenFalse;
                @WhenFalse(of = FeatureA.class)
                public class AFalse implements FeatureA { }
                """);

        JavaFileObject variantB = JavaFileObjects.forSourceString(
                "com.example.BTrue",
                """
                package com.example;
                import com.flagzen.WhenTrue;
                @WhenTrue(of = FeatureB.class)
                public class BTrue implements FeatureB { }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature1, feature2, variant, variantA, variantB);

        assertThat(compilation).succeeded();
    }

    // --- VariantModel ofLong/ofDouble factory methods ---

    @Test
    void variantModelOfLongCreatesLongTypedModel() {
        VariantModel model = VariantModel.ofLong("com.example.LongImpl", 5000L);
        assertThat(model.featureType()).isEqualTo(com.flagzen.FeatureType.LONG);
        assertThat(model.longVariantValue()).isEqualTo(5000L);
        assertThat(model.variantKeyLiteral()).isEqualTo("5000");
    }

    @Test
    void variantModelOfDoubleCreatesDoubleTypedModel() {
        VariantModel model = VariantModel.ofDouble("com.example.DoubleImpl", 0.5, 1e-10);
        assertThat(model.featureType()).isEqualTo(com.flagzen.FeatureType.DOUBLE);
        assertThat(model.doubleVariantValue()).isEqualTo(0.5);
        assertThat(model.doubleDelta()).isEqualTo(1e-10);
        assertThat(model.variantKeyLiteral()).isEqualTo("0.5");
    }
}
