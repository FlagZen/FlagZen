package com.flagzen;

/**
 * The type of flag value used to dispatch variants.
 */
public enum FeatureType {
    /** String-valued flag (default). */
    STRING,
    /** Integer-valued flag. */
    INT,
    /** Long-valued flag. */
    LONG,
    /** Boolean-valued flag. */
    BOOLEAN,
    /** Double-valued flag. */
    DOUBLE
}
