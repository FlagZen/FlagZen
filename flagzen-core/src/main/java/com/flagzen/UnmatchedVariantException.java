package com.flagzen;

/**
 * Thrown when no variant matches the current flag value
 * and the fallback strategy requires a match.
 */
public class UnmatchedVariantException extends FlagZenException {

    public UnmatchedVariantException(String flagKey, String flagValue) {
        super("No variant matches flag value '" + flagValue + "' for key '" + flagKey + "'");
    }
}
