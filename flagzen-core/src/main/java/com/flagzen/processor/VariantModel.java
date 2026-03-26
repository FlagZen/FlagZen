package com.flagzen.processor;

/**
 * Compile-time model of a @Variant-annotated class.
 */
final class VariantModel {
    private final String qualifiedClassName;
    private final String variantValue;

    VariantModel(String qualifiedClassName, String variantValue) {
        this.qualifiedClassName = qualifiedClassName;
        this.variantValue = variantValue;
    }

    String qualifiedClassName() { return qualifiedClassName; }
    String variantValue() { return variantValue; }
}
