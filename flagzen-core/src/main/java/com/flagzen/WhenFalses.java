package com.flagzen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link WhenFalse} annotations.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface WhenFalses {
    /** The contained {@link WhenFalse} annotations. */
    WhenFalse[] value();
}
