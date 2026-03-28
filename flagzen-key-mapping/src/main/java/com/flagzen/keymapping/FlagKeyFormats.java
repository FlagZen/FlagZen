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
}
