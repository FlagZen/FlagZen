package com.flagzen.acceptance.steps;

import com.flagzen.EvaluationContext;
import com.flagzen.FeatureDispatcher;
import com.flagzen.internal.InMemoryFlagProvider;
import com.flagzen.spi.FlagProvider;

/**
 * Thread-safe holder for sharing dispatcher and flag provider state between step definition classes.
 * Reset before each scenario via {@link ScenarioHooks}.
 */
final class SharedDispatcherHolder {

    private static final ThreadLocal<FeatureDispatcher> DISPATCHER = new ThreadLocal<>();
    private static final ThreadLocal<FlagProvider> FLAG_PROVIDER = new ThreadLocal<>();
    private static final ThreadLocal<InMemoryFlagProvider> IN_MEMORY_PROVIDER = new ThreadLocal<>();
    private static final ThreadLocal<EvaluationContext> EVAL_CONTEXT = new ThreadLocal<>();

    private SharedDispatcherHolder() {
    }

    static void setDispatcher(FeatureDispatcher dispatcher) {
        DISPATCHER.set(dispatcher);
    }

    static FeatureDispatcher getDispatcher() {
        return DISPATCHER.get();
    }

    static void setFlagProvider(FlagProvider provider) {
        FLAG_PROVIDER.set(provider);
    }

    static FlagProvider getFlagProvider() {
        return FLAG_PROVIDER.get();
    }

    static void setInMemoryProvider(InMemoryFlagProvider provider) {
        IN_MEMORY_PROVIDER.set(provider);
    }

    static InMemoryFlagProvider getInMemoryProvider() {
        return IN_MEMORY_PROVIDER.get();
    }

    static void setEvalContext(EvaluationContext context) {
        EVAL_CONTEXT.set(context);
    }

    static EvaluationContext getEvalContext() {
        return EVAL_CONTEXT.get();
    }

    static void reset() {
        DISPATCHER.remove();
        FLAG_PROVIDER.remove();
        IN_MEMORY_PROVIDER.remove();
        EVAL_CONTEXT.remove();
    }
}
