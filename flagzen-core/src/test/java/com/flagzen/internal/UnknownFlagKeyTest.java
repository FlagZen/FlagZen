package com.flagzen.internal;

import com.flagzen.FeatureDispatcher;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DefaultFeatureDispatcher through the FeatureDispatcher driving port.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: When flag provider returns no value (empty) for a flag key
 * and the feature uses EXCEPTION fallback, an UnmatchedVariantException
 * is thrown with a message indicating no flag value was found.
 */
class UnknownFlagKeyTest {

    @Test
    void throwsWithNoFlagValueMessageWhenProviderReturnsEmpty() {
        // Given: provider with no flags configured (getString returns Optional.empty)
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve and call method on proxy
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);

        // Then: throws UnmatchedVariantException indicating no flag value found
        assertThatThrownBy(proxy::execute)
                .isInstanceOf(UnmatchedVariantException.class)
                .hasMessageContaining("No flag value")
                .hasMessageContaining("checkout-flow");
    }
}
