package com.flagzen.keymapping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge case tests for FlagKeyFormats.
 */
class FlagKeyFormatsEdgeCaseTest {

    @Test
    void kebabCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.kebabCase().format(List.of())).isEmpty();
    }

    @Test
    void snakeCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.snakeCase().format(List.of())).isEmpty();
    }

    @Test
    void camelCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.camelCase().format(List.of())).isEmpty();
    }

    @Test
    void pascalCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.pascalCase().format(List.of())).isEmpty();
    }

    @Test
    void dotCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.dotCase().format(List.of())).isEmpty();
    }

    @Test
    void colonCaseEmptySegmentsReturnsEmpty() {
        assertThat(FlagKeyFormats.colonCase().format(List.of())).isEmpty();
    }

    @Test
    void pascalCaseSingleSegment() {
        assertThat(FlagKeyFormats.pascalCase().format(List.of("checkout"))).isEqualTo("Checkout");
    }

    @Test
    void camelCaseSingleSegment() {
        assertThat(FlagKeyFormats.camelCase().format(List.of("checkout"))).isEqualTo("checkout");
    }
}
