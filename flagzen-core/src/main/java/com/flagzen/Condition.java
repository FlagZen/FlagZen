package com.flagzen;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a condition predicate for conditional variant dispatch.
 *
 * <p>Used inside {@link Variant#when()} to make a variant active only when
 * a predicate matches (or does not match) the evaluation context.</p>
 *
 * <p>{@code matches} and {@code notMatches} are mutually exclusive.
 * When neither is set (both default to {@link Sentinel}), the condition
 * is considered absent and the variant dispatches unconditionally.</p>
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface Condition {

    /**
     * A predicate class that must match for this variant to activate.
     * Defaults to {@link Sentinel} (no condition).
     */
    Class<?> matches() default Sentinel.class;

    /**
     * A predicate class that must NOT match for this variant to activate.
     * Defaults to {@link Sentinel} (no condition).
     */
    Class<?> notMatches() default Sentinel.class;

    /**
     * Sentinel class used as the default value for {@code matches} and {@code notMatches}.
     * The annotation processor detects this sentinel to mean "no condition specified."
     * Never instantiated.
     */
    final class Sentinel {
    }
}
