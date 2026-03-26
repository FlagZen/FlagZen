package com.flagzen.internal;

import com.flagzen.processor.FlagZenProcessor;
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
 * Unit test: NOOP fallback strategy generates safe defaults in proxy.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated proxy (driven port output).
 * Test Budget: 2 behaviors (void no-op, typed default) x 2 = 4 max. Using 1 test.
 */
class NoopFallbackTest {

    @Test
    void generatesNoopFallbackWithSafeDefaultsForVoidAndTypedMethods() throws IOException {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.DarkMode",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FallbackStrategy;

                @Feature(value = "dark-mode", fallback = FallbackStrategy.NOOP)
                public interface DarkMode {
                    void apply();
                    boolean isEnabled();
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.DarkModeEnabled",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "ENABLED", of = DarkMode.class)
                public class DarkModeEnabled implements DarkMode {
                    @Override
                    public void apply() {}
                    @Override
                    public boolean isEnabled() { return true; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.DarkMode_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // NOOP proxy delegates when variant found, returns safe defaults when not
        assertThat(proxySource)
                .contains("public void apply()")
                .contains("public boolean isEnabled()");

        // resolveVariant returns null for NOOP when no match (instead of throwing)
        assertThat(proxySource).contains("return null");

        // boolean method returns false as safe default when delegate is null
        assertThat(proxySource).contains("return false");

        // NOOP proxy must NOT throw UnmatchedVariantException
        assertThat(proxySource).doesNotContain("UnmatchedVariantException");
    }
}
