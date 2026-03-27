package com.flagzen.acceptance;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-02: Block-scoped context applies to multiple resolve calls.
 *
 * Scenario: Developer scopes evaluation context to a block of code.
 * Port-to-port: FlagContext.run() (driving port) -> FlagProvider (driven port).
 */
class BlockScopedContextAcceptanceTest {

    @Test
    void multiplResolveCallsInsideBlockShareSameContext() {
        // Given: a flag provider that resolves based on targeting key
        ContextCapturingFlagProvider flagProvider = new ContextCapturingFlagProvider();

        // And: the dispatcher is configured with this provider
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // And: an evaluation context for a VIP user
        EvaluationContext context = EvaluationContext.builder()
                .targetingKey("user-vip-42")
                .attribute("plan", "enterprise")
                .build();

        // When: the developer wraps multiple resolve calls inside a FlagContext scoped block
        FlagContext.run(context, () -> {
            // First resolve call
            CheckoutFlow first = dispatcher.resolve(CheckoutFlow.class);
            first.execute();

            // Second resolve call within the same block
            CheckoutFlow second = dispatcher.resolve(CheckoutFlow.class);
            second.execute();
        });

        // Then: both resolve calls used the same evaluation context
        assertThat(flagProvider.callCount()).isEqualTo(2);
        assertThat(flagProvider.contextAtCall(0).targetingKey()).isEqualTo("user-vip-42");
        assertThat(flagProvider.contextAtCall(1).targetingKey()).isEqualTo("user-vip-42");
        assertThat(flagProvider.contextAtCall(0)).isSameAs(flagProvider.contextAtCall(1));

        // And: the context is cleared after the block exits
        assertThat(FlagContext.current()).isNull();
    }

    /**
     * Test fixture: a flag provider that captures every evaluation context it receives.
     */
    private static class ContextCapturingFlagProvider implements FlagProvider {

        private final java.util.List<EvaluationContext> capturedContexts = new java.util.ArrayList<>();

        @Override
        public Optional<String> getString(String key) {
            return Optional.of("CLASSIC");
        }

        @Override
        public Optional<String> getString(String key, EvaluationContext context) {
            capturedContexts.add(context);
            return Optional.of("STREAMLINED");
        }

        int callCount() {
            return capturedContexts.size();
        }

        EvaluationContext contextAtCall(int index) {
            return capturedContexts.get(index);
        }
    }
}
