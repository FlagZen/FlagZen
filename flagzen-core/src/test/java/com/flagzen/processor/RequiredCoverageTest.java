package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: REQUIRED fallback strategy with incomplete variant enum coverage produces
 * compile error listing the missing values.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class RequiredCoverageTest {

    @Test
    void emitsCompileErrorWhenRequiredStrategyHasIncompleteVariantCoverage() {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FallbackStrategy;

                @Feature(value = "checkout-flow", fallback = FallbackStrategy.REQUIRED)
                public interface CheckoutFlow {
                    enum Variant { CLASSIC, STREAMLINED, PREMIUM }
                }
                """
        );

        JavaFileObject classicVariant = JavaFileObjects.forSourceString(
                "com.example.ClassicCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class ClassicCheckout implements CheckoutFlow {
                }
                """
        );

        JavaFileObject streamlinedVariant = JavaFileObjects.forSourceString(
                "com.example.StreamlinedCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "STREAMLINED", of = CheckoutFlow.class)
                public class StreamlinedCheckout implements CheckoutFlow {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, classicVariant, streamlinedVariant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("REQUIRED");
        assertThat(compilation).hadErrorContaining("PREMIUM");
        assertThat(compilation).hadErrorContaining("has no implementation");
    }
}
