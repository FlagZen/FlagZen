package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Optional;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: value-based features (no {@code @Condition}, no order) must continue
 * to generate map-based O(1) dispatch proxies. The introduction of condition support
 * must not alter the code path for plain value-matched variants.
 *
 * <p>Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port).
 *
 * <p>Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class ConditionBackwardCompatibilityTest {

    @Test
    void valueBased_featureGeneratesMapLookupProxy_whenNoConditionOrOrderPresent() throws IOException {
        // Given: a STRING feature with two value-based variants (no @Condition, no order)
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.PaymentMethod",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("payment-method")
                public interface PaymentMethod {
                    String label();
                }
                """);

        JavaFileObject cardVariant = JavaFileObjects.forSourceString(
                "com.example.CardPayment",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "card", of = PaymentMethod.class)
                public class CardPayment implements PaymentMethod {
                    @Override
                    public String label() { return "Credit Card"; }
                }
                """);

        JavaFileObject bankVariant = JavaFileObjects.forSourceString(
                "com.example.BankPayment",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "bank", of = PaymentMethod.class)
                public class BankPayment implements PaymentMethod {
                    @Override
                    public String label() { return "Bank Transfer"; }
                }
                """);

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.DefaultPayment",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = PaymentMethod.class)
                public class DefaultPayment implements PaymentMethod {
                    @Override
                    public String label() { return "Default"; }
                }
                """);

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, cardVariant, bankVariant, defaultVariant);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: the proxy is generated
        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.PaymentMethod_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // And: the proxy uses map-based O(1) lookup (backward-compatible path)
        assertThat(proxySource)
                .contains("Map<String, Supplier<PaymentMethod>>")
                .contains("variants.get(")
                .describedAs("value-based features must use map-based dispatch");

        // And: no condition/predicate infrastructure is generated
        assertThat(proxySource)
                .doesNotContain("Predicate")
                .doesNotContain("Condition")
                .describedAs("value-based features must not include predicate dispatch code");

        // And: the proxy accepts variants via constructor (wired externally by metadata)
        assertThat(proxySource)
                .contains("Map<String, Supplier<PaymentMethod>> variants")
                .contains("Supplier<PaymentMethod> defaultVariant");
    }
}
