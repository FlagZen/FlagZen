package com.flagzen.test;

import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;

/**
 * JUnit 5 extension that reads {@link PinFlag} annotations and sets up
 * a test-scoped {@link TestFlagContext} with pinned flag values.
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;ExtendWith(FlagZenExtension.class)
 * class MyTest {
 *     &#64;Test
 *     &#64;PinFlag(feature = "checkout-flow", variant = "PREMIUM")
 *     void testPremiumCheckout(TestFlagContext flags) {
 *         CheckoutFlow flow = flags.resolve(CheckoutFlow.class);
 *         // flow delegates to PremiumCheckout
 *     }
 * }
 * </pre>
 */
public class FlagZenExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(FlagZenExtension.class);

    private static final String CONTEXT_KEY = "testFlagContext";

    @Override
    public void beforeEach(ExtensionContext context) {
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        TestFlagContext testFlagContext = new TestFlagContext(flagProvider);

        context.getTestMethod().ifPresent(method -> applyPinFlags(method, testFlagContext));

        context.getStore(NAMESPACE).put(CONTEXT_KEY, testFlagContext);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        context.getStore(NAMESPACE).remove(CONTEXT_KEY);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == TestFlagContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(CONTEXT_KEY, TestFlagContext.class);
    }

    private void applyPinFlags(Method method, TestFlagContext testFlagContext) {
        PinFlag[] pinFlags = method.getAnnotationsByType(PinFlag.class);
        for (PinFlag pinFlag : pinFlags) {
            testFlagContext.pin(pinFlag.feature(), pinFlag.variant());
        }
    }
}
