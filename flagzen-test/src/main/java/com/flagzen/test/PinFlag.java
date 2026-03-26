package com.flagzen.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pins a feature flag to a specific variant value for the duration of a test.
 * When used with {@link FlagZenExtension}, the annotated test method will
 * resolve the feature to the pinned variant without any explicit provider setup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(PinFlags.class)
public @interface PinFlag {

    /**
     * The feature flag key.
     */
    String feature();

    /**
     * The variant value to pin.
     */
    String variant();
}
