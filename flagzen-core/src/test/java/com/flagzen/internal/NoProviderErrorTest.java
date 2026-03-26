package com.flagzen.internal;

import com.flagzen.FlagZen;
import com.flagzen.FlagZenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for no-provider configuration error.
 * Test Budget: 1 behavior x 2 = 2 max unit tests. Using 1.
 *
 * Behavior: Creating a dispatcher without a flag provider raises a configuration
 * error with a message stating no provider is configured and suggesting how to add one.
 */
class NoProviderErrorTest {

    @Test
    void throwsConfigurationErrorWithHelpfulMessageWhenNoProviderConfigured() {
        assertThatThrownBy(() -> FlagZen.dispatcher(config -> { /* no provider set */ }))
                .isInstanceOf(FlagZenException.class)
                .hasMessageContaining("No FlagProvider configured")
                .hasMessageContaining("provider");
    }
}
