package com.flagzen.processor;

import com.flagzen.FallbackStrategy;

import java.util.List;

/**
 * Compile-time model of a @Feature-annotated interface.
 */
final class FeatureModel {
    private final String packageName;
    private final String interfaceName;
    private final String flagKey;
    private final FallbackStrategy fallbackStrategy;
    private final List<MethodModel> methods;
    private final List<VariantModel> variants;

    FeatureModel(String packageName, String interfaceName, String flagKey,
                 FallbackStrategy fallbackStrategy, List<MethodModel> methods,
                 List<VariantModel> variants) {
        this.packageName = packageName;
        this.interfaceName = interfaceName;
        this.flagKey = flagKey;
        this.fallbackStrategy = fallbackStrategy;
        this.methods = methods;
        this.variants = variants;
    }

    String packageName() { return packageName; }
    String interfaceName() { return interfaceName; }
    String flagKey() { return flagKey; }
    FallbackStrategy fallbackStrategy() { return fallbackStrategy; }
    List<MethodModel> methods() { return methods; }
    List<VariantModel> variants() { return variants; }

    String proxyClassName() {
        return interfaceName + "_FlagZenProxy";
    }

    String metadataClassName() {
        return interfaceName + "_FlagZenMetadata";
    }
}
