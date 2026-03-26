package com.flagzen.test;

import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.FeatureMetadata;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Set<Class<?>> FEATURE_TYPES = discoverFeatureTypes();

    /**
     * Checks whether the given type is a feature interface with registered metadata.
     * Used to determine if a test parameter can be resolved as a feature proxy.
     */
    public static boolean isFeatureType(Class<?> type) {
        return FEATURE_TYPES.contains(type);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == TestFlagContext.class || isFeatureType(type);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        TestFlagContext context = extensionContext.getStore(NAMESPACE).get(CONTEXT_KEY, TestFlagContext.class);
        if (type == TestFlagContext.class) {
            return context;
        }
        return context.resolve(type);
    }

    @SuppressWarnings("rawtypes")
    private static Set<Class<?>> discoverFeatureTypes() {
        Set<Class<?>> types = ConcurrentHashMap.newKeySet();
        for (FeatureMetadata metadata : ServiceLoader.load(FeatureMetadata.class)) {
            types.add(metadata.featureType());
        }
        return types;
    }

    private void applyPinFlags(Method method, TestFlagContext testFlagContext) {
        PinFlag[] pinFlags = method.getAnnotationsByType(PinFlag.class);
        for (PinFlag pinFlag : pinFlags) {
            testFlagContext.pin(pinFlag.feature(), pinFlag.variant());
        }
    }
}
