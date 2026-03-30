package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Acceptance test: {@code @Variant} supports conditional dispatch via {@code @Condition}
 * and explicit ordering via {@code order}.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation result.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests. Using 3 tests.
 */
class ConditionAnnotationTest {

    @Test
    void compilesVariantWithConditionMatchesAndOrder() {
        // Given: a feature interface
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.RateLimiter",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("rate-limiter")
                public interface RateLimiter {
                    int maxRequests();
                }
                """
        );

        // And: a predicate class
        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsEnterprise",
                """
                package com.example;

                import java.util.function.Predicate;

                public class IsEnterprise implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return "enterprise".equals(s); }
                }
                """
        );

        // And: a variant with @Condition(matches = ...) and order
        JavaFileObject conditionalVariant = JavaFileObjects.forSourceString(
                "com.example.EnterpriseRateLimiter",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "high", of = RateLimiter.class,
                         when = @Condition(matches = IsEnterprise.class),
                         order = 1)
                public class EnterpriseRateLimiter implements RateLimiter {
                    @Override
                    public int maxRequests() { return 10000; }
                }
                """
        );

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, predicate, conditionalVariant);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();
    }

    @Test
    void compilesConditionWithNotMatches() {
        // Given: a feature and predicate
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.Logging",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("logging")
                public interface Logging {
                    String level();
                }
                """
        );

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsProduction",
                """
                package com.example;

                import java.util.function.Predicate;

                public class IsProduction implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return "prod".equals(s); }
                }
                """
        );

        // And: a variant with notMatches condition
        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.VerboseLogging",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "verbose", of = Logging.class,
                         when = @Condition(notMatches = IsProduction.class))
                public class VerboseLogging implements Logging {
                    @Override
                    public String level() { return "DEBUG"; }
                }
                """
        );

        // When/Then: compilation succeeds
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, predicate, variant);

        assertThat(compilation).succeeded();
    }

    @Test
    void compilesExistingVariantWithoutWhenOrOrder() {
        // Given: existing @Variant usage without new attributes (backward compat)
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.Theme",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("theme")
                public interface Theme {
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.DarkTheme",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "dark", of = Theme.class)
                public class DarkTheme implements Theme {
                }
                """
        );

        // When/Then: compilation succeeds (backward compat)
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        assertThat(compilation).succeeded();
    }
}
