package com.twodjnr.engine.core;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Export {
    String name() default "";
    float min() default Float.NEGATIVE_INFINITY;
    float max() default Float.POSITIVE_INFINITY;
    float step() default 0.1f;
    String hint() default "";
}
