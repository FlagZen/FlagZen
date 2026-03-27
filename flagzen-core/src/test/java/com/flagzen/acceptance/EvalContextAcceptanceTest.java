package com.flagzen.acceptance;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-01: Explicit evaluation context passed through flag resolution.
 *
 * Scenario: Developer resolves a feature with per-user evaluation context.
 * Port-to-port: FeatureDispatcher.resolve(Class, EvaluationContext) (driving port) -> FlagProvider (driven port).
 */
class EvalContextAcceptanceTest {

    @Test
    void resolvesFeatureWithPerUserEvaluationContext() {
        // Given: a feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
        // (provided by CheckoutFlowMetadata registered via ServiceLoader)

        // And: a flag provider that returns "STREAMLINED" when targeting key is "user-vip-42"
        ContextCapturingFlagProvider flagProvider = new ContextCapturingFlagProvider("user-vip-42", "STREAMLINED");

        // And: the dispatcher is configured with this provider
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // And: an evaluation context with targeting key "user-vip-42" and attribute "plan" = "enterprise"
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-vip-42")
                .attribute("plan", "enterprise")
                .build();

        // When: the developer resolves "CheckoutFlow" with that evaluation context
        CheckoutFlow checkoutFlow = dispatcher.resolve(CheckoutFlow.class, context);

        // And: calls "execute" on the resolved proxy
        String result = checkoutFlow.execute();

        // Then: the resolved proxy dispatches to the "STREAMLINED" variant
        assertThat(result).isEqualTo("StreamlinedCheckout");

        // And: the flag provider received the evaluation context with targeting key "user-vip-42"
        assertThat(flagProvider.lastReceivedContext()).isNotNull();
        assertThat(flagProvider.lastReceivedContext().targetingKey()).isEqualTo("user-vip-42");
    }

    /**
     * Test fixture: a flag provider that resolves based on targeting key
     * and captures the received evaluation context for assertion.
     */
    private static class ContextCapturingFlagProvider implements FlagProvider {

        private final String expectedTargetingKey;
        private final String variantForKey;
        private EvaluationContext lastContext;

        ContextCapturingFlagProvider(String expectedTargetingKey, String variantForKey) {
            this.expectedTargetingKey = expectedTargetingKey;
            this.variantForKey = variantForKey;
        }

        @Override
        public Optional<String> getString(String key) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getString(String key, EvaluationContext context) {
            this.lastContext = context;
            if (context != null && expectedTargetingKey.equals(context.targetingKey())) {
                return Optional.of(variantForKey);
            }
            return Optional.empty();
        }

        EvaluationContext lastReceivedContext() {
            return lastContext;
        }
    }
}
