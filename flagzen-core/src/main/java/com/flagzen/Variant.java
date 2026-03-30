package com.flagzen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a variant implementation for a feature.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(Variants.class)
public @interface Variant {
    /** The string variant value(s) that activate this implementation. */
    String[] value() default {};

    /** The integer variant value(s) that activate this implementation. */
    int[] intValue() default {};

    /** The long variant value(s) that activate this implementation. */
    long[] longValue() default {};

    /** The double variant value that activates this implementation, with approximate matching. */
    CloseTo[] doubleValue() default {};

    /** The boolean variant value that activates this implementation. Empty string means not set. */
    String booleanValue() default "";

    /** The feature interface this variant belongs to. */
    Class<?> of() default void.class;

    /**
     * A condition that must be satisfied for this variant to activate.
     * Defaults to an unconditioned {@code @Condition} (sentinel values),
     * meaning the variant dispatches based on flag value alone.
     */
    Condition when() default @Condition(matches = Condition.Sentinel.class);

    /**
     * Explicit dispatch order for this variant. Lower values are evaluated first.
     * Defaults to {@link Integer#MAX_VALUE}, meaning no explicit ordering.
     */
    int order() default Integer.MAX_VALUE;
}
