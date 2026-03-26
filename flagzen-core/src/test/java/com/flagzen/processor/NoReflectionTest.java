package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that generated proxy source code contains no runtime reflection
 * and uses direct method calls or map lookups for dispatch.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port output).
 */
class NoReflectionTest {

    @Test
    void generatedProxyContainsNoReflectionAndUsesMapDispatch() throws Exception {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public interface CheckoutFlow {
                    void execute();
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
                    @Override
                    public void execute() {}
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        assertThat(compilation).succeeded();

        var proxyFile = compilation.generatedSourceFile("com.example.CheckoutFlow_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // No reflection imports
        assertThat(proxySource)
                .doesNotContain("java.lang.reflect");

        // Dispatch uses map lookups (variants.get) and direct method calls (resolveVariant())
        assertThat(proxySource)
                .contains("variants.get(")
                .contains("resolveVariant()");
    }
}
