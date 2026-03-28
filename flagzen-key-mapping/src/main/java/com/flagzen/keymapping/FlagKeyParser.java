package com.flagzen.keymapping;

import java.util.List;
import java.util.Optional;

/**
 * Parses a source name (e.g., an environment variable name) into flag key segments.
 *
 * <p>Returns {@link Optional#empty()} if the source name does not match the expected pattern,
 * or a list of lowercase segments if it does.
 */
@FunctionalInterface
public interface FlagKeyParser {

    /**
     * Attempts to parse the given source name into a list of key segments.
     *
     * @param sourceName the raw source name to parse (e.g., "FLAGZEN_CHECKOUT_FLOW")
     * @return the parsed segments, or empty if the name does not match
     */
    Optional<List<String>> parse(String sourceName);
}
