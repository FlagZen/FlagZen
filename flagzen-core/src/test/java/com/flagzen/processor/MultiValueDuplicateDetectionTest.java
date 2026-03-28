package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests for duplicate detection across multi-value variant arrays.
 */
class MultiValueDuplicateDetectionTest {

    @Test
    void rejectsDuplicateStringAcrossArrays() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.Feat1",
                """
                package com.example;
                import com.flagzen.Feature;
                @Feature("feat1")
                public interface Feat1 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.A1",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = {"A", "B"}, of = Feat1.class)
                public class A1 implements Feat1 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.A2",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = {"B", "C"}, of = Feat1.class)
                public class A2 implements Feat1 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("B");
    }

    @Test
    void rejectsDuplicateIntAcrossArrays() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.Feat2",
                """
                package com.example;
                import com.flagzen.Feature;
                import com.flagzen.FeatureType;
                @Feature(value = "feat2", type = FeatureType.INT)
                public interface Feat2 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.I1",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(intValue = {1, 2}, of = Feat2.class)
                public class I1 implements Feat2 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.I2",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(intValue = {2, 3}, of = Feat2.class)
                public class I2 implements Feat2 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("2");
    }

    @Test
    void acceptsNonDuplicateMultiValueArrays() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.Feat3",
                """
                package com.example;
                import com.flagzen.Feature;
                @Feature("feat3")
                public interface Feat3 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.N1",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = {"A", "B"}, of = Feat3.class)
                public class N1 implements Feat3 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.N2",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = {"C", "D"}, of = Feat3.class)
                public class N2 implements Feat3 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).succeeded();
    }

    @Test
    void rejectsDuplicateBetweenArrayAndRepeatable() {
        JavaFileObject feature = JavaFileObjects.forSourceString("com.example.Feat4",
                """
                package com.example;
                import com.flagzen.Feature;
                @Feature("feat4")
                public interface Feat4 {}
                """);
        JavaFileObject v1 = JavaFileObjects.forSourceString("com.example.ArrV",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = {"A", "B"}, of = Feat4.class)
                public class ArrV implements Feat4 {}
                """);
        JavaFileObject v2 = JavaFileObjects.forSourceString("com.example.RepV",
                """
                package com.example;
                import com.flagzen.Variant;
                @Variant(value = "B", of = Feat4.class)
                public class RepV implements Feat4 {}
                """);

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(feature, v1, v2);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate");
        assertThat(compilation).hadErrorContaining("B");
    }
}
