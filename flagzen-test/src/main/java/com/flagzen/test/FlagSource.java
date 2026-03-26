package com.flagzen.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads flag values from a classpath properties file for all tests in the annotated class.
 * Properties file format: {@code flag.key=variant.value} (one per line).
 *
 * <p>Priority: {@link PinFlag} > {@link FlagSource} > default provider.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface FlagSource {

    /**
     * Classpath resource path to the properties file.
     */
    String value();
}
