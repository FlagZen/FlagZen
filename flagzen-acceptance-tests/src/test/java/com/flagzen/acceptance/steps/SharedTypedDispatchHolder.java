package com.flagzen.acceptance.steps;

import com.flagzen.spi.ContextAccessor;

/**
 * Thread-safe holder for sharing typed dispatch state between step definition classes.
 * Reset before each scenario via {@link ScenarioHooks}.
 */
final class SharedTypedDispatchHolder {

    private static final ThreadLocal<ContextAccessor> CONTEXT_ACCESSOR = new ThreadLocal<>();

    private SharedTypedDispatchHolder() {
    }

    static void setContextAccessor(ContextAccessor accessor) {
        CONTEXT_ACCESSOR.set(accessor);
    }

    static ContextAccessor getContextAccessor() {
        return CONTEXT_ACCESSOR.get();
    }

    static void reset() {
        CONTEXT_ACCESSOR.remove();
    }
}
