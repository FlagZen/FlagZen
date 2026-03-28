package com.flagzen.keymapping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge case tests for FlagKeyParsers.
 */
class FlagKeyParsersEdgeCaseTest {

    @Test
    void screamingSnakeCaseWithPrefixOnlyReturnsEmpty() {
        var parser = FlagKeyParsers.screamingSnakeCase("FLAGZEN_");
        assertThat(parser.parse("FLAGZEN_")).isEmpty();
    }

    @Test
    void screamingSnakeCaseConsecutiveUnderscoresProducesEmptySegment() {
        var parser = FlagKeyParsers.screamingSnakeCase("FLAGZEN_");
        var result = parser.parse("FLAGZEN__FLOW");
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("", "flow");
    }

    @Test
    void screamingSnakeCaseWithoutPrefixSingleWord() {
        var parser = FlagKeyParsers.screamingSnakeCase();
        var result = parser.parse("DARKMODE");
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("darkmode");
    }

    @Test
    void camelCaseAllLowercaseReturnsSingleSegment() {
        var parser = FlagKeyParsers.camelCase();
        var result = parser.parse("checkout");
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("checkout");
    }

    @Test
    void camelCaseWithNumbersDoesNotSplitOnDigits() {
        var parser = FlagKeyParsers.camelCase();
        var result = parser.parse("checkout2Flow");
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("checkout2", "flow");
    }

    @Test
    void camelCaseWithPrefixOnlyReturnsEmpty() {
        var parser = FlagKeyParsers.camelCase("myApp");
        assertThat(parser.parse("myApp")).isEmpty();
    }

    @Test
    void camelCaseWithPrefixNonMatchingReturnsEmpty() {
        var parser = FlagKeyParsers.camelCase("myApp");
        assertThat(parser.parse("FLAGZEN_FOO")).isEmpty();
    }
}
