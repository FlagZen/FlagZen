package com.flagzen.spi;

import com.flagzen.internal.InMemoryFlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InMemoryFlagProvider.
 * Test Budget: 3 behaviors x 2 = 6 max unit tests. Using 3.
 */
class InMemoryFlagProviderTest {

    /**
     * Behavior 1: Returns configured flag value for known keys.
     * Parametrized over multiple key-value pairs.
     */
    @ParameterizedTest
    @CsvSource({
            "checkout-flow,STREAMLINED",
            "dark-mode,ENABLED",
            "pricing-tier,PREMIUM"
    })
    void returnsConfiguredValueForKnownKey(String key, String value) {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set(key, value);

        Optional<String> result = provider.getString(key);

        assertThat(result).contains(value);
    }

    /**
     * Behavior 2: Returns empty for unknown key.
     */
    @Test
    void returnsEmptyForUnknownKey() {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();

        Optional<String> result = provider.getString("nonexistent-flag");

        assertThat(result).isEmpty();
    }

    /**
     * Behavior 3: getBoolean parses string values to boolean.
     * "true"/"false" (case-insensitive) -> Optional of boolean, anything else -> empty.
     */
    @ParameterizedTest
    @CsvSource({
            "true,true",
            "false,false",
            "TRUE,true",
            "False,false"
    })
    void getBooleanReturnsParsedBooleanForValidValues(String flagValue, boolean expected) {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("dark-mode", flagValue);

        Optional<Boolean> result = provider.getBoolean("dark-mode");

        assertThat(result).contains(expected);
    }
}
