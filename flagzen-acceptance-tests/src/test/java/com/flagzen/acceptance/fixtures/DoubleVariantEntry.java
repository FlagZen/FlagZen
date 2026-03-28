package com.flagzen.acceptance.fixtures;

import java.util.function.Supplier;

/**
 * Entry mapping a double value with tolerance to a variant supplier.
 * Used by DOUBLE-typed proxy for approximate matching.
 */
public record DoubleVariantEntry(double value, double tolerance, Supplier<SamplingStrategy> supplier) {
    public boolean matches(double flagValue) {
        return Math.abs(flagValue - value) <= tolerance;
    }
}
