package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests for @CloseTo overlap detection in the annotation processor.
 */
class CloseToOverlapValidationTest {

    @Test
    void rejectsOverlappingCloseToRangesBetweenTwoVariants() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.F1",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "f1", type = FeatureType.DOUBLE)
                public interface F1 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.V1",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.05), of = F1.class)
                public class V1 implements F1 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.V2",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.12, delta = 0.05), of = F1.class)
                public class V2 implements F1 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Overlapping");
        assertThat(compilation).hadErrorContaining("V1");
        assertThat(compilation).hadErrorContaining("V2");
    }

    @Test
    void acceptsNonOverlappingCloseToRanges() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.F2",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "f2", type = FeatureType.DOUBLE)
                public interface F2 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.Far1",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.1, delta = 0.01), of = F2.class)
                public class Far1 implements F2 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.Far2",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.5, delta = 0.01), of = F2.class)
                public class Far2 implements F2 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).succeeded();
    }

    @Test
    void rejectsOverlappingCloseToWithinSameVariantArray() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.F3",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "f3", type = FeatureType.DOUBLE)
                public interface F3 {}
                """);
        JavaFileObject variant = JavaFileObjects.forSourceString("com.example.IntraV",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = {
                    @CloseTo(value = 0.1, delta = 0.05),
                    @CloseTo(value = 0.12, delta = 0.05)
                }, of = F3.class)
                public class IntraV implements F3 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Overlapping");
    }

    @Test
    void acceptsNonOverlappingCloseToWithinSameArray() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.F4",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "f4", type = FeatureType.DOUBLE)
                public interface F4 {}
                """);
        JavaFileObject variant = JavaFileObjects.forSourceString("com.example.OkV",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = {
                    @CloseTo(value = 0.1, delta = 0.01),
                    @CloseTo(value = 0.5, delta = 0.01)
                }, of = F4.class)
                public class OkV implements F4 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, variant);

        assertThat(compilation).succeeded();
    }

    @Test
    void exactBoundaryDoesNotOverlap() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.F5",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "f5", type = FeatureType.DOUBLE)
                public interface F5 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.Edge1",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 0.0, delta = 0.5), of = F5.class)
                public class Edge1 implements F5 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.Edge2",
                """
                package com.example;
                import com.flagzen.Variant;
                import com.flagzen.CloseTo;
                @Variant(doubleValue = @CloseTo(value = 1.0, delta = 0.5), of = F5.class)
                public class Edge2 implements F5 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).succeeded();
    }
}
