package com.flagzen.keymapping;

/**
 * Factory methods for common {@link FlagKeyFormat} implementations.
 */
public final class FlagKeyFormats {

    private FlagKeyFormats() {
    }

    /**
     * Creates a formatter that joins segments with hyphens (kebab-case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout-flow".
     *
     * @return a kebab-case formatter
     */
    public static FlagKeyFormat kebabCase() {
        return segments -> String.join("-", segments);
    }

    /**
     * Creates a formatter that joins segments with underscores (snake_case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout_flow".
     *
     * @return a snake-case formatter
     */
    public static FlagKeyFormat snakeCase() {
        return segments -> String.join("_", segments);
    }

    /**
     * Creates a formatter that joins segments in camelCase.
     *
     * <p>The first segment is kept as-is, subsequent segments are capitalized.
     * For example, segments ["checkout", "flow"] produce "checkoutFlow".
     *
     * @return a camelCase formatter
     */
    public static FlagKeyFormat camelCase() {
        return segments -> {
            if (segments.isEmpty()) {
                return "";
            }
            StringBuilder result = new StringBuilder(segments.get(0));
            for (int i = 1; i < segments.size(); i++) {
                String segment = segments.get(i);
                result.append(Character.toUpperCase(segment.charAt(0)))
                      .append(segment.substring(1));
            }
            return result.toString();
        };
    }

    /**
     * Creates a formatter that joins segments in PascalCase.
     *
     * <p>All segments are capitalized.
     * For example, segments ["checkout", "flow"] produce "CheckoutFlow".
     *
     * @return a PascalCase formatter
     */
    public static FlagKeyFormat pascalCase() {
        return segments -> {
            StringBuilder result = new StringBuilder();
            for (String segment : segments) {
                result.append(Character.toUpperCase(segment.charAt(0)))
                      .append(segment.substring(1));
            }
            return result.toString();
        };
    }

    /**
     * Creates a formatter that joins segments with dots (dot.case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout.flow".
     *
     * @return a dot-case formatter
     */
    public static FlagKeyFormat dotCase() {
        return segments -> String.join(".", segments);
    }

    /**
     * Creates a formatter that joins segments with colons (colon:case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout:flow".
     *
     * @return a colon-case formatter
     */
    public static FlagKeyFormat colonCase() {
        return segments -> String.join(":", segments);
    }
}
