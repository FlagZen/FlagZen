package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: default variant for RetryStrategy, used when no integer variant matches.
 */
public class DefaultRetry implements RetryStrategy {

    @Override
    public String execute() {
        return "DefaultRetry";
    }
}
