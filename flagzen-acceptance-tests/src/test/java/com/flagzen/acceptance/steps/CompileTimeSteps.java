package com.flagzen.acceptance.steps;

import com.flagzen.processor.FlagZenProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Step definitions for compile-time validation scenarios.
 * Port-to-port: Java compiler (driving port) -> FlagZenProcessor -> compilation result (driven port output).
 */
public class CompileTimeSteps {

    private static final String PACKAGE = "com.example";

    private String featureInterfaceName;
    private String flagKey;
    private String methodName;
    private boolean hasVariantEnum = true;
    private final List<JavaFileObject> sourceFiles = new ArrayList<>();
    private final List<String> variantNames = new ArrayList<>();
    private final Set<String> featureSourcesAdded = new HashSet<>();
    private Compilation compilation;

    @Given("a feature interface {string} with flag key {string}")
    public void aFeatureInterfaceWithFlagKey(String interfaceName, String key) {
        this.featureInterfaceName = interfaceName;
        this.flagKey = key;
    }

    @And("no inner Variant enum is defined on {string}")
    public void noInnerVariantEnumIsDefinedOn(String interfaceName) {
        this.hasVariantEnum = false;
    }

    @And("a method {string} declared on {string}")
    public void aMethodDeclaredOn(String method, String interfaceName) {
        this.methodName = method;
        featureSourcesAdded.add(interfaceName);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + interfaceName,
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("%s")
                public interface %s {
                    void %s();
                }
                """.formatted(PACKAGE, flagKey, interfaceName, method)
        ));
    }

    @And("a variant {string} implementing {string} for value {string}")
    public void aVariantImplementingForValue(String variantClass, String interfaceName, String value) {
        ensureFeatureSourceExists(interfaceName);
        variantNames.add(variantClass);
        if (methodName != null) {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + variantClass,
                    """
                    package %s;

                    import com.flagzen.Variant;

                    @Variant(value = "%s", of = %s.class)
                    public class %s implements %s {
                        @Override
                        public void %s() {}
                    }
                    """.formatted(PACKAGE, value, interfaceName, variantClass, interfaceName, methodName)
            ));
        } else {
            sourceFiles.add(JavaFileObjects.forSourceString(
                    PACKAGE + "." + variantClass,
                    """
                    package %s;

                    import com.flagzen.Variant;

                    @Variant(value = "%s", of = %s.class)
                    public class %s implements %s {
                    }
                    """.formatted(PACKAGE, value, interfaceName, variantClass, interfaceName)
            ));
        }
    }

    @When("the project compiles")
    public void theProjectCompiles() {
        compilation = javac()
                .withProcessors(new FlagZenProcessor())
                .compile(sourceFiles.toArray(new JavaFileObject[0]));
    }

    @Then("compilation succeeds")
    public void compilationSucceeds() {
        assertThat(compilation).succeeded();
    }

    @And("{string} is accepted as a valid variant value")
    public void isAcceptedAsAValidVariantValue(String value) {
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + featureInterfaceName + "_FlagZenMetadata")
                .contentsAsUtf8String()
                .contains("\"" + value + "\"");
    }

    @And("a dispatch proxy {string} is generated")
    public void aDispatchProxyIsGenerated(String proxyName) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + proxyName)
                .isNotNull();
    }

    @And("the proxy implements the {string} interface")
    public void theProxyImplementsTheInterface(String interfaceName) {
        assertThat(compilation)
                .generatedSourceFile(PACKAGE + "." + interfaceName + "_FlagZenProxy")
                .contentsAsUtf8String()
                .contains("implements " + interfaceName);
    }

    @Given("a class {string} annotated as a feature with key {string}")
    public void aClassAnnotatedAsAFeatureWithKey(String className, String key) {
        featureSourcesAdded.add(className);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + className,
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("%s")
                public class %s {
                }
                """.formatted(PACKAGE, key, className)
        ));
    }

    @Then("compilation fails")
    public void compilationFails() {
        assertThat(compilation).failed();
    }

    @And("the error message states {string}")
    public void theErrorMessageStates(String expectedMessage) {
        assertThat(compilation).hadErrorContaining(expectedMessage);
    }

    private void ensureFeatureSourceExists(String interfaceName) {
        if (featureSourcesAdded.contains(interfaceName)) {
            return;
        }
        featureSourcesAdded.add(interfaceName);
        sourceFiles.add(JavaFileObjects.forSourceString(
                PACKAGE + "." + interfaceName,
                """
                package %s;

                import com.flagzen.Feature;

                @Feature("%s")
                public interface %s {
                }
                """.formatted(PACKAGE, flagKey, interfaceName)
        ));
    }
}
