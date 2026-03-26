package com.flagzen.test;

import com.flagzen.FlagZenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for missing @FlagSource file error handling.
 * Test Budget: 1 behavior x 2 = 2 max unit tests.
 *
 * Behavior: When @FlagSource references a nonexistent classpath resource,
 * a FlagZenException is thrown with the file name and searched locations.
 *
 * Driving port: TestFlagContext.createFromProperties (invoked by FlagZenExtension).
 * Driven port: classpath resource loading.
 */
class MissingFlagSourceTest {

    /**
     * Behavior 1: Missing flag source file throws FlagZenException with file name
     * and searched locations in the message.
     */
    @Test
    void throwsFlagZenExceptionWithFileNameAndSearchedLocations() {
        assertThatThrownBy(() -> TestFlagContext.createFromProperties("nonexistent.properties"))
                .isInstanceOf(FlagZenException.class)
                .hasMessageContaining("nonexistent.properties")
                .hasMessageContaining("not found")
                .hasMessageContaining("classpath");
    }
}
