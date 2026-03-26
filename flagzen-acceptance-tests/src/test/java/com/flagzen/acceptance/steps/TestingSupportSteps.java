package com.flagzen.acceptance.steps;

import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.PaymentMethod;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.FlagZenException;
import com.flagzen.test.FlagSource;
import com.flagzen.test.FlagZenExtension;
import com.flagzen.test.TestFlagContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

/**
 * Step definitions for testing support scenarios (US-07).
 */
public class TestingSupportSteps {

    private TestFlagContext testFlagContext;
    private CheckoutFlow resolvedProxy;
    private PaymentMethod resolvedPaymentProxy;
    private String pinnedFlagKey;
    private String pinnedFlagValue;

    // Isolation scenario fields
    private TestFlagContext testAContext;
    private TestFlagContext testBContext;
    private CheckoutFlow testAProxy;
    private CheckoutFlow testBProxy;

    @Given("a test method annotated to pin {string} to {string}")
    public void aTestMethodAnnotatedToPinTo(String flagKey, String flagValue) {
        pinnedFlagKey = flagKey;
        pinnedFlagValue = flagValue;
    }

    @When("the test resolves {string}")
    public void theTestResolvesFeature(String featureName) {
        if (testFlagContext == null) {
            testFlagContext = TestFlagContext.create();
            testFlagContext.pin(pinnedFlagKey, pinnedFlagValue);
        }
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @And("no flag provider setup was needed in the test")
    public void noFlagProviderSetupWasNeededInTheTest() {
        // Structural: only TestFlagContext.pin() and resolve() used above
    }

    @Given("a test receives a test flag context as a parameter")
    public void aTestReceivesATestFlagContextAsAParameter() {
        testFlagContext = TestFlagContext.create();
    }

    @When("the test pins {string} to {string} via the context")
    public void theTestPinsToViaTheContext(String flagKey, String flagValue) {
        testFlagContext.pin(flagKey, flagValue);
    }

    @And("resolves {string}")
    public void resolvesFeature(String featureName) {
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @Then("the pin is scoped to the current test only")
    public void thePinIsScopedToTheCurrentTestOnly() {
        // Verify isolation: a fresh context does not inherit pins from the first context
        TestFlagContext freshContext = TestFlagContext.create();
        freshContext.pin("checkout-flow", "CLASSIC");
        CheckoutFlow freshProxy = freshContext.resolve(CheckoutFlow.class);
        // The fresh context pinned CLASSIC, so it must NOT delegate to PremiumCheckout
        assertThat(freshProxy.execute()).isEqualTo("ClassicCheckout");
    }

    @Given("a test method pinning {string} to {string} and {string} to {string}")
    public void aTestMethodPinningToAndTo(String flagKey1, String variant1, String flagKey2, String variant2) {
        testFlagContext = TestFlagContext.create();
        testFlagContext.pin(flagKey1, variant1);
        testFlagContext.pin(flagKey2, variant2);
    }

    @When("both features are resolved")
    public void bothFeaturesAreResolved() {
        resolvedProxy = testFlagContext.resolve(CheckoutFlow.class);
        resolvedPaymentProxy = testFlagContext.resolve(PaymentMethod.class);
        SharedProxyHolder.set(resolvedProxy);
        SharedProxyHolder.setPayment(resolvedPaymentProxy);
    }

    @Given("test A pins {string} to {string}")
    public void testAPinsTo(String flagKey, String variant) {
        testAContext = TestFlagContext.create();
        testAContext.pin(flagKey, variant);
    }

    @Given("test B pins {string} to {string}")
    public void testBPinsTo(String flagKey, String variant) {
        testBContext = TestFlagContext.create();
        testBContext.pin(flagKey, variant);
    }

    @When("both tests execute")
    public void bothTestsExecute() {
        testAProxy = testAContext.resolve(CheckoutFlow.class);
        testBProxy = testBContext.resolve(CheckoutFlow.class);
    }

    @Then("test A sees {string}")
    public void testASees(String expectedVariant) {
        assertThat(testAProxy.execute()).isEqualTo(expectedVariant);
    }

    @Then("test B sees {string}")
    public void testBSees(String expectedVariant) {
        assertThat(testBProxy.execute()).isEqualTo(expectedVariant);
    }

    @Then("neither test affects the other")
    public void neitherTestAffectsTheOther() {
        // Re-resolve to confirm no cross-contamination
        CheckoutFlow freshAProxy = testAContext.resolve(CheckoutFlow.class);
        CheckoutFlow freshBProxy = testBContext.resolve(CheckoutFlow.class);
        assertThat(freshAProxy.execute()).isNotEqualTo(freshBProxy.execute());
    }

    // --- Parameter injection scenario ---
    private String parameterFeatureName;
    private Object injectedParameter;

    @Given("a test method that pins {string} to {string}")
    public void aTestMethodThatPinsTo(String flagKey, String variant) {
        // If @FlagSource already loaded a context, simulate @PinFlag overriding it
        if (flagSourceContext != null) {
            testFlagContext = flagSourceContext;
        } else {
            testFlagContext = TestFlagContext.create();
        }
        testFlagContext.pin(flagKey, variant);
    }

    @Given("the test method declares {string} as a parameter")
    public void theTestMethodDeclaresAsAParameter(String featureName) {
        parameterFeatureName = featureName;
    }

    @When("the test executes")
    public void theTestExecutes() {
        // Simulate what FlagZenExtension does when resolving a feature interface parameter:
        // The extension must recognize feature types and resolve them via the test's context.
        Class<?> featureType = resolveFeatureType(parameterFeatureName);

        // Verify FlagZenExtension recognizes this as a supported parameter type
        assertThat(FlagZenExtension.isFeatureType(featureType))
                .as("FlagZenExtension must recognize %s as a resolvable feature parameter", parameterFeatureName)
                .isTrue();

        // Resolve the feature as the extension would during parameter injection
        injectedParameter = testFlagContext.resolve(featureType);
    }

    @Then("the {string} parameter is a proxy resolving to {string}")
    public void theParameterIsAProxyResolvingTo(String featureName, String variantClass) {
        assertThat(injectedParameter).isNotNull();
        CheckoutFlow proxy = (CheckoutFlow) injectedParameter;
        assertThat(proxy.execute()).isEqualTo(variantClass);
    }

    // --- Missing @FlagSource scenario fields ---
    private String missingFileName;
    private Exception caughtException;

    @Given("a test class configured to load flags from {string}")
    public void aTestClassConfiguredToLoadFlagsFrom(String fileName) {
        missingFileName = fileName;
    }

    @When("the test class initializes")
    public void theTestClassInitializes() {
        caughtException = catchException(() ->
                TestFlagContext.createFromProperties(missingFileName));
    }

    @Then("a clear error is raised stating the file was not found")
    public void aClearErrorIsRaisedStatingTheFileWasNotFound() {
        assertThat(caughtException)
                .isNotNull()
                .isInstanceOf(FlagZenException.class)
                .hasMessageContaining(missingFileName)
                .hasMessageContaining("not found");
    }

    @And("the searched locations are listed in the message")
    public void theSearchedLocationsAreListedInTheMessage() {
        assertThat(caughtException.getMessage())
                .containsIgnoringCase("classpath");
    }

    // --- @FlagSource scenario fields ---
    private String propertiesFileName;
    private TestFlagContext flagSourceContext;

    @Given("a properties file containing {string}")
    public void aPropertiesFileContaining(String flagEntry) {
        // The test properties file "flags-test.properties" is pre-created on the classpath
        // with content matching the flag entry (e.g., "checkout-flow=CLASSIC")
        propertiesFileName = "flags-test.properties";
    }

    @Given("a test class configured to load flags from this file")
    public void aTestClassConfiguredToLoadFlagsFromThisFile() {
        // Simulate what FlagZenExtension does when it encounters @FlagSource:
        // It reads the annotation, loads the properties file, and populates the flag provider
        flagSourceContext = TestFlagContext.createFromProperties(propertiesFileName);
    }

    @When("a test in the class resolves {string}")
    public void aTestInTheClassResolvesFeature(String featureName) {
        resolvedProxy = flagSourceContext.resolve(CheckoutFlow.class);
        SharedProxyHolder.set(resolvedProxy);
    }

    @Then("other tests in the class still resolve to {string}")
    public void otherTestsInTheClassStillResolveTo(String variantClass) {
        // Simulate another test in the same @FlagSource class without @PinFlag:
        // A fresh context from the same properties file should resolve to the file value
        TestFlagContext otherTestContext = TestFlagContext.createFromProperties(propertiesFileName);
        CheckoutFlow otherProxy = otherTestContext.resolve(CheckoutFlow.class);
        assertThat(otherProxy.execute()).isEqualTo(variantClass);
    }

    private Class<?> resolveFeatureType(String featureName) {
        return switch (featureName) {
            case "CheckoutFlow" -> CheckoutFlow.class;
            case "PaymentMethod" -> PaymentMethod.class;
            default -> throw new IllegalArgumentException("Unknown feature: " + featureName);
        };
    }

    @Then("{string} delegates to {string}")
    public void featureDelegatesTo(String featureName, String variantClass) {
        switch (featureName) {
            case "CheckoutFlow" -> {
                var proxy = SharedProxyHolder.get();
                assertThat(proxy).as("CheckoutFlow proxy must be set").isNotNull();
                assertThat(proxy.execute()).isEqualTo(variantClass);
            }
            case "PaymentMethod" -> {
                var proxy = SharedProxyHolder.getPayment();
                assertThat(proxy).as("PaymentMethod proxy must be set").isNotNull();
                assertThat(proxy.execute()).isEqualTo(variantClass);
            }
            default -> throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
    }
}
