package com.flagzen.acceptance;

import com.flagzen.FeatureDispatcher;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.internal.DefaultFeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance test for step 01-02: Runtime dispatch through FeatureDispatcher.
 *
 * Scenario: Developer resolves a feature to the active variant at runtime.
 * Port-to-port: FeatureDispatcher.resolve() (driving port) -> FlagProvider (driven port).
 */
class RuntimeDispatchAcceptanceTest {

    @Test
    void resolvesFeatureToActiveVariantAtRuntime() {
        // Given: a compiled feature "CheckoutFlow" with variants "CLASSIC" and "STREAMLINED"
        // (provided by CheckoutFlowMetadata registered via ServiceLoader)

        // And: an in-memory flag provider with "checkout-flow" set to "STREAMLINED"
        InMemoryFlagProvider flagProvider = new InMemoryFlagProvider();
        flagProvider.set("checkout-flow", "STREAMLINED");

        // And: the dispatcher is configured with this provider
        FeatureDispatcher dispatcher = new DefaultFeatureDispatcher(flagProvider);

        // When: the developer resolves "CheckoutFlow" through the dispatcher
        CheckoutFlow checkoutFlow = dispatcher.resolve(CheckoutFlow.class);

        // And: calls "execute" on the resolved proxy
        String result = checkoutFlow.execute();

        // Then: the call is handled by the "StreamlinedCheckout" variant
        assertThat(result).isEqualTo("StreamlinedCheckout");
    }
}
