package com.flagzen.keymapping;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Factory methods for common {@link FlagKeyParser} implementations.
 */
public final class FlagKeyParsers {

    private FlagKeyParsers() {
    }

    /**
     * Creates a parser that matches SCREAMING_SNAKE_CASE names with the given prefix,
     * stripping the prefix and splitting the remainder into lowercase segments.
     *
     * <p>For example, with prefix "FLAGZEN_", the input "FLAGZEN_CHECKOUT_FLOW"
     * produces segments ["checkout", "flow"].
     *
     * @param prefix the required prefix (e.g., "FLAGZEN_")
     * @return a parser for screaming-snake-case names with the given prefix
     */
    public static FlagKeyParser screamingSnakeCase(String prefix) {
        return sourceName -> {
            if (!sourceName.startsWith(prefix)) {
                return Optional.empty();
            }
            String remainder = sourceName.substring(prefix.length());
            if (remainder.isEmpty()) {
                return Optional.empty();
            }
            List<String> segments = Arrays.stream(remainder.split("_"))
                    .map(String::toLowerCase)
                    .toList();
            return Optional.of(segments);
        };
    }
}
