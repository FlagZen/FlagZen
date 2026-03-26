package com.flagzen.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Unit test: @Feature applied to a class produces a compile error.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation diagnostics (driven port output).
 * Test Budget: 1 behavior x 2 = 2 max. Using 1 test.
 */
class FeatureTargetValidationTest {

    @Test
    void emitsCompileErrorWhenFeatureAnnotationAppliedToClass() {
        JavaFileObject classWithFeature = JavaFileObjects.forSourceString(
                "com.example.CheckoutService",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("checkout-flow")
                public class CheckoutService {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(classWithFeature);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Feature can only be applied to interfaces");
    }
}
