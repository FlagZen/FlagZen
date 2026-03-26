package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: @Variant class not implementing its feature interface produces a compile error.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class VariantInterfaceValidationTest {

    @Test
    void emitsCompileErrorWhenVariantDoesNotImplementFeatureInterface() {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public interface CheckoutFlow {
                }
                """
        );

        JavaFileObject brokenVariant = JavaFileObjects.forSourceString(
                "com.example.BrokenVariant",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class BrokenVariant {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, brokenVariant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must implement the feature interface");
    }
}
