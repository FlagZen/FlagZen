package com.flagzen.test;

import com.flagzen.test.fixtures.CheckoutFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for @FlagSource / FlagZenExtension through the driving port.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: @FlagSource loads flag values from a properties file and the
 * dispatcher resolves features accordingly.
 *
 * Driving port: FlagZenExtension (JUnit 5 extension) + TestFlagContext parameter.
 * Driven port: InMemoryFlagProvider populated from properties file.
 */
@ExtendWith(FlagZenExtension.class)
@FlagSource("flags-test.properties")
class FlagSourceTest {

    /**
     * Behavior 1: @FlagSource on test class loads properties file and resolves feature.
     */
    @Test
    void resolvesFeatureFromPropertiesFile(TestFlagContext flags) {
        CheckoutFlow flow = flags.resolve(CheckoutFlow.class);

        assertThat(flow.execute()).isEqualTo("ClassicCheckout");
    }
}
