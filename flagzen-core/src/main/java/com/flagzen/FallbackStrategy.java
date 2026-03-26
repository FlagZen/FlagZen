package com.flagzen;

/**
 * Strategy for handling missing variant matches.
 */
public enum FallbackStrategy {
    /** A variant must always be found; throw if missing. */
    REQUIRED,
    /** Throw UnsupportedOperationException if no variant matches. */
    EXCEPTION,
    /** Silently do nothing if no variant matches. */
    NOOP
}
