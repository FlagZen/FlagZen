package com.flagzen.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeatable {@link PinFlag} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PinFlags {

    /**
     * The repeated {@link PinFlag} annotations.
     */
    PinFlag[] value();
}
