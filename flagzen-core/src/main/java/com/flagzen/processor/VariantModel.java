package com.flagzen.processor;

import com.flagzen.FeatureType;

/**
 * Compile-time model of a @Variant-annotated class.
 */
record VariantModel(String qualifiedClassName, String variantValue, int intVariantValue, FeatureType featureType) {

    /**
     * Creates a STRING-typed variant model (backward compatible).
     */
    VariantModel(String qualifiedClassName, String variantValue) {
        this(qualifiedClassName, variantValue, Integer.MIN_VALUE, FeatureType.STRING);
    }

    /**
     * Returns the variant key as a string suitable for code generation.
     * For STRING features, returns the string value.
     * For INT features, returns the integer value as a string.
     */
    String variantKeyLiteral() {
        return switch (featureType) {
            case INT -> String.valueOf(intVariantValue);
            default -> variantValue;
        };
    }
}
