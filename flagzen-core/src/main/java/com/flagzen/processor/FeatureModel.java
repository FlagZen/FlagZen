package com.flagzen.processor;

import com.flagzen.FallbackStrategy;

import java.util.List;

/**
 * Compile-time model of a @Feature-annotated interface.
 */
record FeatureModel(
        String packageName,
        String interfaceName,
        String flagKey,
        FallbackStrategy fallbackStrategy,
        List<MethodModel> methods,
        List<VariantModel> variants,
        String defaultVariantClassName
) {

    String proxyClassName() {
        return interfaceName + "_FlagZenProxy";
    }

    String metadataClassName() {
        return interfaceName + "_FlagZenMetadata";
    }
}
