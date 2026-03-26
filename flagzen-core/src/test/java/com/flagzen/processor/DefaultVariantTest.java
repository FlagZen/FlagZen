package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: A class annotated with @DefaultVariant compiles successfully
 * and is registered as the fallback for its feature in generated metadata.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated metadata (driven port output).
 * Test Budget: 2 behaviors x 2 = 4 max. Using 1 test.
 */
class DefaultVariantTest {

    @Test
    void registersDefaultVariantAsFallbackInGeneratedMetadata() {
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

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.DefaultCheckout",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = CheckoutFlow.class)
                public class DefaultCheckout implements CheckoutFlow {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, defaultVariant);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.CheckoutFlow_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("DefaultCheckout::new");
    }
}
