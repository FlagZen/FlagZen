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
 * Unit test: @Feature interface without inner Variant enum accepts any string as variant value.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation result + generated metadata.
 * Test Budget: 2 behaviors x 2 = 4 max. Using 1 test.
 */
class FreeFormVariantTest {

    @Test
    void compilesSuccessfullyWithFreeFormVariantValueWhenNoVariantEnumExists() throws IOException {
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.DarkMode",
                """
                package com.example;

                import com.flagzen.Feature;

                @Feature("dark-mode")
                public interface DarkMode {
                }
                """
        );

        JavaFileObject variant = JavaFileObjects.forSourceString(
                "com.example.DarkModeOn",
                """
                package com.example;

                import com.flagzen.Variant;

                @Variant(value = "on", of = DarkMode.class)
                public class DarkModeOn implements DarkMode {
                }
                """
        );

        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, variant);

        // Behavior 1: compilation succeeds without inner Variant enum
        assertThat(compilation).succeeded();

        // Behavior 2: free-form value "on" is accepted and recorded in generated metadata
        Optional<JavaFileObject> metadataFile = compilation.generatedSourceFile(
                "com.example.DarkMode_FlagZenMetadata");
        assertThat(metadataFile).isPresent();
        String metadataSource = metadataFile.get().getCharContent(false).toString();
        assertThat(metadataSource).contains("\"on\"");
    }
}
