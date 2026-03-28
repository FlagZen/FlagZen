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
        throw new UnsupportedOperationException("Not yet implemented");
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
        throw new UnsupportedOperationException("Not yet implemented");
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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Creates a formatter that joins segments with dots (dot.case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout.flow".
     *
     * @return a dot-case formatter
     */
    public static FlagKeyFormat dotCase() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Creates a formatter that joins segments with colons (colon:case).
     *
     * <p>For example, segments ["checkout", "flow"] produce "checkout:flow".
     *
     * @return a colon-case formatter
     */
    public static FlagKeyFormat colonCase() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
