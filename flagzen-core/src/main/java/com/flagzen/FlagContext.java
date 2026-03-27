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

    /**
     * Executes the given block with the specified evaluation context active.
     * The context is available via {@link #current()} for the duration of the block,
     * and is restored to the previous value (or cleared) when the block completes.
     *
     * @param context the evaluation context to activate
     * @param block the code to execute within the scoped context
     */
    public static void run(EvaluationContext context, Runnable block) {
        EvaluationContext previous = CURRENT.get();
        CURRENT.set(context);
        try {
            block.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * Executes the given block with the specified evaluation context active,
     * returning the block's result.
     * The context is available via {@link #current()} for the duration of the block,
     * and is restored to the previous value (or cleared) when the block completes.
     *
     * @param context the evaluation context to activate
     * @param block the code to execute within the scoped context
     * @param <T> the return type
     * @return the result of the block
     */
    public static <T> T run(EvaluationContext context, java.util.function.Supplier<T> block) {
        EvaluationContext previous = CURRENT.get();
        CURRENT.set(context);
        try {
            return block.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
