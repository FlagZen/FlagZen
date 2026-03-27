package com.flagzen.spi;

import com.flagzen.EvaluationContext;

import java.util.Optional;

/**
 * SPI for providing evaluation context from ambient sources.
 * Implementations supply context from framework-specific sources
 * such as HTTP request attributes or security principals.
 *
 * <p>Multiple accessors can be registered via {@link java.util.ServiceLoader}.
 * They are sorted by {@link #priority()} (lower value = higher priority),
 * and the first non-empty result wins.
 *
 * <p>Context accessors are consulted only when no explicit context
 * is passed to {@link com.flagzen.FeatureDispatcher#resolve(Class, EvaluationContext)}.
 */
public interface ContextAccessor {

    /**
     * Returns the current evaluation context from this accessor's source,
     * or empty if no context is available.
     *
     * @return the evaluation context, or empty
     */
    Optional<EvaluationContext> getContext();

    /**
     * Returns the priority of this accessor. Lower values indicate higher priority.
     * When multiple accessors return a context, the one with the lowest priority value wins.
     *
     * @return the priority value
     */
    int priority();
}
