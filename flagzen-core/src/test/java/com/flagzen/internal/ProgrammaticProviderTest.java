package com.flagzen.internal;

import com.flagzen.FeatureDispatcher;
import com.flagzen.FlagZen;
import com.flagzen.acceptance.fixtures.CheckoutFlow;
import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for programmatic flag provider registration via FlagZen configuration API.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Driving port: FlagZen.dispatcher() (public configuration API).
 */
class ProgrammaticProviderTest {

    /**
     * Behavior: A custom FlagProvider registered via the configuration API
     * is used by the dispatcher to resolve features.
     */
    @Test
    void customProviderRegisteredViaConfigurationApiResolvesFeatures() {
        // Given: a custom flag provider
        FlagProvider customProvider = key ->
                "checkout-flow".equals(key) ? Optional.of("CLASSIC") : Optional.empty();

        // When: configured via FlagZen API and resolved
        FeatureDispatcher dispatcher = FlagZen.dispatcher(config -> config.provider(customProvider));
        CheckoutFlow proxy = dispatcher.resolve(CheckoutFlow.class);
        String result = proxy.execute();

        // Then: dispatches through the custom provider
        assertThat(result).isEqualTo("ClassicCheckout");
    }
}
