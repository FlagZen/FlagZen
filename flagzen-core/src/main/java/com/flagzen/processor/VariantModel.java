package com.flagzen.processor;

import com.flagzen.FeatureType;

/**
 * Compile-time model of a @Variant-annotated class.
 */
record VariantModel(String qualifiedClassName, String variantValue, int intVariantValue,
                    long longVariantValue, double doubleVariantValue, double doubleDelta,
                    boolean booleanVariantValue, FeatureType featureType,
                    ConditionModel condition, int order) {

    /**
     * Creates a STRING-typed variant model (backward compatible, no condition).
     */
    VariantModel(String qualifiedClassName, String variantValue) {
        this(qualifiedClassName, variantValue, Integer.MIN_VALUE, Long.MIN_VALUE,
                Double.NaN, 0.0, false, FeatureType.STRING, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a STRING-typed variant model with condition and order.
     */
    VariantModel(String qualifiedClassName, String variantValue,
                 ConditionModel condition, int order) {
        this(qualifiedClassName, variantValue, Integer.MIN_VALUE, Long.MIN_VALUE,
                Double.NaN, 0.0, false, FeatureType.STRING, condition, order);
    }

    /**
     * Creates an INT-typed variant model.
     */
    VariantModel(String qualifiedClassName, String variantValue, int intVariantValue, FeatureType featureType) {
        this(qualifiedClassName, variantValue, intVariantValue, Long.MIN_VALUE,
                Double.NaN, 0.0, false, featureType, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a BOOLEAN-typed variant model.
     */
    static VariantModel ofBoolean(String qualifiedClassName, boolean booleanValue) {
        return new VariantModel(qualifiedClassName, "", Integer.MIN_VALUE, Long.MIN_VALUE,
                Double.NaN, 0.0, booleanValue, FeatureType.BOOLEAN, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a LONG-typed variant model.
     */
    static VariantModel ofLong(String qualifiedClassName, long longValue) {
        return new VariantModel(qualifiedClassName, "", Integer.MIN_VALUE, longValue,
                Double.NaN, 0.0, false, FeatureType.LONG, null, Integer.MAX_VALUE);
    }

    /**
     * Creates a DOUBLE-typed variant model.
     */
    static VariantModel ofDouble(String qualifiedClassName, double doubleValue, double delta) {
        return new VariantModel(qualifiedClassName, "", Integer.MIN_VALUE, Long.MIN_VALUE,
                doubleValue, delta, false, FeatureType.DOUBLE, null, Integer.MAX_VALUE);
    }

    /**
     * Returns the variant key as a string suitable for code generation.
     * For STRING features, returns the string value.
     * For INT features, returns the integer value as a string.
     * For LONG features, returns the long value as a string.
     * For DOUBLE features, returns the double value as a string.
     * For BOOLEAN features, returns "true" or "false".
     */
    String variantKeyLiteral() {
        return switch (featureType) {
            case INT -> String.valueOf(intVariantValue);
            case LONG -> String.valueOf(longVariantValue);
            case DOUBLE -> String.valueOf(doubleVariantValue);
            case BOOLEAN -> String.valueOf(booleanVariantValue);
            default -> variantValue;
        };
    }
}
