package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: variant value not in feature's inner Variant enum produces compile error listing valid values.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class EnumValidationTest {

    @Test
    void emitsCompileErrorWhenVariantValueNotInEnum() {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public interface CheckoutFlow {
                    enum Variant { CLASSIC, STREAMLINED, PREMIUM }
                }
                """
        );

        JavaFileObject invalidVariant = JavaFileObjects.forSourceString(
                "com.example.TurboCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "TURBO", of = CheckoutFlow.class)
                public class TurboCheckout implements CheckoutFlow {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, invalidVariant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(
                "@Variant(\"TURBO\") does not match any value in CheckoutFlow.Variant"
        );
        assertThat(compilation).hadErrorContaining(
                "Valid values: CLASSIC, STREAMLINED, PREMIUM"
        );
    }
}
