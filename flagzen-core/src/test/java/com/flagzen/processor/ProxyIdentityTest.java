package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Verifies that generated proxy classes provide a descriptive toString identity.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port output).
 */
class ProxyIdentityTest {

    @Test
    void generatedProxyToStringReturnsFlagZenProxyWithFlagKey() throws Exception {
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

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.ClassicCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "CLASSIC", of = CheckoutFlow.class)
                public class ClassicCheckout implements CheckoutFlow {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        assertThat(compilation).succeeded();

        var proxyFile = compilation.generatedSourceFile("com.example.CheckoutFlow_FlagZenProxy");
        org.assertj.core.api.Assertions.assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        org.assertj.core.api.Assertions.assertThat(proxySource)
                .contains("return \"FlagZenProxy[checkout-flow]\"");
    }
}
