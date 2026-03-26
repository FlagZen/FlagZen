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
 * Acceptance test for step 01-01: @Feature + @Variant annotations and proxy generation.
 *
 * Scenario: Developer defines a feature with variants and a dispatch proxy is generated.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port output).
 */
class ProxyGenerationTest {

    @Test
    void generatesProxyThatImplementsFeatureInterface() throws IOException {
        // Given: a feature interface annotated with @Feature
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

        // And: variant classes annotated with @Variant
        JavaFileObject classicVariant = JavaFileObjects.forSourceString(
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

        JavaFileObject streamlinedVariant = JavaFileObjects.forSourceString(
                "com.example.StreamlinedCheckout",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "STREAMLINED", of = CheckoutFlow.class)
                public class StreamlinedCheckout implements CheckoutFlow {
                    @Override
                    public void execute() {}
                }
                """
        );

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, classicVariant, streamlinedVariant);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: a dispatch proxy "CheckoutFlow_FlagZenProxy" is generated
        assertThat(compilation)
                .generatedSourceFile("com.example.CheckoutFlow_FlagZenProxy")
                .isNotNull();

        // And: the proxy implements the "CheckoutFlow" interface
        Optional<JavaFileObject> generatedFile = compilation.generatedSourceFile(
                "com.example.CheckoutFlow_FlagZenProxy");
        assertThat(generatedFile).isPresent();

        String generatedSource = generatedFile.get()
                .getCharContent(false).toString();

        // Verify structural properties of the generated proxy
        assertThat(generatedSource)
                .contains("public class CheckoutFlow_FlagZenProxy implements CheckoutFlow")
                .contains("private final String flagKey")
                .contains("private final Map<String, Supplier<CheckoutFlow>> variants")
                .contains("private final Supplier<CheckoutFlow> defaultVariant")
                .contains("private final FallbackStrategy fallbackStrategy")
                .contains("CheckoutFlow_FlagZenProxy(String flagKey")
                .contains("public void execute()")
                .contains("return \"FlagZenProxy[checkout-flow]\"");

        // Verify no runtime reflection
        assertThat(generatedSource)
                .doesNotContain("java.lang.reflect");
    }
}
