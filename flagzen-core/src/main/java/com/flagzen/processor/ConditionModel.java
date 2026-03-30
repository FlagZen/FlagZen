package com.flagzen.processor;

/**
 * Compile-time model of a {@code @Condition} annotation on a variant.
 *
 * @param predicateClassName the fully qualified class name of the predicate
 * @param negated true if declared via {@code notMatches}, false for {@code matches}
 */
record ConditionModel(String predicateClassName, boolean negated) {
}
