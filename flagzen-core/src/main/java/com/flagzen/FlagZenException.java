package com.flagzen;

/**
 * Base exception for FlagZen runtime errors.
 */
public class FlagZenException extends RuntimeException {

    public FlagZenException(String message) {
        super(message);
    }

    public FlagZenException(String message, Throwable cause) {
        super(message, cause);
    }
}
