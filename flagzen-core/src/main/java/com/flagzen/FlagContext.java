package com.flagzen;

/**
 * Thread-local holder for the current {@link EvaluationContext}.
 * Used internally by generated proxies to access the context
 * set by {@link FeatureDispatcher#resolve(Class, EvaluationContext)}.
 *
 * <p>This class is not part of the public API. Context is set by the dispatcher
 * before proxy creation and cleared after resolution.
 */
public final class FlagContext {

    private static final ThreadLocal<EvaluationContext> CURRENT = new ThreadLocal<>();

    private FlagContext() {
        // utility class
    }

    /**
     * Returns the current evaluation context, or {@code null} if none is set.
     *
     * @return the current context
     */
    public static EvaluationContext current() {
        return CURRENT.get();
    }

    /**
     * Sets the current evaluation context.
     * This method is intended for internal use by the dispatcher.
     *
     * @param context the context to set
     */
    public static void set(EvaluationContext context) {
        CURRENT.set(context);
    }

    /**
     * Clears the current evaluation context.
     * This method is intended for internal use by the dispatcher.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
