package com.flagzen;

/**
 * Resolves a feature interface to its active variant proxy.
 * The proxy re-evaluates the flag on every method call (dynamic dispatch).
 */
public interface FeatureDispatcher {

    /**
     * Resolves the given feature type to a dispatch proxy.
     * The proxy delegates each method call to the variant matching
     * the current flag value from the configured provider.
     *
     * @param featureType the feature interface class
     * @param <T> the feature type
     * @return a proxy that dispatches to the active variant
     */
    <T> T resolve(Class<T> featureType);

    /**
     * Resolves the given feature type to a dispatch proxy using the provided evaluation context.
     * The context is made available to the flag provider for targeted resolution.
     *
     * @param featureType the feature interface class
     * @param context the evaluation context for targeted resolution
     * @param <T> the feature type
     * @return a proxy that dispatches to the active variant
     */
    <T> T resolve(Class<T> featureType, EvaluationContext context);
}
