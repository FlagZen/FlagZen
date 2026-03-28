package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.JavaFileObject;
import java.util.stream.Stream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit tests for compile-time type mismatch validation in FlagZenProcessor.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 6 behaviors x 2 = 12 max. Using 6 tests (1 parametrized + 5 individual).
 */
class TypeMismatchValidationTest {

    /**
     * Behavior 1: Wrong attribute for feature type is rejected with actionable error message.
     * Parametrized across all type mismatch combinations.
     */
    @ParameterizedTest(name = "rejects {1} attribute on {0} feature")
    @MethodSource("typeMismatchCases")
    void rejectsWrongAttributeForFeatureType(String featureType, String wrongAttr,
                                              String variantAnnotation, String expectedSuggestion) {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.TestFeature",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "test-feature", type = FeatureType.%s)
                public interface TestFeature {
                }
                """.formatted(featureType)
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.TestVariant",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(%s, of = TestFeature.class)
                public class TestVariant implements TestFeature {
                }
                """.formatted(variantAnnotation)
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("TestVariant");
        assertThat(compilation).hadErrorContaining(expectedSuggestion);
    }

    static Stream<Arguments> typeMismatchCases() {
        return Stream.of(
                // featureType, wrongAttrDescription, variantAnnotation, expectedSuggestion
                Arguments.of("INT", "string", "value = \"3\"", "intValue"),
                Arguments.of("BOOLEAN", "integer", "intValue = 1", "boolean"),
                Arguments.of("LONG", "booleanValue", "booleanValue = \"true\"", "longValue"),
                Arguments.of("DOUBLE", "integer", "intValue = 1", "doubleValue"),
                Arguments.of("INT", "long", "longValue = 1000L", "intValue")
        );
    }

    /**
     * Behavior 2: Mixed attribute types within same feature are rejected.
     */
    @Test
    void rejectsMixedAttributeTypesWithinSameFeature() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.MixFeature",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "mix-feature", type = FeatureType.INT)
                public interface MixFeature {
                }
                """
        );

        JavaFileObject correct = JavaFileObjects.forSourceString(
                "com.example.CorrectVariant",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(intValue = 3, of = MixFeature.class)
                public class CorrectVariant implements MixFeature {
                }
                """
        );

        JavaFileObject wrong = JavaFileObjects.forSourceString(
                "com.example.WrongVariant",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "fast", of = MixFeature.class)
                public class WrongVariant implements MixFeature {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, correct, wrong);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("WrongVariant");
    }

    /**
     * Behavior 3: Boolean REQUIRED feature missing a boolean value is rejected.
     */
    @Test
    void rejectsBooleanRequiredFeatureMissingFalseVariant() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.BoolFeature",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                import com.flagzen.FallbackStrategy;

                @Feature(value = "bool-feature", type = FeatureType.BOOLEAN, fallback = FallbackStrategy.REQUIRED)
                public interface BoolFeature {
                }
                """
        );

        JavaFileObject trueVariant = JavaFileObjects.forSourceString(
                "com.example.TrueImpl",
                """
                package com.example;

                import com.flagzen.WhenTrue;

                @WhenTrue(of = BoolFeature.class)
                public class TrueImpl implements BoolFeature {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, trueVariant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("false");
    }

    /**
     * Behavior 4: Error message includes feature flag key and variant class name.
     */
    @Test
    void errorMessageIncludesFeatureKeyAndVariantName() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.MaxRetries",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "max-retries", type = FeatureType.INT)
                public interface MaxRetries {
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.SlowRetry",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "3", of = MaxRetries.class)
                public class SlowRetry implements MaxRetries {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("max-retries");
        assertThat(compilation).hadErrorContaining("SlowRetry");
    }

    /**
     * Behavior 5: Actionable suggested fix contains the correct attribute name.
     */
    @Test
    void errorMessageContainsActionableSuggestedFix() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.IntFeature",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "int-feature", type = FeatureType.INT)
                public interface IntFeature {
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.BadVariant",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "wrong", of = IntFeature.class)
                public class BadVariant implements IntFeature {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("intValue");
        assertThat(compilation).hadErrorContaining("Use");
    }
}
