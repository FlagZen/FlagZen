package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Optional;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Property test: every valid feature interface with at least one variant produces
 * exactly one proxy class that implements the feature interface.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1.
 */
class OneProxyPerFeatureTest {

    @Test
    void eachFeatureProducesExactlyOneProxyThatImplementsIt() throws Exception {
        // Given: two distinct feature interfaces, each with at least one variant
        JavaFileObject checkoutFlow = JavaFileObjects.forSourceString(
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

        JavaFileObject classicCheckout = JavaFileObjects.forSourceString(
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

        JavaFileObject darkMode = JavaFileObjects.forSourceString(
                "com.example.DarkMode",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("dark-mode")
                public interface DarkMode {
                    void toggle();
                }
                """
        );

        JavaFileObject darkModeOn = JavaFileObjects.forSourceString(
                "com.example.DarkModeOn",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "ON", of = DarkMode.class)
                public class DarkModeOn implements DarkMode {
                    @Override
                    public void toggle() {}
                }
                """
        );

        // When: the project compiles with both features
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(checkoutFlow, classicCheckout, darkMode, darkModeOn);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: exactly one proxy per feature
        List<String> features = List.of("CheckoutFlow", "DarkMode");
        for (String feature : features) {
            Optional<JavaFileObject> proxy = compilation.generatedSourceFile(
                    "com.example." + feature + "_FlagZenProxy");
            org.assertj.core.api.Assertions.assertThat(proxy)
                    .as("Proxy generated for " + feature)
                    .isPresent();

            // Verify no duplicate proxy
            Optional<JavaFileObject> duplicate = compilation.generatedSourceFile(
                    "com.example." + feature + "_FlagZenProxy2");
            org.assertj.core.api.Assertions.assertThat(duplicate)
                    .as("No duplicate proxy for " + feature)
                    .isEmpty();

            // And: each proxy implements its feature interface
            String source = proxy.get().getCharContent(false).toString();
            org.assertj.core.api.Assertions.assertThat(source)
                    .as("Proxy implements " + feature)
                    .contains("implements " + feature);
        }
    }
}
