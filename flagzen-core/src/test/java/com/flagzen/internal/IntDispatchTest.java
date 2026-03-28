package com.flagzen.internal;

import com.flagzen.spi.FlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FlagProvider.getInt() integer parsing dispatch.
 * Test Budget: 2 behaviors x 2 = 4 max unit tests.
 *
 * Port-to-port: FlagProvider (driving port) getString -> getInt default method parsing.
 */
class IntDispatchTest {

    @ParameterizedTest
    @ValueSource(strings = {"3", "10", "0", "-1", "2147483647"})
    void parsesValidIntegerFromStringFlagValue(String intValue) {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("max-retries", intValue);

        OptionalInt result = provider.getInt("max-retries");

        assertThat(result).isPresent();
        assertThat(result.getAsInt()).isEqualTo(Integer.parseInt(intValue));
    }

    @Test
    void returnsEmptyWhenFlagKeyNotSet() {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();

        OptionalInt result = provider.getInt("missing-key");

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "3.14", "", "  "})
    void returnsEmptyWhenFlagValueIsNotValidInteger(String invalidInt) {
        InMemoryFlagProvider provider = new InMemoryFlagProvider();
        provider.set("max-retries", invalidInt);

        OptionalInt result = provider.getInt("max-retries");

        assertThat(result).isEmpty();
    }
}
