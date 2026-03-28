package com.flagzen;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a double variant value with approximate matching.
 * Used as an annotation attribute within {@link Variant#doubleValue()}.
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface CloseTo {
    /** The double value to match against. */
    double value();

    /** The maximum absolute difference for a match. Defaults to 1e-10. */
    double delta() default 1e-10;
}
