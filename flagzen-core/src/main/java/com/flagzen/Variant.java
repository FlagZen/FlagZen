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
    /** The string variant value that activates this implementation. */
    String value() default "";

    /** The integer variant value that activates this implementation. */
    int intValue() default Integer.MIN_VALUE;

    /** The long variant value that activates this implementation. */
    long longValue() default Long.MIN_VALUE;

    /** The double variant value that activates this implementation, with approximate matching. */
    CloseTo[] doubleValue() default {};

    /** The boolean variant value that activates this implementation. Empty string means not set. */
    String booleanValue() default "";

    /** The feature interface this variant belongs to. */
    Class<?> of() default void.class;
}
