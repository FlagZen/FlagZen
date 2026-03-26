package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: A variant class with repeated @Variant annotations for two features
 * compiles successfully and is registered in both feature metadata files.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated metadata (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class MultiFeatureVariantTest {

    @Test
    void registersVariantForBothFeaturesWhenAnnotatedWithRepeatedVariant() {
        JavaFileObject checkoutFlow = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public interface CheckoutFlow {
                }
                """
        );

        JavaFileObject paymentMethod = JavaFileObjects.forSourceString(
                "com.example.PaymentMethod",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("payment-method")
                public interface PaymentMethod {
                }
                """
        );

        JavaFileObject multiVariant = JavaFileObjects.forSourceString(
                "com.example.PremiumCreditCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "PREMIUM", of = CheckoutFlow.class)
                @Variant(value = "CREDIT_CARD", of = PaymentMethod.class)
                public class PremiumCreditCheckout implements CheckoutFlow, PaymentMethod {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(checkoutFlow, paymentMethod, multiVariant);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.CheckoutFlow_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("PremiumCreditCheckout");
        assertThat(compilation)
                .generatedSourceFile("com.example.PaymentMethod_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("PremiumCreditCheckout");
    }
}
