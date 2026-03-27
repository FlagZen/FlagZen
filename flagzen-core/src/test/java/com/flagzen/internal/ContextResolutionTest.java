package com.flagzen.internal;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagContext;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.spi.ContextAccessor;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for context resolution order through the FeatureDispatcher driving port.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests.
 *
 * Behaviors:
 * 1. Default context is used as fallback when no other context source provides context
 * 2. Resolution order: explicit > accessor > scoped > default (parametrized)
 * 3. Empty accessor is skipped, next accessor consulted
 */
class ContextResolutionTest {

    @AfterEach
    void tearDown() {
        FlagContext.clear();
    }

    /**
     * Behavior 1: Default context is used when no other context source is available.
     */
    @Test
    void usesDefaultContextWhenNoOtherContextSourceAvailable() {
        // Given: a capturing flag provider and a default context
        AtomicReference<String> capturedKey = new AtomicReference<>();
        FlagProvider provider = capturingProvider(capturedKey);

        EvaluationContext defaultContext = EvaluationContext.builder()
                .targetingKey("default-user")
                .build();

        // And: dispatcher with default context, no accessors
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider, defaultContext);

        // And: no scoped context
        FlagContext.clear();

        // When: resolve without explicit context
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        proxy.execute();

        // Then: default context is used
        assertThat(capturedKey.get()).isEqualTo("default-user");
    }

    /**
     * Behavior 2: Resolution order is deterministic: explicit > accessor > scoped > default.
     * Parametrized over which sources are active to prove each level of precedence.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("resolutionOrderScenarios")
    void resolvesContextInDeterministicOrder(
            String scenario,
            EvaluationContext explicitCtx,
            ContextAccessor accessor,
            EvaluationContext scopedCtx,
            EvaluationContext defaultCtx,
            String expectedTargetingKey) {

        AtomicReference<String> capturedKey = new AtomicReference<>();
        FlagProvider provider = capturingProvider(capturedKey);

        // Build dispatcher with default context and optional accessor
        FeatureDispatcher dispatcher;
        if (accessor != null && defaultCtx != null) {
            dispatcher = new DefaultFeatureDispatcher(provider, defaultCtx, accessor);
        } else if (accessor != null) {
            dispatcher = new DefaultFeatureDispatcher(provider, accessor);
        } else if (defaultCtx != null) {
            dispatcher = new DefaultFeatureDispatcher(provider, defaultCtx);
        } else {
            dispatcher = new DefaultFeatureDispatcher(provider);
        }

        // Set scoped context if provided
        FlagContext.clear();
        if (scopedCtx != null) {
            FlagContext.set(scopedCtx);
        }

        // Resolve with or without explicit context
        CheckoutFlow proxy;
        if (explicitCtx != null) {
            proxy = dispatcher.resolve(CheckoutFlow.class, explicitCtx);
        } else {
            proxy = dispatcher.resolve(CheckoutFlow.class);
        }
        proxy.execute();

        assertThat(capturedKey.get()).isEqualTo(expectedTargetingKey);
    }

    static Stream<Arguments> resolutionOrderScenarios() {
        EvaluationContext explicit = ctx("explicit-user");
        EvaluationContext accessor = ctx("accessor-user");
        EvaluationContext scoped = ctx("scoped-user");
        EvaluationContext defaultCtx = ctx("default-user");

        ContextAccessor accessorSpi = stubAccessor(0, "accessor-user");

        return Stream.of(
                Arguments.of("explicit beats all", explicit, accessorSpi, scoped, defaultCtx, "explicit-user"),
                Arguments.of("accessor beats scoped and default", null, accessorSpi, scoped, defaultCtx, "accessor-user"),
                Arguments.of("scoped beats default", null, null, scoped, defaultCtx, "scoped-user"),
                Arguments.of("default is last resort", null, null, null, defaultCtx, "default-user")
        );
    }

    /**
     * Behavior 3: Accessor returning empty is skipped, next accessor with context wins.
     */
    @Test
    void skipsEmptyAccessorAndConsultsNext() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        FlagProvider provider = capturingProvider(capturedKey);

        ContextAccessor emptyAccessor = new ContextAccessor() {
            @Override
            public Optional<EvaluationContext> getContext() {
                return Optional.empty();
            }

            @Override
            public int priority() {
                return 50;
            }
        };
        ContextAccessor providingAccessor = stubAccessor(100, "fallback-user");

        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(
                provider, emptyAccessor, providingAccessor);

        FlagContext.clear();
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        proxy.execute();

        assertThat(capturedKey.get()).isEqualTo("fallback-user");
    }

    // --- Helpers ---

    private static EvaluationContext ctx(String targetingKey) {
        return EvaluationContext.builder().targetingKey(targetingKey).build();
    }

    private static ContextAccessor stubAccessor(int priority, String targetingKey) {
        return new ContextAccessor() {
            @Override
            public Optional<EvaluationContext> getContext() {
                return Optional.of(ctx(targetingKey));
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    private static FlagProvider capturingProvider(AtomicReference<String> capturedKey) {
        return new FlagProvider() {
            @Override
            public Optional<String> getString(String key) {
                capturedKey.set(null);
                return Optional.of("CLASSIC");
            }

            @Override
            public Optional<String> getString(String key, EvaluationContext ctx) {
                if (ctx != null && ctx.targetingKey() != null) {
                    capturedKey.set(ctx.targetingKey());
                    return Optional.of("CLASSIC");
                }
                return getString(key);
            }
        };
    }
}
