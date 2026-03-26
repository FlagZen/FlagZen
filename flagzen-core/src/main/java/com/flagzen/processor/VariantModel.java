package com.flagzen.processor;

/**
 * Compile-time model of a @Variant-annotated class.
 */
record VariantModel(String qualifiedClassName, String variantValue) {
}
