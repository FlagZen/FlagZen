package com.flagzen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a variant as the default when no flag value matches.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DefaultVariant {
    /** The feature interface this default variant belongs to. */
    Class<?> of() default void.class;
}
