package com.flagzen.internal;

import com.flagzen.FeatureDispatcher;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EXCEPTION fallback strategy through the FeatureDispatcher driving port.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: When a flag value has no matching variant and the feature uses
 * EXCEPTION strategy, the error message lists all known variants.
 */
class ExceptionFallbackTest {

    @Test
    void errorMessageListsKnownVariantsWhenNoMatch() {
        // Given: provider returns a value that doesn't match any variant
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("checkout-flow", "BETA");
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(provider);

        // When: resolve and call method
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);

        // Then: exception message lists all known variants
        assertThatThrownBy(proxy::execute)
                .isInstanceOf(UnmatchedVariantException.class)
                .hasMessageContaining("BETA")
                .hasMessageContaining("CLASSIC")
                .hasMessageContaining("STREAMLINED");
    }
}
