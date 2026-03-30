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
 * Acceptance test: ordered dispatch proxy generation for INT, LONG, DOUBLE, BOOLEAN features
 * with conditions and notMatches negation.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port).
 * Test Budget: 4 behaviors x 2 = 8 max unit tests. Using 4 tests.
 *
 * Behaviors:
 * 1. INT feature with ordered dispatch (exact match + IntPredicate condition)
 * 2. LONG feature with ordered dispatch (exact match + LongPredicate condition with notMatches)
 * 3. DOUBLE feature with ordered dispatch (DoublePredicate condition)
 * 4. BOOLEAN feature with ordered dispatch (Predicate&lt;Boolean&gt; condition)
 */
class TypedConditionDispatchTest {

    @Test
    void intFeatureGeneratesOrderedDispatchWithIntPredicateAndExactMatch() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.RateLimit",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "rate-limit", type = FeatureType.INT)
                public interface RateLimit {
                    int maxRequests();
                }
                """);

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsHighTraffic",
                """
                package com.example;

                import java.util.function.IntPredicate;

                public class IsHighTraffic implements IntPredicate {
                    @Override
                    public boolean test(int value) { return value > 1000; }
                }
                """);

        JavaFileObject exactVariant = JavaFileObjects.forSourceString(
                "com.example.UnlimitedRate",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(intValue = 0, of = RateLimit.class, order = 1)
                public class UnlimitedRate implements RateLimit {
                    @Override
                    public int maxRequests() { return Integer.MAX_VALUE; }
                }
                """);

        JavaFileObject condVariant = JavaFileObjects.forSourceString(
                "com.example.ThrottledRate",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(intValue = 500, of = RateLimit.class,
                         when = @Condition(matches = IsHighTraffic.class),
                         order = 2)
                public class ThrottledRate implements RateLimit {
                    @Override
                    public int maxRequests() { return 500; }
                }
                """);

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.StandardRate",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = RateLimit.class)
                public class StandardRate implements RateLimit {
                    @Override
                    public int maxRequests() { return 100; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, exactVariant, condVariant, defaultVariant);

        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.RateLimit_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // Uses IntPredicate (not Predicate<String>)
        assertThat(proxySource).contains("IntPredicate");
        assertThat(proxySource).contains("new IsHighTraffic()");

        // Uses getInt for flag resolution
        assertThat(proxySource).contains("getInt(");

        // Has exact int match and predicate test
        assertThat(proxySource).contains("== 0");  // exact match for intValue=0
        assertThat(proxySource).contains(".test(");

        // No map-based dispatch
        assertThat(proxySource).doesNotContain("Map<Integer, Supplier<RateLimit>>");
    }

    @Test
    void longFeatureGeneratesOrderedDispatchWithNotMatchesNegation() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.CacheTtl",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "cache-ttl", type = FeatureType.LONG)
                public interface CacheTtl {
                    long ttlMillis();
                }
                """);

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsShortLived",
                """
                package com.example;

                import java.util.function.LongPredicate;

                public class IsShortLived implements LongPredicate {
                    @Override
                    public boolean test(long value) { return value < 1000L; }
                }
                """);

        // notMatches = negated: activate when NOT short-lived
        JavaFileObject condVariant = JavaFileObjects.forSourceString(
                "com.example.LongCacheTtl",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(longValue = 60000L, of = CacheTtl.class,
                         when = @Condition(notMatches = IsShortLived.class),
                         order = 1)
                public class LongCacheTtl implements CacheTtl {
                    @Override
                    public long ttlMillis() { return 60000L; }
                }
                """);

        JavaFileObject exactVariant = JavaFileObjects.forSourceString(
                "com.example.NoCacheTtl",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(longValue = 0L, of = CacheTtl.class, order = 2)
                public class NoCacheTtl implements CacheTtl {
                    @Override
                    public long ttlMillis() { return 0L; }
                }
                """);

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.DefaultCacheTtl",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = CacheTtl.class)
                public class DefaultCacheTtl implements CacheTtl {
                    @Override
                    public long ttlMillis() { return 5000L; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, condVariant, exactVariant, defaultVariant);

        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.CacheTtl_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // Uses LongPredicate
        assertThat(proxySource).contains("LongPredicate");
        assertThat(proxySource).contains("new IsShortLived()");

        // Uses getLong for flag resolution
        assertThat(proxySource).contains("getLong(");

        // Negated predicate uses !pred.test()
        assertThat(proxySource).contains("!pred0.test(");

        // Exact long match
        assertThat(proxySource).contains("== 0L");
    }

    @Test
    void doubleFeatureGeneratesOrderedDispatchWithDoublePredicate() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.Threshold",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "threshold", type = FeatureType.DOUBLE)
                public interface Threshold {
                    double value();
                }
                """);

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsAboveHalf",
                """
                package com.example;

                import java.util.function.DoublePredicate;

                public class IsAboveHalf implements DoublePredicate {
                    @Override
                    public boolean test(double value) { return value > 0.5; }
                }
                """);

        JavaFileObject condVariant = JavaFileObjects.forSourceString(
                "com.example.HighThreshold",
                """
                package com.example;

                import com.flagzen.CloseTo;
                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(doubleValue = @CloseTo(0.9), of = Threshold.class,
                         when = @Condition(matches = IsAboveHalf.class),
                         order = 1)
                public class HighThreshold implements Threshold {
                    @Override
                    public double value() { return 0.9; }
                }
                """);

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.DefaultThreshold",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = Threshold.class)
                public class DefaultThreshold implements Threshold {
                    @Override
                    public double value() { return 0.5; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, condVariant, defaultVariant);

        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.Threshold_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // Uses DoublePredicate
        assertThat(proxySource).contains("DoublePredicate");
        assertThat(proxySource).contains("new IsAboveHalf()");

        // Uses getDouble for flag resolution
        assertThat(proxySource).contains("getDouble(");

        // Has predicate test call
        assertThat(proxySource).contains(".test(");
    }

    @Test
    void booleanFeatureGeneratesOrderedDispatchWithPredicateOfBoolean() throws IOException {
        JavaFileObject feature = JavaFileObjects.forSourceString(
                "com.example.AuditMode",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "audit-mode", type = FeatureType.BOOLEAN)
                public interface AuditMode {
                    String mode();
                }
                """);

        JavaFileObject predicate = JavaFileObjects.forSourceString(
                "com.example.IsEnabled",
                """
                package com.example;

                import java.util.function.Predicate;

                public class IsEnabled implements Predicate<Boolean> {
                    @Override
                    public boolean test(Boolean value) { return value; }
                }
                """);

        JavaFileObject condVariant = JavaFileObjects.forSourceString(
                "com.example.FullAudit",
                """
                package com.example;

                import com.flagzen.Condition;
                import com.flagzen.Variant;

                @Variant(booleanValue = "true", of = AuditMode.class,
                         when = @Condition(matches = IsEnabled.class),
                         order = 1)
                public class FullAudit implements AuditMode {
                    @Override
                    public String mode() { return "full"; }
                }
                """);

        JavaFileObject defaultVariant = JavaFileObjects.forSourceString(
                "com.example.NoAudit",
                """
                package com.example;

                import com.flagzen.DefaultVariant;

                @DefaultVariant(of = AuditMode.class)
                public class NoAudit implements AuditMode {
                    @Override
                    public String mode() { return "none"; }
                }
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, predicate, condVariant, defaultVariant);

        assertThat(compilation).succeeded();

        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.AuditMode_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        // Uses Predicate<Boolean>
        assertThat(proxySource).contains("Predicate<Boolean>");
        assertThat(proxySource).contains("new IsEnabled()");

        // Uses getBoolean for flag resolution
        assertThat(proxySource).contains("getBoolean(");

        // Has predicate test call
        assertThat(proxySource).contains(".test(");
    }
}
