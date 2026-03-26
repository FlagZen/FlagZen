package com.flagzen;

/**
 * Thrown when no variant matches the current flag value
 * and the fallback strategy requires a match.
 */
public class UnmatchedVariantException extends FlagZenException {

    public UnmatchedVariantException(String flagKey, String flagValue) {
        super("No variant matches flag value '" + flagValue + "' for key '" + flagKey + "'");
    }

    private UnmatchedVariantException(String message) {
        super(message);
    }

    /**
     * Creates an exception for the case where no flag value was found at all.
     */
    public static UnmatchedVariantException noFlagValue(String flagKey) {
        return new UnmatchedVariantException(
                "No flag value was found for key '" + flagKey + "'");
    }
}
