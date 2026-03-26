package com.flagzen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a feature flag dispatch point.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Feature {
    /** The flag key used to resolve the active variant. */
    String value();

    /** Strategy when no variant matches the flag value. */
    FallbackStrategy fallback() default FallbackStrategy.EXCEPTION;
}
