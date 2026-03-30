package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Validates compile-time checks for {@code @Condition} predicates on {@code @Variant}.
 *
 * <p>Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics.
 * Test Budget: 5 behaviors x 2 = 10 max unit tests. Using 5 tests.
 */
class ConditionValidationTest {

    @Test
    void rejectsPredicateNotImplementingCorrectFunctionalInterface() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Greeting",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("greeting")
                public interface Greeting {
                    String greet();
                }
                """
        );

        // A predicate that implements IntPredicate, but the feature is STRING type
        JavaFileObject wrongPredicate = JavaFileObjects.forSourceString(
                "com.example.WrongPredicate",
                """
                package com.example;

                import java.util.function.IntPredicate;

                public class WrongPredicate implements IntPredicate {
                    @Override
                    public boolean test(int value) { return true; }
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.FormalGreeting",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "formal", of = Greeting.class,
                         when = @Condition(matches = WrongPredicate.class))
                public class FormalGreeting implements Greeting {
                    @Override
                    public String greet() { return "Good day"; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, wrongPredicate, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Predicate");
        assertThat(compilation).hadErrorContaining("WrongPredicate");
    }

    @Test
    void rejectsPredicateWithoutNoArgConstructor() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Banner",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("banner")
                public interface Banner {
                    String text();
                }
                """
        );

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.NeedsArgPredicate",
                """
                package com.example;

                import java.util.function.Predicate;

                public class NeedsArgPredicate implements Predicate<String> {
                    private final String required;
                    public NeedsArgPredicate(String required) {
                        this.required = required;
                    }
                    @Override
                    public boolean test(String s) { return required.equals(s); }
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.PromoBanner",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "promo", of = Banner.class,
                         when = @Condition(matches = NeedsArgPredicate.class))
                public class PromoBanner implements Banner {
                    @Override
                    public String text() { return "Sale!"; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("no-arg constructor");
        assertThat(compilation).hadErrorContaining("NeedsArgPredicate");
    }

    @Test
    void rejectsDuplicateOrderValues() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Pricing",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("pricing")
                public interface Pricing {
                    int price();
                }
                """
        );

        JavaFileObject predA = JavaFileObjects.forSourceString(
                "com.example.CheckA",
                """
                package com.example;

                import java.util.function.Predicate;

                public class CheckA implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return true; }
                }
                """
        );

        JavaFileObject predB = JavaFileObjects.forSourceString(
                "com.example.CheckB",
                """
                package com.example;

                import java.util.function.Predicate;

                public class CheckB implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return false; }
                }
                """
        );

        JavaFileObject variantA = JavaFileObjects.forSourceString(
                "com.example.PricingA",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "a", of = Pricing.class,
                         when = @Condition(matches = CheckA.class),
                         order = 1)
                public class PricingA implements Pricing {
                    @Override
                    public int price() { return 100; }
                }
                """
        );

        JavaFileObject variantB = JavaFileObjects.forSourceString(
                "com.example.PricingB",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "b", of = Pricing.class,
                         when = @Condition(matches = CheckB.class),
                         order = 1)
                public class PricingB implements Pricing {
                    @Override
                    public int price() { return 200; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predA, predB, variantA, variantB);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("order");
        assertThat(compilation).hadErrorContaining("1");
    }

    @Test
    void rejectsBothMatchesAndNotMatchesOnSameCondition() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Layout",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("layout")
                public interface Layout {
                    String name();
                }
                """
        );

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.SomePredicate",
                """
                package com.example;

                import java.util.function.Predicate;

                public class SomePredicate implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return true; }
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.CompactLayout",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "compact", of = Layout.class,
                         when = @Condition(matches = SomePredicate.class,
                                           notMatches = SomePredicate.class))
                public class CompactLayout implements Layout {
                    @Override
                    public String name() { return "compact"; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("matches");
        assertThat(compilation).hadErrorContaining("notMatches");
        assertThat(compilation).hadErrorContaining("mutually exclusive");
    }

    @Test
    void rejectsRequiredStrategyWithConditionsButNoDefaultVariant() {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Cache",
                """
                package com.example;

                import com.flagzen.FallbackStrategy;
                import com.flagzen.Feature;

                @Feature(value = "cache", fallback = FallbackStrategy.REQUIRED)
                public interface Cache {
                    int ttl();
                }
                """
        );

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsHighTraffic",
                """
                package com.example;

                import java.util.function.Predicate;

                public class IsHighTraffic implements Predicate<String> {
                    @Override
                    public boolean test(String s) { return true; }
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.AggressiveCache",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(value = "aggressive", of = Cache.class,
                         when = @Condition(matches = IsHighTraffic.class),
                         order = 1)
                public class AggressiveCache implements Cache {
                    @Override
                    public int ttl() { return 3600; }
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("REQUIRED");
        assertThat(compilation).hadErrorContaining("@DefaultVariant");
        assertThat(compilation).hadErrorContaining("condition");
    }
}
