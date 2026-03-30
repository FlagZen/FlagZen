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
 * Acceptance test: ordered dispatch proxy generation for STRING features with conditions.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port).
 * Test Budget: 3 behaviors x 2 = 6 max unit tests. Using 3 tests.
 *
 * Behaviors:
 * 1. Ordered dispatch generates predicate fields, ordered if-else chain (exact match + predicate)
 * 2. Negated predicate generates !pred.test() in dispatch chain
 * 3. Features without order still use map-based O(1) lookup (no regression)
 */
class OrderedDispatchProxyTest {

    @Test
    void generatesOrderedDispatchProxyWithPredicateFieldsAndIfElseChain() throws IOException {
        // Given: a STRING feature
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Pricing",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("pricing")
                public interface Pricing {
                    String plan();
                }
                """);

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
                """);

        // And: an exact-match variant with order=1
        JavaFileObject exactVariant = JavaFileObjects.forSourceString(
                "com.example.FreePricing",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "free", of = Pricing.class, order = 1)
                public class FreePricing implements Pricing {
                    @Override
                    public String plan() { return "free"; }
                }
                """);

        // And: a condition-based variant with order=2
        JavaFileObject condVariant = JavaFileObjects.forSourceString(
                "com.example.EnterprisePricing",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "enterprise", of = Pricing.class,
                         when = @Condition(matches = IsEnterprise.class),
                         order = 2)
                public class EnterprisePricing implements Pricing {
                    @Override
                    public String plan() { return "enterprise"; }
                }
                """);

        // And: a default variant
        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.StandardPricing",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = Pricing.class)
                public class StandardPricing implements Pricing {
                    @Override
                    public String plan() { return "standard"; }
                }
                """);

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, exactVariant, condVariant, defaultVariant);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: the proxy is generated
        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.Pricing_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // And: the proxy has predicate fields (instantiated via new, not reflection)
        assertThat(proxySource).contains("Predicate<String>");
        assertThat(proxySource).contains("new IsEnterprise()");

        // And: the proxy does NOT have a variants map (ordered dispatch replaces it)
        assertThat(proxySource).doesNotContain("Map<String, Supplier<Pricing>>");

        // And: the resolve method uses ordered if-else dispatch
        // Exact match at order=1: value.equals("free")
        assertThat(proxySource).contains(".equals(\"free\")");
        // Condition match at order=2: predicate.test(rawValue)
        assertThat(proxySource).contains(".test(");

        // And: default variant is still available
        assertThat(proxySource).contains("defaultVariant");
    }

    @Test
    void generatesNegatedPredicateInOrderedDispatch() throws IOException {
        // Given: a STRING feature with a negated condition
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Logging",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("logging")
                public interface Logging {
                    String level();
                }
                """);

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsProduction",
                """
                package com.example;

                import java.util.function.Predicate;

                public class IsProduction implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return "prod".equals(s); }
                }
                """);

        // notMatches = negated predicate
        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.VerboseLogging",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "verbose", of = Logging.class,
                         when = @Condition(notMatches = IsProduction.class),
                         order = 1)
                public class VerboseLogging implements Logging {
                    @Override
                    public String level() { return "DEBUG"; }
                }
                """);

        JavaFileObject defaultV = JavaFileObjects.forSourceString(
                "com.example.DefaultLogging",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = Logging.class)
                public class DefaultLogging implements Logging {
                    @Override
                    public String level() { return "INFO"; }
                }
                """);

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, variant, defaultV);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.Logging_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // And: the negated predicate generates !pred.test()
        assertThat(proxySource).contains("!").describedAs("negated predicate should use !");
        assertThat(proxySource).contains(".test(");
    }

    @Test
    void featureWithoutOrderStillUsesMapBasedLookup() throws IOException {
        // Given: a STRING feature with plain variants (no order, no conditions)
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Theme",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("theme")
                public interface Theme {
                    String color();
                }
                """);

        JavaFileObject darkTheme = JavaFileObjects.forSourceString(
                "com.example.DarkTheme",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "dark", of = Theme.class)
                public class DarkTheme implements Theme {
                    @Override
                    public String color() { return "black"; }
                }
                """);

        JavaFileObject lightTheme = JavaFileObjects.forSourceString(
                "com.example.LightTheme",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "light", of = Theme.class)
                public class LightTheme implements Theme {
                    @Override
                    public String color() { return "white"; }
                }
                """);

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, darkTheme, lightTheme);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.Theme_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // And: the proxy uses map-based lookup (no regression)
        assertThat(proxySource).contains("Map<String, Supplier<Theme>>");
        assertThat(proxySource).contains("variants.get(");

        // And: no predicate fields
        assertThat(proxySource).doesNotContain("Predicate");
    }
}
