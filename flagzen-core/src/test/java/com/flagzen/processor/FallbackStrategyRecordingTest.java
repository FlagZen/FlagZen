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
 * Unit test: @Feature with explicit fallback strategy records it in generated metadata.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated metadata (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class FallbackStrategyRecordingTest {

    @Test
    void recordsFallbackStrategyInGeneratedMetadataWhenExplicitlyConfigured() throws IOException {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.CheckoutFlow",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FallbackStrategy;

                @Feature(value = "checkout-flow", fallback = FallbackStrategy.REQUIRED)
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

        Optional<JavaFileObject> metadataFile = compilation.generatedSourceFile(
                "com.example.CheckoutFlow_FlagZenMetadata");
        assertThat(metadataFile).isPresent();
        String metadataSource = metadataFile.get().getCharContent(false).toString();
        assertThat(metadataSource).contains("FallbackStrategy.REQUIRED");

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.CheckoutFlow_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();
        assertThat(proxySource).contains("FallbackStrategy");
    }
}
