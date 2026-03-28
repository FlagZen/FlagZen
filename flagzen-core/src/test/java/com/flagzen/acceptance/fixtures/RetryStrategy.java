package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: integer-typed feature interface for INT dispatch acceptance testing.
 * In production, this would be annotated with @Feature(value = "max-retries", type = FeatureType.INT).
 */
public interface RetryStrategy {
    int maxRetries();
}
