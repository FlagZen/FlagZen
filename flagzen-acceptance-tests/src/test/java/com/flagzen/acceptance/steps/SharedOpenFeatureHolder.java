package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.spi.FlagProvider;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Shared state holder for OpenFeature adapter acceptance tests.
 * Holds the provider, evaluation context, and last results across step definitions.
 */
public final class SharedOpenFeatureHolder {

    private static FlagProvider provider;
    private static EvaluationContext evaluationContext;
    private static Optional<String> lastStringResult;
    private static Optional<Boolean> lastBooleanResult;
    private static OptionalInt lastIntResult;
    private static OptionalLong lastLongResult;
    private static OptionalDouble lastDoubleResult;

    private SharedOpenFeatureHolder() {
    }

    public static void reset() {
        provider = null;
        evaluationContext = null;
        lastStringResult = null;
        lastBooleanResult = null;
        lastIntResult = null;
        lastLongResult = null;
        lastDoubleResult = null;
    }

    public static void setProvider(FlagProvider p) {
        provider = p;
    }

    public static FlagProvider getProvider() {
        return provider;
    }

    public static void setEvaluationContext(EvaluationContext ctx) {
        evaluationContext = ctx;
    }

    public static EvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    public static void setLastStringResult(Optional<String> result) {
        lastStringResult = result;
    }

    public static Optional<String> getLastStringResult() {
        return lastStringResult;
    }

    public static void setLastBooleanResult(Optional<Boolean> result) {
        lastBooleanResult = result;
    }

    public static Optional<Boolean> getLastBooleanResult() {
        return lastBooleanResult;
    }

    public static void setLastIntResult(OptionalInt result) {
        lastIntResult = result;
    }

    public static OptionalInt getLastIntResult() {
        return lastIntResult;
    }

    public static void setLastLongResult(OptionalLong result) {
        lastLongResult = result;
    }

    public static OptionalLong getLastLongResult() {
        return lastLongResult;
    }

    public static void setLastDoubleResult(OptionalDouble result) {
        lastDoubleResult = result;
    }

    public static OptionalDouble getLastDoubleResult() {
        return lastDoubleResult;
    }
}
