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
import java.util.List;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Step definitions for walking skeleton scenario 1:
 * "Developer defines a feature with variants and a dispatch proxy is generated"
 */
public class CompileTimeSteps {

    private static final String PACKAGE = "com.example";

    private String featureInterfaceName;
    private String flagKey;
    private String methodName;
    private final List<JavaFileObject> sourceFiles = new ArrayList<>();
    private final List<String> variantNames = new ArrayList<>();
    private Compilation compilation;

    @Given("a feature interface {string} with flag key {string}")
    public void aFeatureInterfaceWithFlagKey(String interfaceName, String key) {
        this.featureInterfaceName = interfaceName;
        this.flagKey = key;
    }

    @And("a method {string} declared on {string}")
    public void aMethodDeclaredOn(String method, String interfaceName) {
        this.methodName = method;
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
        variantNames.add(variantClass);
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
}
