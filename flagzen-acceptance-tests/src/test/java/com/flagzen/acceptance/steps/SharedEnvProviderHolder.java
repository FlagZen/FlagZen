package com.flagzen.acceptance.steps;

import com.flagzen.env.EnvironmentVariableFlagProvider;
import com.flagzen.spi.FlagProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds shared state for environment variable provider acceptance tests.
 */
public final class SharedEnvProviderHolder {

    private static final Map<String, String> envVars = new HashMap<>();
    private static FlagProvider provider;
    private static Optional<String> lastResult;
    private static List<Optional<String>> repeatedResults = new ArrayList<>();
    private static List<FlagProvider> discoveredProviders = new ArrayList<>();
    private static EnvironmentVariableFlagProvider.Builder builder;

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

    public static void setRepeatedResults(List<Optional<String>> results) {
        SharedEnvProviderHolder.repeatedResults = results;
    }

    public static List<Optional<String>> getRepeatedResults() {
        return repeatedResults;
    }

    public static void setDiscoveredProviders(List<FlagProvider> providers) {
        SharedEnvProviderHolder.discoveredProviders = providers;
    }

    public static List<FlagProvider> getDiscoveredProviders() {
        return discoveredProviders;
    }

    public static void setBuilder(EnvironmentVariableFlagProvider.Builder builder) {
        SharedEnvProviderHolder.builder = builder;
    }

    public static EnvironmentVariableFlagProvider.Builder getBuilder() {
        return builder;
    }

    public static void reset() {
        envVars.clear();
        provider = null;
        lastResult = null;
        repeatedResults = new ArrayList<>();
        discoveredProviders = new ArrayList<>();
        builder = null;
    }
}
