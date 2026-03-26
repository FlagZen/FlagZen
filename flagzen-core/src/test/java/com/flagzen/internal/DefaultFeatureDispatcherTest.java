package com.flagzen.internal;

import com.flagzen.FallbackStrategy;
import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagZenException;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.ClassicCheckout;
import com.flagzen.acceptance.fixtures.StreamlinedCheckout;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DefaultFeatureDispatcher through the FeatureDispatcher driving port.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests.
 *
 * Uses CheckoutFlow fixtures with hand-crafted metadata registered via ServiceLoader.
 */
class DefaultFeatureDispatcherTest {

    /**
     * Behavior 1: Proxy delegates to the variant matching the current flag value.
     * Parametrized: different flag values resolve to different variants.
     */
    @ParameterizedTest
    @CsvSource({
            "CLASSIC,ClassicCheckout",
            "STREAMLINED,StreamlinedCheckout"
    })
    void delegatesToMatchingVariant(String flagValue, String expectedResult) {
        // Given: provider with a flag value
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", flagValue);

        // And: dispatcher configured with this provider
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve and call method
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        String result = proxy.execute();

        // Then: delegates to matching variant
        assertThat(result).isEqualTo(expectedResult);
    }

    /**
     * Behavior 2: Proxy re-evaluates flag on every method call (dynamic dispatch).
     * Changing the flag value between calls should change which variant handles the call.
     */
    @Test
    void reEvaluatesFlagOnEveryMethodCall() {
        // Given: provider initially set to CLASSIC
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "CLASSIC");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve proxy once
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        assertThat(proxy.execute()).isEqualTo("ClassicCheckout");

        // And: change flag value
        provider.set("checkout-flow", "STREAMLINED");

        // Then: same proxy now delegates to different variant
        assertThat(proxy.execute()).isEqualTo("StreamlinedCheckout");
    }

    /**
     * Behavior 3: Throws UnmatchedVariantException when no variant matches
     * and fallback strategy is EXCEPTION.
     */
    @Test
    void throwsWhenNoVariantMatchesWithExceptionFallback() {
        // Given: provider with unknown flag value
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "UNKNOWN");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve and call method
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);

        // Then: throws UnmatchedVariantException
        assertThatThrownBy(proxy::execute)
                .isInstanceOf(UnmatchedVariantException.class)
                .hasMessageContaining("UNKNOWN")
                .hasMessageContaining("checkout-flow");
    }

    /**
     * Behavior 4: Dispatcher returns the same proxy instance for the same feature type
     * (singleton per feature per dispatcher).
     */
    @Test
    void returnsSameProxyInstanceForSameFeature() {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "CLASSIC");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        CheckoutFlow first = dispatcher.resolve(CheckoutFlow.class);
        CheckoutFlow second = dispatcher.resolve(CheckoutFlow.class);

        assertThat(first).isSameAs(second);
    }

    /**
     * Behavior 5: Dispatcher throws when no metadata found for feature type.
     */
    @Test
    void throwsWhenNoMetadataFoundForFeature() {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        assertThatThrownBy(() -> dispatcher.resolve(Runnable.class))
                .isInstanceOf(FlagZenException.class)
                .hasMessageContaining("Runnable");
    }
}
