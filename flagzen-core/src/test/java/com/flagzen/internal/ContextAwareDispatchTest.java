package com.flagzen.internal;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for context-aware dispatch through FeatureDispatcher driving port.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests.
 */
class ContextAwareDispatchTest {

    /**
     * Behavior 1: Context-aware resolve dispatches to the variant
     * determined by the flag provider using the evaluation context.
     */
    @ParameterizedTest
    @CsvSource({
            "user-vip-42,STREAMLINED,StreamlinedCheckout",
            "user-regular,CLASSIC,ClassicCheckout"
    })
    void dispatchesToVariantBasedOnContext(String targetingKey, String flagValue, String expectedResult) {
        // Given: a flag provider that resolves based on targeting key
        FlagProvider provider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.empty();
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                if (targetingKey.equals(context.targetingKey())) {
                    return Optional.of(flagValue);
                }
                return Optional.empty();
            }
        };

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        EvaluationContext context = EvaluationContext.builder()
                .targetingKey(targetingKey)
                .build();

        // When: resolve with context and call method
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, context);
        String result = proxy.execute();

        // Then: dispatches to the correct variant
        assertThat(result).isEqualTo(expectedResult);
    }

    /**
     * Behavior 2: The flag provider receives the evaluation context
     * including targeting key and attributes.
     */
    @Test
    void flagProviderReceivesEvaluationContext() {
        // Given: a flag provider that captures the received context
        AtomicReference<EvaluationContext> capturedContext = new AtomicReference<>();
        FlagProvider provider = new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                capturedContext.set(context);
                return Optional.of("CLASSIC");
            }
        };

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-7291")
                .attribute("plan", "enterprise")
                .attribute("region", "eu-west")
                .build();

        // When: resolve with context
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, context);
        proxy.execute();

        // Then: the flag provider received the full context
        EvaluationContext received = capturedContext.get();
        assertThat(received).isNotNull();
        assertThat(received.targetingKey()).isEqualTo("user-7291");
        assertThat(received.attributes()).containsEntry("plan", "enterprise");
        assertThat(received.attributes()).containsEntry("region", "eu-west");
    }

    /**
     * Behavior 3: Resolve without context (backward compat) still works
     * and flag provider receives no context.
     */
    @Test
    void resolveWithoutContextFallsBackToBasicGetString() {
        // Given: a provider that only implements getString(key)
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "CLASSIC");

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve without context (existing API)
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        String result = proxy.execute();

        // Then: still dispatches correctly
        assertThat(result).isEqualTo("ClassicCheckout");
    }
}
