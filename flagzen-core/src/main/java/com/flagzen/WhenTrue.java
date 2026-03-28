package com.flagzen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience annotation for boolean feature variants activated when the flag is {@code true}.
 * Equivalent to {@code @Variant(booleanValue = "true")}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(WhenTrues.class)
public @interface WhenTrue {
    /** The feature interface this variant belongs to. */
    Class<?> of() default void.class;
}
