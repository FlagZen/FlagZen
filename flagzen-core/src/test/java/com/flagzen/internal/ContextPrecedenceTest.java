package com.flagzen.internal;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.spi.ContextAccessor;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for context resolution order in DefaultFeatureDispatcher.
 * Resolution order: explicit > accessor > scoped > default.
 *
 * Test Budget: 2 behaviors x 2 = 4 max unit tests.
 */
class ContextPrecedenceTest {

    @AfterEach
    void tearDown() {
        FlagContext.clear();
    }

    /**
     * Behavior 1: When explicit context is provided alongside an accessor,
     * the explicit context is used and the accessor is not consulted.
     */
    @Test
    void explicitContextWinsOverAccessorAndScopedContext() {
        // Given: a flag provider that captures the context it receives
        AtomicReference<EvaluationContext> capturedContext = new AtomicReference<>();
        FlagProvider provider = contextCapturingProvider(capturedContext);

        // And: an accessor that would provide a different context
        boolean[] accessorCalled = {false};
        ContextAccessor accessor = new ContextAccessor() {
            @Override
            public Optional<EvaluationContext> getContext() {
                accessorCalled[0] = true;
                return Optional.of(EvaluationContext.builder()
                        .targetingKey("accessor-user").build());
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider, accessor);

        // And: a scoped context is active
        FlagContext.set(EvaluationContext.builder()
                .targetingKey("scoped-user").build());

        // And: an explicit context
        EvaluationContext explicit = EvaluationContext.builder()
                .targetingKey("explicit-user").build();

        // When: resolve with explicit context
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class, explicit);
        proxy.execute();

        // Then: explicit context was used
        assertThat(capturedContext.get().targetingKey()).isEqualTo("explicit-user");

        // And: accessor was not consulted
        assertThat(accessorCalled[0]).isFalse();
    }

    /**
     * Behavior 2: When no explicit context is provided, the accessor's context
     * takes precedence over the scoped context.
     */
    @Test
    void accessorContextWinsOverScopedContextWhenNoExplicitContext() {
        // Given: a flag provider that captures the context it receives
        AtomicReference<EvaluationContext> capturedContext = new AtomicReference<>();
        FlagProvider provider = contextCapturingProvider(capturedContext);

        // And: an accessor that provides context
        ContextAccessor accessor = new ContextAccessor() {
            @Override
            public Optional<EvaluationContext> getContext() {
                return Optional.of(EvaluationContext.builder()
                        .targetingKey("accessor-user").build());
            }

            @Override
            public int priority() {
                return 0;
            }
        };

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider, accessor);

        // And: a scoped context is active
        FlagContext.set(EvaluationContext.builder()
                .targetingKey("scoped-user").build());

        // When: resolve WITHOUT explicit context
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        proxy.execute();

        // Then: accessor context was used (not scoped)
        assertThat(capturedContext.get().targetingKey()).isEqualTo("accessor-user");
    }

    private static FlagProvider contextCapturingProvider(AtomicReference<EvaluationContext> captured) {
        return new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext context) {
                captured.set(context);
                return Optional.of("CLASSIC");
            }
        };
    }
}
