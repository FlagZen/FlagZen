package com.flagzen.acceptance;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.spi.ContextAccessor;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-03: Explicit context takes precedence over all other sources.
 *
 * Scenario: When explicit context, scoped context, and a context accessor are all active,
 * explicit context wins and the accessor is not consulted.
 *
 * Port-to-port: FeatureDispatcher.resolve(Class, EvaluationContext) (driving port) -> FlagProvider (driven port).
 */
class ContextPrecedenceAcceptanceTest {

    @AfterEach
    void tearDown() {
        FlagContext.clear();
    }

    @Test
    void explicitContextTakesPrecedenceOverAllOtherContextSources() {
        // Given: a flag provider that resolves variant based on targeting key
        TargetingKeyFlagProvider flagProvider = new TargetingKeyFlagProvider();

        // And: a context accessor that would return "accessor-user" context
        SpyContextAccessor accessor = new SpyContextAccessor(
                EvaluationContext.builder().targetingKey("accessor-user").build()
        );

        // And: the dispatcher is configured with the flag provider and context accessor
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider, accessor);

        // And: a scoped context for "scoped-user" is active
        FlagContext.set(EvaluationContext.builder()
                .targetingKey("scoped-user")
                .build());

        // Precondition: verify accessor IS wired by resolving WITHOUT explicit context
        // The accessor should be consulted and its context used (accessor > scoped in priority)
        CheckoutFlow proxyWithoutExplicit = dispatcher.resolve(CheckoutFlow.class);
        proxyWithoutExplicit.execute();
        assertThat(accessor.wasConsulted())
                .as("Accessor must be consulted when no explicit context is provided")
                .isTrue();
        assertThat(flagProvider.lastReceivedTargetingKey())
                .as("Accessor context should be used when no explicit context is provided")
                .isEqualTo("accessor-user");

        // Reset spy state for the main assertion
        accessor.reset();
        flagProvider.reset();

        // And: an explicit context for "explicit-user"
        EvaluationContext explicitContext = EvaluationContext.builder()
                .targetingKey("explicit-user")
                .build();

        // When: the developer resolves with explicit context
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, explicitContext);
        String result = proxy.execute();

        // Then: the flag provider received the explicit context (not scoped, not accessor)
        assertThat(flagProvider.lastReceivedTargetingKey()).isEqualTo("explicit-user");

        // And: the context accessor was NOT consulted
        assertThat(accessor.wasConsulted()).isFalse();

        // And: the result reflects the explicit context resolution
        assertThat(result).isEqualTo("StreamlinedCheckout");
    }

    /**
     * Flag provider that resolves based on targeting key and captures what it received.
     */
    private static class TargetingKeyFlagProvider implements FlagProvider {

        private String lastTargetingKey;

        @Override
        public Optional<String> getString(String key) {
            return Optional.of("CLASSIC");
        }

        @Override
        public Optional<String> getString(String key, EvaluationContext context) {
            this.lastTargetingKey = context != null ? context.targetingKey() : null;
            if ("explicit-user".equals(lastTargetingKey)) {
                return Optional.of("STREAMLINED");
            }
            if ("accessor-user".equals(lastTargetingKey)) {
                return Optional.of("STREAMLINED");
            }
            return Optional.of("CLASSIC");
        }

        String lastReceivedTargetingKey() {
            return lastTargetingKey;
        }

        void reset() {
            lastTargetingKey = null;
        }
    }

    /**
     * Spy context accessor that tracks whether it was consulted.
     */
    private static class SpyContextAccessor implements ContextAccessor {

        private final EvaluationContext context;
        private boolean consulted = false;

        SpyContextAccessor(EvaluationContext context) {
            this.context = context;
        }

        @Override
        public Optional<EvaluationContext> getContext() {
            this.consulted = true;
            return Optional.ofNullable(context);
        }

        @Override
        public int priority() {
            return 0;
        }

        boolean wasConsulted() {
            return consulted;
        }

        void reset() {
            this.consulted = false;
        }
    }
}
