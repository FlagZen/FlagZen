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
 * Acceptance test: Developer declares an integer-typed feature with typed variants.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port).
 * Verifies that @Feature(type=INT) with @Variant(intValue=N) compiles and generates
 * a proxy with Map<Integer, Supplier<T>> variant map.
 */
class TypedVariantCompilationTest {

    @Test
    void compilesIntegerTypedFeatureAndGeneratesProxyWithIntegerVariantMap() throws IOException {
        // Given: a feature interface with INT type
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.MaxRetries",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "max-retries", type = FeatureType.INT)
                public interface MaxRetries {
                    int getMaxRetries();
                }
                """
        );

        // And: variant classes with integer values
        JavaFileObject lowRetries = JavaFileObjects.forSourceString(
                "com.example.LowRetries",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(intValue = 3, of = MaxRetries.class)
                public class LowRetries implements MaxRetries {
                    @Override
                    public int getMaxRetries() { return 3; }
                }
                """
        );

        JavaFileObject highRetries = JavaFileObjects.forSourceString(
                "com.example.HighRetries",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(intValue = 10, of = MaxRetries.class)
                public class HighRetries implements MaxRetries {
                    @Override
                    public int getMaxRetries() { return 10; }
                }
                """
        );

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, lowRetries, highRetries);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: a dispatch proxy is generated
        assertThat(compilation)
                .generatedSourceFile("com.example.MaxRetries_FlagZenProxy")
                .isNotNull();

        // And: the proxy uses Integer-keyed variant map
        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.MaxRetries_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        assertThat(proxySource)
                .contains("Map<Integer, Supplier<MaxRetries>> variants")
                .contains("public class MaxRetries_FlagZenProxy implements MaxRetries");

        // And: metadata is generated
        assertThat(compilation)
                .generatedSourceFile("com.example.MaxRetries_FlagZenMetadata")
                .isNotNull();
    }
}
