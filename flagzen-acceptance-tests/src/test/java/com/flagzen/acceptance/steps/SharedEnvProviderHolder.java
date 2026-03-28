package com.flagzen.acceptance.steps;

import com.flagzen.spi.FlagProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Holds shared state for environment variable provider acceptance tests.
 */
public final class SharedEnvProviderHolder {

    private static final Map<String, String> envVars = new HashMap<>();
    private static FlagProvider provider;
    private static Optional<String> lastResult;

    private SharedEnvProviderHolder() {
    }

    public static void setEnvVar(String name, String value) {
        envVars.put(name, value);
    }

    public static Map<String, String> getEnvVars() {
        return Map.copyOf(envVars);
    }

    public static void setProvider(FlagProvider provider) {
        SharedEnvProviderHolder.provider = provider;
    }

    public static FlagProvider getProvider() {
        return provider;
    }

    public static void setLastResult(Optional<String> result) {
        SharedEnvProviderHolder.lastResult = result;
    }

    public static Optional<String> getLastResult() {
        return lastResult;
    }

    public static void reset() {
        envVars.clear();
        provider = null;
        lastResult = null;
    }
}
