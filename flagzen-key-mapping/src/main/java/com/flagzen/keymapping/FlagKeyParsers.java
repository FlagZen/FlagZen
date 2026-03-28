package com.flagzen.keymapping;

import java.util.ArrayList;
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

    /**
     * Creates a parser that matches any SCREAMING_SNAKE_CASE name,
     * splitting on underscores and lowercasing all segments.
     *
     * <p>For example, "CHECKOUT_FLOW" produces segments ["checkout", "flow"].
     *
     * @return a parser for screaming-snake-case names without a prefix
     */
    public static FlagKeyParser screamingSnakeCase() {
        return sourceName -> {
            List<String> segments = Arrays.stream(sourceName.split("_"))
                    .map(String::toLowerCase)
                    .toList();
            return Optional.of(segments);
        };
    }

    /**
     * Creates a parser that matches camelCase names with the given prefix,
     * stripping the prefix and splitting on uppercase boundaries into lowercase segments.
     *
     * <p>For example, with prefix "myApp", the input "myAppCheckoutFlow"
     * produces segments ["checkout", "flow"].
     *
     * @param prefix the required prefix (e.g., "myApp")
     * @return a parser for camelCase names with the given prefix
     */
    public static FlagKeyParser camelCase(String prefix) {
        return sourceName -> {
            if (!sourceName.startsWith(prefix)) {
                return Optional.empty();
            }
            String remainder = sourceName.substring(prefix.length());
            if (remainder.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(splitCamelCase(remainder));
        };
    }

    /**
     * Creates a parser that matches any camelCase name,
     * splitting on uppercase boundaries into lowercase segments.
     *
     * <p>For example, "checkoutFlow" produces segments ["checkout", "flow"].
     *
     * @return a parser for camelCase names without a prefix
     */
    public static FlagKeyParser camelCase() {
        return sourceName -> Optional.of(splitCamelCase(sourceName));
    }

    private static List<String> splitCamelCase(String input) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        for (int i = 1; i < input.length(); i++) {
            if (Character.isUpperCase(input.charAt(i))) {
                segments.add(input.substring(start, i).toLowerCase());
                start = i;
            }
        }
        segments.add(input.substring(start).toLowerCase());
        return segments;
    }
}
