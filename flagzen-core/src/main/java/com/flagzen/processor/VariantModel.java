package com.flagzen.processor;

import com.flagzen.FeatureType;

/**
 * Compile-time model of a @Variant-annotated class.
 */
record VariantModel(String qualifiedClassName, String variantValue, int intVariantValue,
                    boolean booleanVariantValue, FeatureType featureType) {

    /**
     * Creates a STRING-typed variant model (backward compatible).
     */
    VariantModel(String qualifiedClassName, String variantValue) {
        this(qualifiedClassName, variantValue, Integer.MIN_VALUE, false, FeatureType.STRING);
    }

    /**
     * Creates an INT-typed variant model.
     */
    VariantModel(String qualifiedClassName, String variantValue, int intVariantValue, FeatureType featureType) {
        this(qualifiedClassName, variantValue, intVariantValue, false, featureType);
    }

    /**
     * Creates a BOOLEAN-typed variant model.
     */
    static VariantModel ofBoolean(String qualifiedClassName, boolean booleanValue) {
        return new VariantModel(qualifiedClassName, "", Integer.MIN_VALUE, booleanValue, FeatureType.BOOLEAN);
    }

    /**
     * Returns the variant key as a string suitable for code generation.
     * For STRING features, returns the string value.
     * For INT features, returns the integer value as a string.
     * For BOOLEAN features, returns "true" or "false".
     */
    String variantKeyLiteral() {
        return switch (featureType) {
            case INT -> String.valueOf(intVariantValue);
            case BOOLEAN -> String.valueOf(booleanVariantValue);
            default -> variantValue;
        };
    }
}
