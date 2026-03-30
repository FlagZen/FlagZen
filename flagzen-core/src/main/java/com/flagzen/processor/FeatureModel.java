package com.flagzen.processor;

import com.flagzen.FallbackStrategy;
import com.flagzen.FeatureType;

import java.util.List;

/**
 * Compile-time model of a @Feature-annotated interface.
 */
record FeatureModel(
        String packageName,
        String interfaceName,
        String flagKey,
        FallbackStrategy fallbackStrategy,
        FeatureType featureType,
        List<MethodModel> methods,
        List<VariantModel> variants,
        String defaultVariantClassName
) {

    /**
     * Backward-compatible constructor for STRING-typed features.
     */
    FeatureModel(String packageName, String interfaceName, String flagKey,
                 FallbackStrategy fallbackStrategy, List<MethodModel> methods,
                 List<VariantModel> variants, String defaultVariantClassName) {
        this(packageName, interfaceName, flagKey, fallbackStrategy, FeatureType.STRING,
                methods, variants, defaultVariantClassName);
    }

    String proxyClassName() {
        return interfaceName + "_FlagZenProxy";
    }

    String metadataClassName() {
        return interfaceName + "_FlagZenMetadata";
    }

    /**
     * Returns true if any variant has an explicit dispatch order (order != Integer.MAX_VALUE).
     */
    boolean hasOrderedDispatch() {
        return variants.stream().anyMatch(v -> v.order() != Integer.MAX_VALUE);
    }
}
