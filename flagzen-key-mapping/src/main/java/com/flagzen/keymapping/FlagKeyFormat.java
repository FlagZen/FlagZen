package com.flagzen.keymapping;

import java.util.List;

/**
 * Formats a list of key segments into a flag key string.
 */
@FunctionalInterface
public interface FlagKeyFormat {

    /**
     * Formats the given segments into a flag key string.
     *
     * @param segments the key segments to format (e.g., ["checkout", "flow"])
     * @return the formatted flag key (e.g., "checkout-flow")
     */
    String format(List<String> segments);
}
