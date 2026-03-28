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
 * Acceptance test: Developer dispatches a boolean feature using convenience annotations.
 *
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> generated source (driven port).
 * Verifies that @Feature(type=BOOLEAN) with @WhenTrue/@WhenFalse compiles and generates
 * a proxy with Map&lt;Boolean, Supplier&lt;T&gt;&gt; variant map using getBoolean dispatch.
 */
class BooleanDispatchTest {

    @Test
    void compilesBooleanTypedFeatureWithWhenTrueWhenFalseAndGeneratesBooleanProxy() throws IOException {
        // Given: a feature interface with BOOLEAN type
        JavaFileObject featureInterface = JavaFileObjects.forSourceString(
                "com.example.DarkMode",
                """
                package com.example;

                import com.flagzen.Feature;
                import com.flagzen.FeatureType;

                @Feature(value = "dark-mode", type = FeatureType.BOOLEAN)
                public interface DarkMode {
                    String theme();
                }
                """
        );

        // And: variant classes using @WhenTrue and @WhenFalse convenience annotations
        JavaFileObject darkModeOn = JavaFileObjects.forSourceString(
                "com.example.DarkModeOn",
                """
                package com.example;

                import com.flagzen.WhenTrue;

                @WhenTrue(of = DarkMode.class)
                public class DarkModeOn implements DarkMode {
                    @Override
                    public String theme() { return "dark"; }
                }
                """
        );

        JavaFileObject darkModeOff = JavaFileObjects.forSourceString(
                "com.example.DarkModeOff",
                """
                package com.example;

                import com.flagzen.WhenFalse;

                @WhenFalse(of = DarkMode.class)
                public class DarkModeOff implements DarkMode {
                    @Override
                    public String theme() { return "light"; }
                }
                """
        );

        // When: the project compiles
        Compilation compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(featureInterface, darkModeOn, darkModeOff);

        // Then: compilation succeeds
        assertThat(compilation).succeeded();

        // And: a dispatch proxy is generated
        assertThat(compilation)
                .generatedSourceFile("com.example.DarkMode_FlagZenProxy")
                .isNotNull();

        // And: the proxy uses Boolean-keyed variant map with getBoolean dispatch
        Optional<JavaFileObject> proxyFile = compilation.generatedSourceFile(
                "com.example.DarkMode_FlagZenProxy");
        assertThat(proxyFile).isPresent();
        String proxySource = proxyFile.get().getCharContent(false).toString();

        assertThat(proxySource)
                .contains("Map<Boolean, Supplier<DarkMode>> variants")
                .contains("flagProvider.getBoolean(")
                .contains("public class DarkMode_FlagZenProxy implements DarkMode");

        // And: metadata is generated
        assertThat(compilation)
                .generatedSourceFile("com.example.DarkMode_FlagZenMetadata")
                .isNotNull();

        // And: metadata contains variant suppliers for both true and false
        Optional<JavaFileObject> metadataFile = compilation.generatedSourceFile(
                "com.example.DarkMode_FlagZenMetadata");
        assertThat(metadataFile).isPresent();
        String metadataSource = metadataFile.get().getCharContent(false).toString();

        assertThat(metadataSource)
                .contains("DarkModeOn::new")
                .contains("DarkModeOff::new");
    }
}
