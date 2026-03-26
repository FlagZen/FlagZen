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
    /** The variant value that activates this implementation. */
    String value();

    /** The feature interface this variant belongs to. */
    Class<?> of() default void.class;
}
