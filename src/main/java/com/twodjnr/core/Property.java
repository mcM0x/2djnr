package com.twodjnr.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Property {
    String label() default "";
    String hint() default "";
    float min() default Float.NEGATIVE_INFINITY;
    float max() default Float.POSITIVE_INFINITY;
}
