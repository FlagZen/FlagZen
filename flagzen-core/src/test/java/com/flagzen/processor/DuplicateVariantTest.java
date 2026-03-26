package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: two variant classes with the same value for the same feature produce a compile error
 * identifying both conflicting classes.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class DuplicateVariantTest {

    @Test
    void emitsCompileErrorIdentifyingBothConflictingVariantsWithSameValue() {
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

        JavaFileObject classicCheckout = JavaFileObjects.forSourceString(
                "com.example.ClassicCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class ClassicCheckout implements CheckoutFlow {
                }
                """
        );

        JavaFileObject legacyCheckout = JavaFileObjects.forSourceString(
                "com.example.LegacyCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class LegacyCheckout implements CheckoutFlow {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, classicCheckout, legacyCheckout);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("CLASSIC");
        assertThat(compilation).hadErrorContaining("checkout-flow");
        assertThat(compilation).hadErrorContaining("ClassicCheckout");
        assertThat(compilation).hadErrorContaining("LegacyCheckout");
    }
}
