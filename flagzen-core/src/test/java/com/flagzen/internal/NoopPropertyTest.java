package com.flagzen.internal;

import com.flagzen.processor.FlagZenProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test: for ANY feature with NOOP strategy, NO generated method throws
 * and return values are safe defaults for their types.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated proxy (driven port).
 *
 * Invariant: the generated NOOP proxy is structurally safe for all Java return types:
 * void (no NPE), boolean (false), int (0), long (0L), double (0.0d), float (0.0f),
 * String (null). No UnmatchedVariantException anywhere in generated code.
 *
 * Test Budget: 1 behavior (NOOP safe defaults across all types) x 2 = 2 max. Using 1 test.
 */
class NoopPropertyTest {

    @Test
    void generatedNoopProxyNeverThrowsAndReturnsSafeDefaultsForAllTypes() throws Exception {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.MultiType",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FallbackStrategy;

                @Feature(value = "multi-type", fallback = FallbackStrategy.NOOP)
                public interface MultiType {
                    void doNothing();
                    boolean getBoolean();
                    int getInt();
                    long getLong();
                    double getDouble();
                    float getFloat();
                    String getText();
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.MultiTypeActive",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "ACTIVE", of = MultiType.class)
                public class MultiTypeActive implements MultiType {
                    @Override public void doNothing() {}
                    @Override public boolean getBoolean() { return true; }
                    @Override public int getInt() { return 42; }
                    @Override public long getLong() { return 42L; }
                    @Override public double getDouble() { return 3.14; }
                    @Override public float getFloat() { return 1.5f; }
                    @Override public String getText() { return "active"; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        assertThat(compilation).succeeded();

        JavaFileObject proxyFile = compilation.generatedSourceFile("com.example.MultiType_FlagZenProxy")
                .orElseThrow(() -> new AssertionError("Proxy not generated"));
        String proxySource = proxyFile.getCharContent(false).toString();

        // Invariant 1: NOOP proxy must NEVER contain UnmatchedVariantException
        assertThat(proxySource)
                .as("NOOP proxy must never throw UnmatchedVariantException")
                .doesNotContain("UnmatchedVariantException");

        // Invariant 2: resolveVariant returns null (graceful fallback, not exception)
        assertThat(proxySource).contains("return null");

        // Invariant 3: void method has null-guard to prevent NPE
        assertThat(proxySource).contains("if (delegate != null)");

        // Invariant 4: safe defaults for each primitive return type
        assertThat(proxySource).contains("return false");    // boolean
        assertThat(proxySource).contains("return 0;");       // int
        assertThat(proxySource).contains("return 0L");       // long
        assertThat(proxySource).contains("return 0.0d");     // double
        assertThat(proxySource).contains("return 0.0f");     // float
    }
}
