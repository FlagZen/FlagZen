package com.flagzen.internal;

import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.acceptance.fixtures.CheckoutFlowMetadata;
import com.flagzen.acceptance.fixtures.DefaultCheckout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for default variant priority through the FeatureDispatcher driving port.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: When a flag value has no matching variant but a default variant exists,
 * the default handles the call regardless of the fallback strategy.
 */
class DefaultVariantPriorityTest {

    @AfterEach
    void resetMetadata() {
        CheckoutFlowMetadata.reset();
    }

    @Test
    void defaultVariantHandlesUnmatchedValueInsteadOfThrowingException() {
        // Given: default variant registered and unmatched flag value
        CheckoutFlowMetadata.setDefaultVariant(DefaultCheckout::new);
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "BETA");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve and call method
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        String result = proxy.execute();

        // Then: default variant handles the call, no exception thrown
        assertThat(result).isEqualTo("DefaultCheckout");
        assertThatCode(proxy::execute).doesNotThrowAnyException();
    }
}
